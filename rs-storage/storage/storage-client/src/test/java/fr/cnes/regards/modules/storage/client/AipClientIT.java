/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.compress.utils.Lists;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MimeType;

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
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.notification.client.INotificationClient;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.storage.dao.IAIPDao;
import fr.cnes.regards.modules.storage.dao.IDataFileDao;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPBuilder;
import fr.cnes.regards.modules.storage.domain.AIPCollection;
import fr.cnes.regards.modules.storage.domain.AvailabilityRequest;
import fr.cnes.regards.modules.storage.domain.AvailabilityResponse;
import fr.cnes.regards.modules.storage.domain.RejectedAip;
import fr.cnes.regards.modules.storage.plugin.allocation.strategy.DefaultAllocationStrategyPlugin;
import fr.cnes.regards.modules.storage.plugin.datastorage.local.LocalDataStorage;
import fr.cnes.regards.modules.storage.service.IPrioritizedDataStorageService;

/**
 * Run client IT scenario tests for storeAndCreate and restore files from rs-storage microservice.
 * @author Sébastien Binda
 */
@TestPropertySource("classpath:test.properties")
@ActiveProfiles("testAmqp")
@Ignore("TODO: fix timing")
public class AipClientIT extends AbstractRegardsWebIT {

    /**
     * Class logger
     */
    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(AipClientIT.class);

    private static final String CATALOG_SECURITY_DELEGATION_LABEL = "AipClientIT";

    private final static String workspace = "target/workspace";

    private static URL baseStorageLocation = null;

    private static Path downloadDir = Paths.get("target/download");

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

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

    private PluginConfiguration catalogSecuDelegConf;

    @Autowired
    private IPrioritizedDataStorageService prioritizedDataStorageService;

    @BeforeClass
    public static void initAll() throws IOException {
        if (Paths.get(workspace).toFile().exists()) {
            Files.walk(Paths.get(workspace)).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);

        }
        Files.createDirectory(Paths.get(workspace));
        baseStorageLocation = new URL("file", "", Paths.get("target/AIPServiceIT").toFile().getAbsolutePath());
    }

    @Before
    public void init() throws ModuleException, IOException, URISyntaxException {
        if (baseStorageLocation != null && Paths.get(baseStorageLocation.toURI()).toFile().exists()) {
            Files.walk(Paths.get(baseStorageLocation.toURI())).sorted(Comparator.reverseOrder()).map(Path::toFile)
                    .forEach(File::delete);
        }

        if (downloadDir.toFile().exists()) {
            Files.walk(downloadDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
        Files.createDirectories(downloadDir);
        client = FeignClientBuilder.build(
                                          new TokenClientProvider<>(IAipClient.class,
                                                  "http://" + serverAddress + ":" + getPort(), feignSecurityManager),
                                          gson);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        FeignSecurityManager.asSystem();
        initDb();

    }

    private void initDb() throws ModuleException, IOException, URISyntaxException {

        cleanUp();
        // second, lets storeAndCreate a plugin configuration for IAllocationStrategy
        PluginMetaData catalogSecuDelegMeta = PluginUtils.createPluginMetaData(FalseSecurityDelegation.class);
        catalogSecuDelegConf = new PluginConfiguration(catalogSecuDelegMeta, CATALOG_SECURITY_DELEGATION_LABEL);
        catalogSecuDelegConf = pluginService.savePluginConfiguration(catalogSecuDelegConf);
        PluginMetaData allocationMeta = PluginUtils.createPluginMetaData(DefaultAllocationStrategyPlugin.class);
        PluginConfiguration allocationConfiguration = new PluginConfiguration(allocationMeta, "allocation");
        allocationConfiguration.setIsActive(true);
        pluginService.savePluginConfiguration(allocationConfiguration);
        // third, lets storeAndCreate a plugin configuration of IDataStorage with the highest priority
        PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(LocalDataStorage.class);

        Set<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(LocalDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME, baseStorageLocation.toString())
                .addParameter(LocalDataStorage.LOCAL_STORAGE_TOTAL_SPACE, 90000000000000L).getParameters();
        PluginConfiguration dataStorageConf = new PluginConfiguration(dataStoMeta, "dsLabel", parameters, 0);
        dataStorageConf.setIsActive(true);
        prioritizedDataStorageService.create(dataStorageConf);
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
     * @throws InterruptedException
     */
    @Test
    public void testCreateAIP() throws IOException, NoSuchAlgorithmException, InterruptedException {

        UniformResourceName sipId = new UniformResourceName(OAISIdentifier.SIP, EntityType.DATASET, getDefaultTenant(),
                UUID.randomUUID(), 1);
        UniformResourceName aipId = new UniformResourceName(OAISIdentifier.AIP, EntityType.DATASET, getDefaultTenant(),
                sipId.getEntityId(), 1);

        // Create new AIP
        AIPBuilder builder = new AIPBuilder(aipId, Optional.of(sipId), "clientAipTest", EntityType.DATA, "Session 1");
        // Init a test file to add with the new AIP.
        Path file = initTestFile();

        String fileChecksum = "";
        try (FileInputStream is = new FileInputStream(file.toFile())) {
            fileChecksum = ChecksumUtils.computeHexChecksum(is, "MD5");
        }
        builder.getContentInformationBuilder().setDataObject(DataType.RAWDATA, file.toAbsolutePath(), "MD5",
                                                             fileChecksum);
        builder.getContentInformationBuilder().setSyntax("application/text", "text",
                                                         MimeType.valueOf("application/text"));
        builder.addContentInformation();

        builder.addEvent(EventType.SUBMISSION.toString(), "Creation", OffsetDateTime.now());
        AIP aip = builder.build();
        Set<AIP> aips = Sets.newHashSet(aip);
        Assert.assertFalse("AIP should not exists before test",
                           aipDao.findOneByAipId(aip.getId().toString()).isPresent());

        // 1. Create new AIP
        AIPCollection aipCollection = new AIPCollection();
        aipCollection.addAll(aips);
        ResponseEntity<List<RejectedAip>> resp = client.store(aipCollection);
        Assert.assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        // Wait for job ends.
        Thread.sleep(10000);
        // 2. Retrieve it
        ResponseEntity<PagedResources<Resource<AIP>>> resp2 = client.retrieveAIPs(null, null, null, 0, 10);
        Assert.assertTrue("Http response should be OK adter retrieveAIPs.",
                          HttpStatus.OK.equals(resp2.getStatusCode()));
        Assert.assertTrue("There should be only one AIP retrieve, the one created before",
                          resp2.getBody().getMetadata().getTotalElements() == 1);
        aip = aipDao.findOneByAipId(aip.getId().toString()).get();

        // 3. Retrieve associated files (RAWDATA and AIP metadata)
        ResponseEntity<List<OAISDataObject>> resp3 = client.retrieveAIPFiles(aip.getId().toString());
        Assert.assertTrue("Http response should be OK adter retrieveAIPFiles.",
                          HttpStatus.OK.equals(resp3.getStatusCode()));
        Assert.assertTrue("There should be one DataObject from the AIP.", resp3.getBody() != null);
        Assert.assertTrue(String
                .format("There should be two DataObjects from the AIP(stored file and metadata file) not %s.",
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
        // Assert.assertTrue("Http response should be OK for download.",
        // HttpStatus.OK.equals(downloadResponse.status()));

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

    @After
    public void cleanUp() throws URISyntaxException, IOException {
        jobInfoRepo.deleteAll();
        dataFileDao.deleteAll();
        aipDao.deleteAll();
        pluginRepo.deleteAll();
    }

    @Configuration
    static class Conf {

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
