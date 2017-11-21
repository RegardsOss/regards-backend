package fr.cnes.regards.modules.storage.rest;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.assertj.core.util.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.geojson.GeoJsonMediaType;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.EventType;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.security.utils.HttpConstants;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.storage.dao.IAIPDao;
import fr.cnes.regards.modules.storage.dao.IDataFileDao;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPBuilder;
import fr.cnes.regards.modules.storage.domain.AIPCollection;
import fr.cnes.regards.modules.storage.domain.AvailabilityRequest;
import fr.cnes.regards.modules.storage.domain.AvailabilityResponse;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.domain.event.DataStorageEvent;
import fr.cnes.regards.modules.storage.plugin.allocation.strategy.DefaultAllocationStrategyPlugin;
import fr.cnes.regards.modules.storage.plugin.datastorage.IDataStorage;
import fr.cnes.regards.modules.storage.plugin.datastorage.IOnlineDataStorage;
import fr.cnes.regards.modules.storage.plugin.datastorage.local.LocalDataStorage;
import fr.cnes.regards.modules.storage.plugin.security.ISecurityDelegation;
import fr.cnes.regards.modules.storage.service.DataStorageEventHandler;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@TestPropertySource(locations = "classpath:test.properties")
@ActiveProfiles("testAmqp")
public class AIPControllerIT extends AbstractRegardsTransactionalIT {

    private static final String ALLOCATION_CONF_LABEL = "AIPControllerIT_ALLOCATION";

    private static final String DATA_STORAGE_CONF_LABEL = "AIPControllerIT_DATA_STORAGE";

    private static final String CATALOG_SECURITY_DELEGATION_LABEL = "AIPControllerIT_SECU_DELEG";

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private Gson gson;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IRabbitVirtualHostAdmin vHost;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private IPluginConfigurationRepository pluginRepo;

    @Autowired
    private IJobInfoRepository jobInfoRepo;

    @Autowired
    private IDataFileDao dataFileDao;

    @Autowired
    private IAIPDao aipDao;

    private URL baseStorageLocation;

    private AIP aip;

    @Before
    public void init() throws IOException, ModuleException, URISyntaxException {
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        // first of all, lets get an AIP with accessible dataObjects and real checksums
        aip = getAIP();
        // second, lets storeAndCreate a plugin configuration for IAllocationStrategy
        PluginMetaData allocationMeta = PluginUtils
                .createPluginMetaData(DefaultAllocationStrategyPlugin.class,
                                      DefaultAllocationStrategyPlugin.class.getPackage().getName());
        PluginConfiguration allocationConfiguration = new PluginConfiguration(allocationMeta, ALLOCATION_CONF_LABEL);
        allocationConfiguration.setIsActive(true);
        pluginService.savePluginConfiguration(allocationConfiguration);
        // third, lets storeAndCreate a plugin configuration of IDataStorage with the highest priority
        PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(LocalDataStorage.class,
                                                                      IDataStorage.class.getPackage().getName(),
                                                                      IOnlineDataStorage.class.getPackage().getName());
        baseStorageLocation = new URL("file", "", Paths.get("target/AIPControllerIT").toFile().getAbsolutePath());
        Files.createDirectories(Paths.get(baseStorageLocation.toURI()));
        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(LocalDataStorage.LOCAL_STORAGE_TOTAL_SPACE, "9000000000000000").addParameter(
                        LocalDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                        gson.toJson(baseStorageLocation)).getParameters();
        PluginConfiguration dataStorageConf = new PluginConfiguration(dataStoMeta,
                                                                      DATA_STORAGE_CONF_LABEL,
                                                                      parameters,
                                                                      0);
        dataStorageConf.setIsActive(true);
        pluginService.savePluginConfiguration(dataStorageConf);
        // forth, lets configure a plugin for security checks
        pluginService.addPluginPackage(FakeSecurityDelegation.class.getPackage().getName());
        PluginMetaData catalogSecuDelegMeta = PluginUtils
                .createPluginMetaData(FakeSecurityDelegation.class, FakeSecurityDelegation.class.getPackage().getName(),
                                      ISecurityDelegation.class.getPackage().getName());
        PluginConfiguration catalogSecuDelegConf = new PluginConfiguration(catalogSecuDelegMeta,
                CATALOG_SECURITY_DELEGATION_LABEL);
        pluginService.savePluginConfiguration(catalogSecuDelegConf);
    }

