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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.matchers.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.restdocs.request.RequestDocumentation;
import org.springframework.restdocs.snippet.Attributes;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.util.MimeType;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.geojson.GeoJsonMediaType;
import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
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
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.notification.client.INotificationClient;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.storage.dao.IAIPDao;
import fr.cnes.regards.modules.storage.dao.IDataFileDao;
import fr.cnes.regards.modules.storage.dao.IPrioritizedDataStorageRepository;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPBuilder;
import fr.cnes.regards.modules.storage.domain.AIPCollection;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.AvailabilityRequest;
import fr.cnes.regards.modules.storage.domain.AvailabilityResponse;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.event.DataStorageEvent;
import fr.cnes.regards.modules.storage.domain.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.IOnlineDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.ISecurityDelegation;
import fr.cnes.regards.modules.storage.plugin.allocation.strategy.DefaultAllocationStrategyPlugin;
import fr.cnes.regards.modules.storage.plugin.datastorage.local.LocalDataStorage;
import fr.cnes.regards.modules.storage.service.DataStorageEventHandler;
import fr.cnes.regards.modules.storage.service.IPrioritizedDataStorageService;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@TestPropertySource(locations = "classpath:test.properties")
@ActiveProfiles("testAmqp")
public class AIPControllerIT extends AbstractRegardsTransactionalIT {

    private static final String ALLOCATION_CONF_LABEL = "AIPControllerIT_ALLOCATION";

    private static final String DATA_STORAGE_CONF_LABEL = "AIPControllerIT_DATA_STORAGE";

    private static final String CATALOG_SECURITY_DELEGATION_LABEL = "AIPControllerIT_SECU_DELEG";

    private static final int MAX_WAIT = 60000;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private Gson gson;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

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

    @Autowired
    private IPrioritizedDataStorageService prioritizedDataStorageService;

    @Autowired
    private IPrioritizedDataStorageRepository prioritizedDataStorageRepository;

    private URL baseStorageLocation;

    private AIP aip;

    @Before
    public void init() throws IOException, ModuleException, URISyntaxException {
        cleanUp();
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        // first of all, lets get an AIP with accessible dataObjects and real checksums
        aip = getAIP();
        // second, lets storeAndCreate a plugin configuration for IAllocationStrategy
        PluginMetaData allocationMeta = PluginUtils.createPluginMetaData(DefaultAllocationStrategyPlugin.class,
                                                                         DefaultAllocationStrategyPlugin.class
                                                                                 .getPackage().getName());
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
                .addParameter(LocalDataStorage.LOCAL_STORAGE_TOTAL_SPACE, 9000000000000000L)
                .addParameter(LocalDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME, baseStorageLocation.toString())
                .getParameters();
        PluginConfiguration dataStorageConf = new PluginConfiguration(dataStoMeta,
                                                                      DATA_STORAGE_CONF_LABEL,
                                                                      parameters,
                                                                      0);
        dataStorageConf.setIsActive(true);
        prioritizedDataStorageService.create(dataStorageConf);
        // forth, lets configure a plugin for security checks
        pluginService.addPluginPackage(FakeSecurityDelegation.class.getPackage().getName());
        PluginMetaData catalogSecuDelegMeta = PluginUtils.createPluginMetaData(FakeSecurityDelegation.class,
                                                                               FakeSecurityDelegation.class.getPackage()
                                                                                       .getName(),
                                                                               ISecurityDelegation.class.getPackage()
                                                                                       .getName());
        PluginConfiguration catalogSecuDelegConf = new PluginConfiguration(catalogSecuDelegMeta,
                                                                           CATALOG_SECURITY_DELEGATION_LABEL);
        pluginService.savePluginConfiguration(catalogSecuDelegConf);
    }

