/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.compress.utils.Lists;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import feign.Response;
import fr.cnes.regards.framework.feign.FeignClientBuilder;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
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
import fr.cnes.regards.framework.oais.OAISDataObject;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.test.integration.AbstractRegardsWebIT;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.storage.dao.IAIPDao;
import fr.cnes.regards.modules.storage.dao.IDataFileDao;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPBuilder;
import fr.cnes.regards.modules.storage.domain.AIPCollection;
import fr.cnes.regards.modules.storage.domain.AvailabilityRequest;
import fr.cnes.regards.modules.storage.domain.AvailabilityResponse;
import fr.cnes.regards.modules.storage.plugin.datastorage.IDataStorage;
import fr.cnes.regards.modules.storage.plugin.datastorage.IOnlineDataStorage;
import fr.cnes.regards.modules.storage.plugin.datastorage.local.LocalDataStorage;
import fr.cnes.regards.modules.storage.plugin.security.ISecurityDelegation;
import fr.cnes.regards.modules.storage.service.DefaultAllocationStrategyPlugin;
import fr.cnes.regards.modules.storage.service.IAllocationStrategy;

/**
 * Run client IT scenario tests for store and restore files from rs-storage microservice.
 * @author Sébastien Binda
 */
@TestPropertySource("classpath:test.properties")
@ActiveProfiles("testAmqp")
public class AipClientIT extends AbstractRegardsWebIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(AipClientIT.class);

    private static final String CATALOG_SECURITY_DELEGATION_LABEL = "AipClientIT";

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    private final static String workspace = "target/workspace";

    private IAipClient client;

    @Autowired
    private IPluginService pluginService;

    /**
     * Feign security manager
     */
    @Autowired
    private FeignSecurityManager feignSecurityManager;

    @Value("${server.address}")
    private String serverAddress;

    @Autowired
    private Gson gson;

    @Autowired
    private IPluginConfigurationRepository pluginRepo;

    @Autowired
    private IAIPDao aipDao;

    @Autowired
    private IDataFileDao dataFileDao;

    @Autowired
    private IJobInfoRepository jobInfoRepo;

    private static URL baseStorageLocation = null;

    private static Path downloadDir = Paths.get("target/download");

    private PluginConfiguration catalogSecuDelegConf;

    @BeforeClass
    public static void initAll() throws IOException {
        if (Paths.get(workspace).toFile().exists()) {
            FileUtils.deleteDirectory(Paths.get(workspace).toFile());
        }
        Files.createDirectory(Paths.get(workspace));
        baseStorageLocation = new URL("file", "", Paths.get("target/AIPServiceIT").toFile().getAbsolutePath());
    }

    @Before
    public void init() throws ModuleException, IOException, URISyntaxException {
        if ((baseStorageLocation != null) && Paths.get(baseStorageLocation.toURI()).toFile().exists()) {
            FileUtils.deleteDirectory(Paths.get(baseStorageLocation.toURI()).toFile());
        }

        if (downloadDir.toFile().exists()) {
            FileUtils.deleteDirectory(downloadDir.toFile());
        }
        Files.createDirectories(downloadDir);
        client = FeignClientBuilder
                .build(new TokenClientProvider<>(IAipClient.class, "http://" + serverAddress + ":" + getPort(),
                                                 feignSecurityManager), gson);
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        FeignSecurityManager.asSystem();
        initDb();

    }

    private void initDb() throws ModuleException, IOException, URISyntaxException {

        cleanUp();
        // second, lets store a plugin configuration for IAllocationStrategy
        pluginService.addPluginPackage(IAllocationStrategy.class.getPackage().getName());
        pluginService.addPluginPackage(DefaultAllocationStrategyPlugin.class.getPackage().getName());
        pluginService.addPluginPackage(IDataStorage.class.getPackage().getName());
        pluginService.addPluginPackage(IOnlineDataStorage.class.getPackage().getName());
        pluginService.addPluginPackage(LocalDataStorage.class.getPackage().getName());
        pluginService.addPluginPackage(ISecurityDelegation.class.getPackage().getName());
        pluginService.addPluginPackage(FalseSecurityDelegation.class.getPackage().getName());
        PluginMetaData catalogSecuDelegMeta = PluginUtils.createPluginMetaData(FalseSecurityDelegation.class,
                                                                               FalseSecurityDelegation.class
                                                                                       .getPackage().getName(),
                                                                               ISecurityDelegation.class.getPackage()
                                                                                       .getName());
        catalogSecuDelegConf = new PluginConfiguration(catalogSecuDelegMeta, CATALOG_SECURITY_DELEGATION_LABEL);
        catalogSecuDelegConf = pluginService.savePluginConfiguration(catalogSecuDelegConf);
        PluginMetaData allocationMeta = PluginUtils.createPluginMetaData(DefaultAllocationStrategyPlugin.class,
                                                                         DefaultAllocationStrategyPlugin.class
                                                                                 .getPackage().getName());
        PluginConfiguration allocationConfiguration = new PluginConfiguration(allocationMeta, "allocation");
        allocationConfiguration.setIsActive(true);
        pluginService.savePluginConfiguration(allocationConfiguration);
        // third, lets store a plugin configuration of IDataStorage with the highest priority
        PluginMetaData dataStoMeta = PluginUtils
                .createPluginMetaData(LocalDataStorage.class, IDataStorage.class.getPackage().getName(),
                                      IOnlineDataStorage.class.getPackage().getName());

        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(LocalDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                              gson.toJson(baseStorageLocation))
                .addParameter(LocalDataStorage.LOCAL_STORAGE_OCCUPIED_SPACE_THRESHOLD, "90").getParameters();
        PluginConfiguration dataStorageConf = new PluginConfiguration(dataStoMeta, "dsLabel", parameters, 0);
        dataStorageConf.setIsActive(true);
        pluginService.savePluginConfiguration(dataStorageConf);
    }

    public Path initTestFile() throws IOException {
        Path filePath = Paths.get("target/file1.test");
        if (filePath.toFile().exists()) {
            filePath.toFile().delete();
        }
        Path fileCreated = Files.createFile(filePath);
        List<String> lines = Lists.newArrayList();
        lines.add("Ligne 1 test");
        // check special character.
        lines.add("Ligne 2 test numéo 2 !!!!");
        Files.write(fileCreated, lines);

        return fileCreated;
    }

    /**
     * Client full test scenario.
     * <ul>
     * <li>1. Store a new AIP</li>
     * <li>2. Retrieve created new AIP</li>
     * <li>3. Retrieve files associated to the new AIP</li>
     * <li>4. Make files available for download</li>
     * <li>5. Donwload files</li>
     * </ul>
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    @Test
    public void testCreateAIP() throws IOException, NoSuchAlgorithmException {
        // Create new AIP
        AIPBuilder builder = new AIPBuilder(
                new UniformResourceName(OAISIdentifier.AIP, EntityType.DATASET, DEFAULT_TENANT, UUID.randomUUID(), 1),
                "clientAipTest", EntityType.DATA);
        // Init a test file to add with the new AIP.
        Path file = initTestFile();

        String fileChecksum = "";
        try (FileInputStream is = new FileInputStream(file.toFile())) {
            fileChecksum = ChecksumUtils.computeHexChecksum(is, "MD5");
        }
        builder.getContentInformationBuilder()
                .setDataObject(DataType.RAWDATA, new URL("file://" + file.toFile().getAbsolutePath()), "MD5",
                               fileChecksum);
        builder.getContentInformationBuilder().setSyntax("application/text", "text", "application/text");
        builder.addContentInformation();

        builder.addEvent(EventType.SUBMISSION.toString(), "Creation", OffsetDateTime.now());
        AIP aip = builder.build();
        Set<AIP> aips = Sets.newHashSet(aip);
        Assert.assertFalse("AIP should not exists before test",
                           aipDao.findOneByIpId(aip.getId().toString()).isPresent());

        // 1. Create new AIP
        AIPCollection aipCollection = new AIPCollection();
        aipCollection.addAll(aips);
        ResponseEntity<Set<UUID>> resp = client.store(aipCollection);
        // Wait for job ends.
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            // Nothing to do
        }
        Assert.assertTrue("Http response should be OK after createAIP.", HttpStatus.OK.equals(resp.getStatusCode()));
        Assert.assertTrue("AIP is not created", aipDao.findOneByIpId(aip.getId().toString()).isPresent());
        // 2. Retrieve it
        ResponseEntity<PagedResources<Resource<AIP>>> resp2 = client.retrieveAIPs(null, null, null, 0, 10);
        Assert.assertTrue("Http response should be OK adter retrieveAIPs.",
                          HttpStatus.OK.equals(resp2.getStatusCode()));
        Assert.assertTrue("There should be only one AIP retrieve, the one created before",
                          resp2.getBody().getMetadata().getTotalElements() == 1);
        aip = aipDao.findOneByIpId(aip.getId().toString()).get();

        // 3. Retrieve associated files (RAWDATA and AIP metadata)
        ResponseEntity<List<OAISDataObject>> resp3 = client.retrieveAIPFiles(aip.getId().toString());
        Assert.assertTrue("Http response should be OK adter retrieveAIPFiles.",
                          HttpStatus.OK.equals(resp3.getStatusCode()));
        Assert.assertTrue("There should be one DataObject from the AIP.", resp3.getBody() != null);
        Assert.assertTrue(
                String.format("There should be two DataObjects from the AIP(stored file and metadata file) not %s.",
                              resp3.getBody().size()), resp3.getBody().size() == 2);
        // 4. Make file available for download
        AvailabilityRequest request = new AvailabilityRequest(OffsetDateTime.now().plusDays(2), fileChecksum);
        ResponseEntity<AvailabilityResponse> response = client.makeFilesAvailable(request);
        Assert.assertTrue("Http response should be OK adter makeFilesAvailable.",
                          HttpStatus.OK.equals(response.getStatusCode()));
        Assert.assertTrue("There should be files available directly from response.", response.getBody() != null);
        Assert.assertTrue("There should be files available directly from response.",
                          response.getBody().getAlreadyAvailable().size() == 1);
        Assert.assertTrue("There should not be files in error from response.",
                          response.getBody().getErrors().isEmpty());

        // 5. Download file
        Response downloadResponse = client.downloadFile(aip.getId().toString(), fileChecksum);
        // Assert.assertTrue("Http response should be OK for download.", HttpStatus.OK.equals(downloadResponse.status()));

        Map<String, Collection<String>> headers = downloadResponse.headers();
        Collection<String> cds = headers.get("content-disposition");
        String fileName = null;
        for (String cd : cds) {
            Pattern pattern = Pattern.compile("^(.*)filename=\"([^\"]*)\"(.*)$");
            Matcher matcher = pattern.matcher(cd);
            if (matcher.matches()) {
                fileName = matcher.group(2);
            }
        }
        Assert.assertTrue("Unable to retrieve file name from response headers", fileName != null);

        File result = new File(downloadDir.toString() + "/download-" + fileName);
        try (InputStream is = downloadResponse.body().asInputStream()) {
            Files.copy(is, result.toPath());
        }

        try (FileInputStream ris = new FileInputStream(result)) {
            String newFileChecksum = ChecksumUtils.computeHexChecksum(ris, "MD5");
            Assert.assertTrue("Invalid downloaded file", fileChecksum.equals(newFileChecksum));
        }

    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @After
    public void cleanUp() throws URISyntaxException, IOException {
        jobInfoRepo.deleteAll();
        dataFileDao.deleteAll();
        aipDao.deleteAll();
        pluginRepo.deleteAll();
    }

}