    @Test
    public void testStore() {
        // make requirements
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isCreated());
        requestBuilderCustomizer.customizeHeaders().putAll(getHeaders());
        // perform request
        performDefaultPost(AIPController.AIP_PATH, new AIPCollection(aip), requestBuilderCustomizer,
                           "AIP storage should have been schedule properly");
    }

    @Test
    public void testStoreTotalFail() {
        testStore();
        // now just try to store the same aip
        // make requirements
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isUnprocessableEntity());
        requestBuilderCustomizer.customizeHeaders().putAll(getHeaders());
        // perform request
        performDefaultPost(AIPController.AIP_PATH, new AIPCollection(aip), requestBuilderCustomizer,
                           "Same AIP cannot be stored twice");
    }

    @Test
    public void testStoreFailPartial() throws MalformedURLException {
        testStore();
        // now just try to store the same aip
        // make requirements
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isPartialContent());
        requestBuilderCustomizer.customizeHeaders().putAll(getHeaders());
        AIP aip2 = getAIP();

        // perform request
        performDefaultPost(AIPController.AIP_PATH, new AIPCollection(aip, aip2), requestBuilderCustomizer,
                           "Success should be partial, aip cannot be re stored but aip2 can be stored");
    }

    private Map<String, List<String>> getHeaders() {
        Map<String, List<String>> headers = Maps.newHashMap();
        headers.put(HttpConstants.ACCEPT,
                    Lists.newArrayList("application/json", MediaType.APPLICATION_OCTET_STREAM_VALUE));
        headers.put(HttpConstants.CONTENT_TYPE, Lists.newArrayList(GeoJsonMediaType.APPLICATION_GEOJSON_VALUE));
        return headers;
    }

    @Test
    public void testMakeAvailable() throws InterruptedException {
        // ask for an aip to be stored
        testStore();
        // wait for the AIP
        Thread.sleep(4000);
        // get the datafiles checksum
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        Set<DataFile> dataFiles = dataFileDao.findAllByAip(aip);
        Set<String> dataFilesChecksum = dataFiles.stream().map(df -> df.getChecksum()).collect(Collectors.toSet());
        // ask for availability
        AvailabilityRequest availabilityRequest = new AvailabilityRequest(OffsetDateTime.now().plusDays(2),
                dataFilesChecksum.toArray(new String[dataFilesChecksum.size()]));
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.content()
                .json(gson.toJson(new AvailabilityResponse(Sets.newHashSet(), dataFiles, Sets.newHashSet()))));
        performDefaultPost(AIPController.AIP_PATH + AIPController.PREPARE_DATA_FILES, availabilityRequest,
                           requestBuilderCustomizer,
                           "data should already be available as they are in an online data storage");
    }

    @Test
    public void testDownload() throws InterruptedException {
        // lets make files available
        // first lets make available the file
        testMakeAvailable();
        // lets ask for download now
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        // lets get the hand of a datafile checksum
        Set<DataFile> dataFiles = dataFileDao.findAllByAip(aip);
        DataFile dataFile = dataFiles.toArray(new DataFile[dataFiles.size()])[0];
        // now lets download it!
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.customizeHeaders().putAll(getHeaders());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        performDefaultGet(AIPController.AIP_PATH + AIPController.DOWLOAD_AIP_FILE, requestBuilderCustomizer,
                          "We should be downloading the data file", aip.getId().toString(), dataFile.getChecksum());
    }

    @After
    public void cleanUp() throws URISyntaxException, IOException {
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        subscriber.purgeQueue(DataStorageEvent.class, DataStorageEventHandler.class);
        jobInfoRepo.deleteAll();
        dataFileDao.deleteAll();
        aipDao.deleteAll();
        pluginRepo.deleteAll();
        if (baseStorageLocation != null) {
            Files.walk(Paths.get(baseStorageLocation.toURI())).sorted(Comparator.reverseOrder()).map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    private AIP getAIP() throws MalformedURLException {

        AIPBuilder aipBuilder = new AIPBuilder(
                new UniformResourceName(OAISIdentifier.AIP, EntityType.DATA, DEFAULT_TENANT, UUID.randomUUID(), 1),
                null, EntityType.DATA);

        String path = System.getProperty("user.dir") + "/src/test/resources/data.txt";
        aipBuilder.getContentInformationBuilder().setDataObject(DataType.RAWDATA, new URL("file", "", path), "MD5",
                                                                "de89a907d33a9716d11765582102b2e0");
        aipBuilder.getContentInformationBuilder().setSyntax("text", "description", "text/plain");
        aipBuilder.addContentInformation();
        aipBuilder.getPDIBuilder().setAccessRightInformation("public");
        aipBuilder.getPDIBuilder().setFacility("CS");
        aipBuilder.getPDIBuilder().addProvenanceInformationEvent(EventType.SUBMISSION.name(), "test event",
                                                                 OffsetDateTime.now());

        return aipBuilder.build();
    }

}