    @Test
    @Requirement("REGARDS_DSL_STO_AIP_080")
    public void testStoreUnvalid() {
        aip.getProperties().getContentInformations().forEach(ci -> ci.getDataObject().setChecksum(null));
        // make requirements
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isUnprocessableEntity());
        requestBuilderCustomizer.customizeHeaders().putAll(getHeaders());
        // perform request
        performDefaultPost(AIPController.AIP_PATH,
                           new AIPCollection(aip),
                           requestBuilderCustomizer,
                           "AIP storage should have been schedule properly");
    }

    @Test
    public void testStore() {
        // make requirements
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isCreated());
        requestBuilderCustomizer.customizeHeaders().putAll(getHeaders());
        // perform request
        performDefaultPost(AIPController.AIP_PATH,
                           new AIPCollection(aip),
                           requestBuilderCustomizer,
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
        performDefaultPost(AIPController.AIP_PATH,
                           new AIPCollection(aip),
                           requestBuilderCustomizer,
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
        performDefaultPost(AIPController.AIP_PATH,
                           new AIPCollection(aip, aip2),
                           requestBuilderCustomizer,
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
    @Requirement("REGARDS_DSL_STO_AIP_140")
    public void testMakeAvailable() throws InterruptedException {
        // ask for an aip to be stored
        testStore();
        // wait for the AIP
        Thread.sleep(20000);
        // get the datafiles checksum
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        Set<StorageDataFile> dataFiles = dataFileDao.findAllByAip(aip);
        Set<String> dataFilesChecksum = dataFiles.stream().map(df -> df.getChecksum()).collect(Collectors.toSet());
        // ask for availability
        AvailabilityRequest availabilityRequest = new AvailabilityRequest(OffsetDateTime.now().plusDays(2),
                                                                          dataFilesChecksum
                                                                                  .toArray(new String[dataFilesChecksum
                                                                                          .size()]));
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.content()
                                                        .json(gson.toJson(new AvailabilityResponse(Sets.newHashSet(),
                                                                                                   dataFiles,
                                                                                                   Sets.newHashSet()))));
        performDefaultPost(AIPController.AIP_PATH + AIPController.PREPARE_DATA_FILES,
                           availabilityRequest,
                           requestBuilderCustomizer,
                           "data should already be available as they are in an online data storage");
    }

    @Test
    @Requirement("REGARDS_DSL_STO_AIP_110")
    public void testRetrieveAip() {
        testStore();
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.
                addDocumentationSnippet(RequestDocumentation
                                                .pathParameters(RequestDocumentation.parameterWithName("ip_id")
                                                                        .description("the AIP identifier")
                                                                        .attributes(Attributes
                                                                                            .key(RequestBuilderCustomizer.PARAM_TYPE)
                                                                                            .value(String.class
                                                                                                           .getSimpleName()),
                                                                                    Attributes
                                                                                            .key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                                                            .value("Should respect UniformResourceName pattern"))));
        performDefaultGet(AIPController.AIP_PATH + AIPController.ID_PATH,
                          requestBuilderCustomizer,
                          "we should have the aip",
                          aip.getId().toString());
    }

    @Test
    @Requirement("REGARDS_DSL_STOP_AIP_115")
    public void testRetrieveDeletedAip() throws InterruptedException {
        testDelete();
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        // first we expect that the aip has a DELETION event in its history
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(
                "$.properties.pdi.provenanceInformation.history[*].type",
                IsCollectionContaining.hasItem(EventType.DELETION.name())));
        // now we expect that those events does have a date
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(
                "$.properties.pdi.provenanceInformation.history[?(@.type == \"" + EventType.DELETION.name()
                        + "\")].date",
                NotNull.NOT_NULL));
        performDefaultGet(AIPController.AIP_PATH + AIPController.ID_PATH,
                          requestBuilderCustomizer,
                          "we should have the aip",
                          aip.getId().toString());
    }

    @Test
    @Requirement("REGARDS_DSL_STO_AIP_560")
    @Purpose("System allow to retrieve aips thanks to a list of ip id")
    public void testRetrieveAipBulk() {
        testStore();
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        performDefaultPost(AIPController.AIP_PATH + AIPController.AIP_BULK,
                           Sets.newHashSet(aip.getId().toString()),
                           requestBuilderCustomizer,
                           "we should have the aips");
    }

    @Test
    @Requirement("REGARDS_DSL_STO_AIP_560")
    @Purpose("System allow to retrieve aips thanks to a tag")
    public void testRetrieveAipTag() {
        testStore();
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.
                addDocumentationSnippet(RequestDocumentation
                                                .pathParameters(RequestDocumentation.parameterWithName("tag")
                                                                        .description(
                                                                                "the tag with which AIPs should be tagged")
                                                                        .attributes(Attributes
                                                                                            .key(RequestBuilderCustomizer.PARAM_TYPE)
                                                                                            .value(String.class
                                                                                                           .getSimpleName())),
                                                                RequestDocumentation.parameterWithName("ip_id")
                                                                        .description("the AIP identifier")
                                                                        .attributes(Attributes
                                                                                            .key(RequestBuilderCustomizer.PARAM_TYPE)
                                                                                            .value(String.class
                                                                                                           .getSimpleName()),
                                                                                    Attributes
                                                                                            .key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                                                            .value("Should respect UniformResourceName pattern"))));

        performDefaultGet(AIPController.AIP_PATH + AIPController.TAG,
                          requestBuilderCustomizer,
                          "we should have the aips",
                          aip.getId().toString(),
                          "tag");
    }

    @Test
    public void testDelete() throws InterruptedException {
        testStore();
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        int wait = 0;
        // lets wait for this AIP to be stored
        while ((aipDao.findOneByIpId(aip.getId().toString()).get().getState() != AIPState.STORED) && (wait
                < MAX_WAIT)) {
            Thread.sleep(1000);
            wait += 1000;
        }
        Assert.assertTrue("AIP was not fully stored in time: " + wait, wait < MAX_WAIT);
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isNoContent());
        requestBuilderCustomizer.
                addDocumentationSnippet(RequestDocumentation
                                                .pathParameters(RequestDocumentation.parameterWithName("ip_id")
                                                                        .description("the AIP identifier")
                                                                        .attributes(Attributes
                                                                                            .key(RequestBuilderCustomizer.PARAM_TYPE)
                                                                                            .value(String.class
                                                                                                           .getSimpleName()),
                                                                                    Attributes
                                                                                            .key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                                                            .value("Should respect UniformResourceName pattern"))));
        performDefaultDelete(AIPController.AIP_PATH + AIPController.ID_PATH,
                             requestBuilderCustomizer,
                             "deletion of this aip should be possible",
                             aip.getId().toString());
    }

    @Test
    public void testBulkDelete() {
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isNoContent());

        Set<String> sipIpIds = new HashSet<>();
        sipIpIds.add("SIPIPIDTEST1");
        sipIpIds.add("SIPIPIDTEST2");
        performDefaultPost(AIPController.AIP_PATH + AIPController.AIP_BULK_DELETE,
                           sipIpIds,
                           requestBuilderCustomizer,
                           "AIPs should be deleted");
    }

    @Test
    @Requirement("REGARDS_DSL_STO_AIP_120")
    public void testRetrieveAIPFiles() {
        testStore();
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.
                addDocumentationSnippet(RequestDocumentation
                                                .pathParameters(RequestDocumentation.parameterWithName("ip_id")
                                                                        .description("the AIP identifier")
                                                                        .attributes(Attributes
                                                                                            .key(RequestBuilderCustomizer.PARAM_TYPE)
                                                                                            .value(String.class
                                                                                                           .getSimpleName()),
                                                                                    Attributes
                                                                                            .key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                                                            .value("Should respect UniformResourceName pattern"))));
        performDefaultGet(AIPController.AIP_PATH + AIPController.OBJECT_LINK_PATH,
                          requestBuilderCustomizer,
                          "we should have the metadata of the files of the aip",
                          aip.getId().toString());
    }

    @Test
    @Requirement("REGARDS_DSL_STO_AIP_150")
    public void testRetrieveAIPVersionHistory() {
        testStore();
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.
                addDocumentationSnippet(RequestDocumentation
                                                .pathParameters(RequestDocumentation.parameterWithName("ip_id")
                                                                        .description("the AIP identifier")
                                                                        .attributes(Attributes
                                                                                            .key(RequestBuilderCustomizer.PARAM_TYPE)
                                                                                            .value(String.class
                                                                                                           .getSimpleName()),
                                                                                    Attributes
                                                                                            .key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                                                            .value("Should respect UniformResourceName pattern"))));
        performDefaultGet(AIPController.AIP_PATH + AIPController.VERSION_PATH,
                          requestBuilderCustomizer,
                          "we should have the different versions of an aip",
                          aip.getId().toString());
    }

    @Test
    @Requirement("REGARDS_DSL_STO_AIP_160")
    public void testRetrieveAIPHistory() {
        testStore();
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.
                addDocumentationSnippet(RequestDocumentation
                                                .pathParameters(RequestDocumentation.parameterWithName("ip_id")
                                                                        .description("the AIP identifier")
                                                                        .attributes(Attributes
                                                                                            .key(RequestBuilderCustomizer.PARAM_TYPE)
                                                                                            .value(String.class
                                                                                                           .getSimpleName()),
                                                                                    Attributes
                                                                                            .key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                                                            .value("Should respect UniformResourceName pattern"))));
        performDefaultGet(AIPController.AIP_PATH + AIPController.HISTORY_PATH,
                          requestBuilderCustomizer,
                          "we should have the history of an aip",
                          aip.getId().toString());
    }

    @Test
    @Requirement("REGARDS_DSL_STO_ARC_200")
    @Requirement("REGARDS_DSL_STO_AIP_130")
    public void testDownload() throws InterruptedException {
        // lets make files available
        // first lets make available the file
        testMakeAvailable();
        // lets ask for download now
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        // lets get the datafile checksum
        Set<StorageDataFile> dataFiles = dataFileDao.findAllByAip(aip);
        StorageDataFile dataFile = dataFiles.toArray(new StorageDataFile[dataFiles.size()])[0];
        // now lets download it!
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.customizeHeaders().putAll(getHeaders());
        requestBuilderCustomizer.customizeHeaders()
                .put(HttpConstants.ACCEPT, Lists.newArrayList(dataFile.getMimeType().toString()));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.
                addDocumentationSnippet(RequestDocumentation
                                                .pathParameters(RequestDocumentation.parameterWithName("ip_id")
                                                                        .description("the AIP identifier")
                                                                        .attributes(Attributes
                                                                                            .key(RequestBuilderCustomizer.PARAM_TYPE)
                                                                                            .value(String.class
                                                                                                           .getSimpleName()),
                                                                                    Attributes
                                                                                            .key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                                                            .value("Should respect UniformResourceName pattern")),
                                                                RequestDocumentation.parameterWithName("checksum")
                                                                        .description("the file to download checksum.")
                                                                        .attributes(Attributes
                                                                                            .key(RequestBuilderCustomizer.PARAM_TYPE)
                                                                                            .value(String.class
                                                                                                           .getSimpleName()))));
        performDefaultGet(AIPController.AIP_PATH + AIPController.DOWNLOAD_AIP_FILE,
                          requestBuilderCustomizer,
                          "We should be downloading the data file",
                          aip.getId().toString(),
                          dataFile.getChecksum());
    }

    @Test
    public void testRetrieveAips() {
        testStore();
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.customizeRequestParam()
                .param("from", OffsetDateTimeAdapter.format(OffsetDateTime.now().minusDays(40)))
                .param("to", OffsetDateTimeAdapter.format(OffsetDateTime.now()));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer
                .addExpectation(MockMvcResultMatchers.jsonPath("$.content", Matchers.not(Matchers.empty())));
        requestBuilderCustomizer.
                addDocumentationSnippet(RequestDocumentation
                                                .requestParameters(RequestDocumentation.parameterWithName("state")
                                                                           .description("state the aips should be in")
                                                                           .attributes(Attributes
                                                                                               .key(RequestBuilderCustomizer.PARAM_TYPE)
                                                                                               .value(String.class
                                                                                                              .getSimpleName()),
                                                                                       Attributes
                                                                                               .key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                                                               .value("Available values: "
                                                                                                              + Arrays
                                                                                                       .stream(AIPState.values())
                                                                                                       .map(aipState -> aipState
                                                                                                               .getName())
                                                                                                       .reduce((first, second) ->
                                                                                                                       first
                                                                                                                               + ", "
                                                                                                                               + second)
                                                                                                       .get()))
                                                                           .optional(),
                                                                   RequestDocumentation.parameterWithName("from")
                                                                           .description(
                                                                                   "date after which the aip should have been added to the system")
                                                                           .attributes(Attributes
                                                                                               .key(RequestBuilderCustomizer.PARAM_TYPE)
                                                                                               .value(String.class
                                                                                                              .getSimpleName()),
                                                                                       Attributes
                                                                                               .key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                                                               .value("Should respect UTC format"))
                                                                           .optional(),
                                                                   RequestDocumentation.parameterWithName("to")
                                                                           .description(
                                                                                   "date before which the aip should have been added to the system")
                                                                           .attributes(Attributes
                                                                                               .key(RequestBuilderCustomizer.PARAM_TYPE)
                                                                                               .value(String.class
                                                                                                              .getSimpleName()),
                                                                                       Attributes
                                                                                               .key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                                                               .value("Should respect UTC format"))
                                                                           .optional()));
        performDefaultGet(AIPController.AIP_PATH, requestBuilderCustomizer, "There should be some AIP to show");
    }

    @After
    public void cleanUp() throws URISyntaxException, IOException {
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        subscriber.purgeQueue(DataStorageEvent.class, DataStorageEventHandler.class);
        jobInfoRepo.deleteAll();
        dataFileDao.deleteAll();
        aipDao.deleteAll();
        prioritizedDataStorageRepository.deleteAll();
        pluginRepo.deleteAll();
        if (baseStorageLocation != null) {
            Files.walk(Paths.get(baseStorageLocation.toURI())).sorted(Comparator.reverseOrder()).map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    private AIP getAIP() throws MalformedURLException {

        AIPBuilder aipBuilder = new AIPBuilder(new UniformResourceName(OAISIdentifier.AIP,
                                                                       EntityType.DATA,
                                                                       DEFAULT_TENANT,
                                                                       UUID.randomUUID(),
                                                                       1), null, EntityType.DATA);

        String path = System.getProperty("user.dir") + "/src/test/resources/data.txt";
        aipBuilder.getContentInformationBuilder()
                .setDataObject(DataType.RAWDATA, new URL("file", "", path), "MD5", "de89a907d33a9716d11765582102b2e0");
        aipBuilder.getContentInformationBuilder().setSyntax("text", "description", MimeType.valueOf("text/plain"));
        aipBuilder.addContentInformation();
        aipBuilder.addTags("tag");
        aipBuilder.getPDIBuilder().setAccessRightInformation("public");
        aipBuilder.getPDIBuilder().setFacility("CS");
        aipBuilder.getPDIBuilder()
                .addProvenanceInformationEvent(EventType.SUBMISSION.name(), "test event", OffsetDateTime.now());

        return aipBuilder.build();
    }

    @Configuration
    static class Config {

        @Bean
        public IProjectsClient projectsClient() {
            return Mockito.mock(IProjectsClient.class);
        }

        @Bean
        public INotificationClient notificationClient() {
            return Mockito.mock(INotificationClient.class);
        }
    }

}
