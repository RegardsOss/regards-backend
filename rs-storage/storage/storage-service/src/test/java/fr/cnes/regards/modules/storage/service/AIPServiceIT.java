/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Sets;
import com.google.gson.Gson;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.RegardsAmqpAdmin;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.ContentInformation;
import fr.cnes.regards.framework.oais.EventType;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.test.report.annotation.Requirements;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.storage.dao.IAIPDao;
import fr.cnes.regards.modules.storage.dao.IDataFileDao;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPBuilder;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.domain.database.DataFileState;
import fr.cnes.regards.modules.storage.plugin.datastorage.IDataStorage;
import fr.cnes.regards.modules.storage.plugin.datastorage.IOnlineDataStorage;
import fr.cnes.regards.modules.storage.plugin.datastorage.local.LocalDataStorage;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@ContextConfiguration(classes = { TestConfig.class })
@TestPropertySource(locations = "classpath:test.properties")
@ActiveProfiles("testAmqp")
@DirtiesContext
public class AIPServiceIT extends AbstractRegardsServiceTransactionalIT {

    private static final Logger LOG = LoggerFactory.getLogger(AIPServiceIT.class);

    private static final String ALLOCATION_CONF_LABEL = "AIPServiceIT_ALLOCATION";

    private static final String DATA_STORAGE_CONF_LABEL = "AIPServiceIT_DATA_STORAGE";

    private static final int MAX_WAIT_TEST = 10000;

    @Autowired
    private Gson gson;

    @Autowired
    private IAIPService aipService;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IPluginConfigurationRepository pluginRepo;

    @Autowired
    private IAIPDao aipDao;

    @Autowired
    private IDataFileDao dataFileDao;

    @Autowired
    private IJobInfoRepository jobInfoRepo;

    @Autowired
    private ISubscriber subscriber;

    @Value("${regards.storage.workspace}")
    private String workspace;

    private AIP aip;

    private URL baseStorageLocation;

    private final StoreJobEventHandler handler = new StoreJobEventHandler();

    @Autowired
    private IRabbitVirtualHostAdmin vHost;

    @Autowired
    private RegardsAmqpAdmin amqpAdmin;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Before
    public void init() throws IOException, ModuleException, URISyntaxException, InterruptedException {
        tenantResolver.forceTenant(DEFAULT_TENANT);
        //         this.cleanUp(); //comment if you are not interrupting tests during their execution
        subscriber.subscribeTo(JobEvent.class, handler);
        initDb();
    }

    private void initDb() throws ModuleException, IOException, URISyntaxException {

        // first of all, lets get an AIP with accessible dataObjects and real checksums
        aip = getAIP();
        // second, lets store a plugin configuration for IAllocationStrategy
        PluginMetaData allocationMeta = PluginUtils
                .createPluginMetaData(DefaultAllocationStrategyPlugin.class,
                                      DefaultAllocationStrategyPlugin.class.getPackage().getName());
        PluginConfiguration allocationConfiguration = new PluginConfiguration(allocationMeta, ALLOCATION_CONF_LABEL);
        allocationConfiguration.setIsActive(true);
        pluginService.savePluginConfiguration(allocationConfiguration);
        // third, lets store a plugin configuration of IDataStorage with the highest priority
        PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(LocalDataStorage.class,
                                                                      IDataStorage.class.getPackage().getName(),
                                                                      IOnlineDataStorage.class.getPackage().getName());
        baseStorageLocation = new URL("file", "", Paths.get("target/AIPServiceIT").toFile().getAbsolutePath());
        Files.createDirectories(Paths.get(baseStorageLocation.toURI()));
        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(LocalDataStorage.LOCAL_STORAGE_OCCUPIED_SPACE_THRESHOLD, "90")
                .addParameter(LocalDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                              gson.toJson(baseStorageLocation))
                .getParameters();
        PluginConfiguration dataStorageConf = new PluginConfiguration(dataStoMeta, DATA_STORAGE_CONF_LABEL, parameters,
                0);
        dataStorageConf.setIsActive(true);
        pluginService.savePluginConfiguration(dataStorageConf);
    }

    @Test
    @Requirements({ @Requirement("REGARDS_DSL_STO_AIP_010") })
    public void createSuccessTest() throws ModuleException, InterruptedException {
        Set<UUID> jobIds = aipService.store(Sets.newHashSet(aip));
        jobIds.forEach(job -> LOG.info("Waiting for job {} end", job.toString()));
        int wait = 0;
        while (!handler.getJobSucceeds().containsAll(jobIds) && !handler.isFailed() && (wait < 10000)) {
            // lets wait for 1 sec before checking again if all our jobs has been done or not
            Thread.sleep(1000);
            wait = wait + 1000;
        }
        Assert.assertFalse("The job failed while it should not have", handler.isFailed());
        LOG.info("All waiting JOB succeeded");
        Optional<AIP> aipFromDB = aipDao.findOneByIpId(aip.getId().toString());
        // Wait for AIP set STORED status
        LOG.info("Waiting for AIP {} stored", aip.getId().toString());
        wait = 0;
        while (!AIPState.STORED.equals(aipFromDB.get().getState()) && (wait < 10000)) {
            aipFromDB = aipDao.findOneByIpId(aip.getId().toString());
            Thread.sleep(1000);
            wait = wait + 1000;
        }
        Assert.assertNotEquals(MAX_WAIT_TEST, wait);
        Assert.assertEquals(AIPState.STORED, aipFromDB.get().getState());
        LOG.info("AIP {} stored", aip.getId().toString());
        Set<DataFile> dataFiles = dataFileDao.findAllByStateAndAip(DataFileState.STORED, aip);
        Assert.assertEquals(2, dataFiles.size());
    }

    @Test
    public void createFailOnDataTest() throws MalformedURLException, ModuleException, InterruptedException {
        // first lets change the data location to be sure it fails
        aip.getProperties().getContentInformations()
                .toArray(new ContentInformation[aip.getProperties().getContentInformations().size()])[0].getDataObject()
                        .setUrl(new URL("file", "", Paths.get("src/test/resources/data_that_does_not_exists.txt")
                                .toFile().getAbsolutePath()));
        Set<UUID> jobIds = aipService.store(Sets.newHashSet(aip));
        int wait = 0;
        LOG.info("Waiting for jobs end ...");
        while (!handler.getJobSucceeds().containsAll(jobIds) && !handler.isFailed() && (wait < MAX_WAIT_TEST)) {
            // lets wait for 1 sec before checking again if all our jobs has been done or not
            Thread.sleep(1000);
            wait = wait + 1000;
        }
        Assert.assertTrue("The job succeeded while it should not have", handler.isFailed());
        Optional<AIP> aipFromDB = aipDao.findOneByIpId(aip.getId().toString());
        wait = 0;
        LOG.info("Waiting for AIP {} error ...", aip.getId().toString());
        while (!AIPState.STORAGE_ERROR.equals(aipFromDB.get().getState()) && (wait < MAX_WAIT_TEST)) {
            aipFromDB = aipDao.findOneByIpId(aip.getId().toString());
            Thread.sleep(1000);
            wait = wait + 1000;
        }
        Assert.assertNotEquals(MAX_WAIT_TEST, wait);
        Assert.assertEquals(AIPState.STORAGE_ERROR, aipFromDB.get().getState());
        LOG.info("AIP {} is in ERROR State", aip.getId().toString());
        Set<DataFile> dataFiles = dataFileDao.findAllByStateAndAip(DataFileState.ERROR, aip);
        Assert.assertEquals(1, dataFiles.size());
    }

    @Test
    @Ignore("test ignored for now, time to get the CI running with a real user not root which bypass permissions on directories")
    //FIXME
    public void createFailOnMetadataTest() throws ModuleException, InterruptedException, IOException {
        // to make the process fail just on metadata storage, lets remove permissions from the workspace
        Path workspacePath = Paths.get(workspace, DEFAULT_TENANT);
        Set<PosixFilePermission> oldPermissions = Files.getPosixFilePermissions(workspacePath);
        Files.setPosixFilePermissions(workspacePath, Sets.newHashSet());
        try {
            Set<UUID> jobIds = aipService.store(Sets.newHashSet(aip));
            int wait = 0;
            while (!handler.getJobSucceeds().containsAll(jobIds) && !handler.isFailed() && (wait < MAX_WAIT_TEST)) {
                // lets wait for 1 sec before checking again if all our jobs has been done or not
                Thread.sleep(1000);
                wait = wait + 1000;
            }
            Assert.assertFalse("The job failed while it should not have", handler.isFailed());
            LOG.info("Waiting for AIP {} error ...", aip.getId().toString());
            Optional<AIP> aipFromDB = aipDao.findOneByIpId(aip.getId().toString());
            wait = 0;
            while (!AIPState.STORAGE_ERROR.equals(aipFromDB.get().getState()) && (wait < MAX_WAIT_TEST)) {
                aipFromDB = aipDao.findOneByIpId(aip.getId().toString());
                Thread.sleep(1000);
                wait = wait + 1000;
            }
            Assert.assertEquals(AIPState.STORAGE_ERROR, aipFromDB.get().getState());
            Set<DataFile> dataFiles = dataFileDao.findAllByStateAndAip(DataFileState.STORED, aip);
            Assert.assertEquals(1, dataFiles.size());
        } finally {
            // to avoid issues with following tests, lets set back the permissions
            Files.setPosixFilePermissions(workspacePath, oldPermissions);
        }
    }

    @Test
    @Requirements({ @Requirement("REGARDS_DSL_STO_AIP_030"), @Requirement("REGARDS_DSL_STO_AIP_040") })
    public void testUpdate() throws InterruptedException, ModuleException, URISyntaxException {
        // first lets store the aip
        createSuccessTest();
        // now that it is correctly created, lets say it has been updated and add a tag
        aip = aipDao.findOneByIpId(aip.getId().toString()).get();
        aip.getTags().add("Exemple Tag For Fun");
        aipService.updateAip(aip.getId().toString(), aip);
        Optional<DataFile> oldDataFile = dataFileDao.findByAipAndType(aip, DataType.AIP);
        Assert.assertTrue("The old data file should exists !",
                          Files.exists(Paths.get(oldDataFile.get().getUrl().toURI())));
        // now lets launch the method without scheduling
        aipService.updateAlreadyStoredMetadata();

        // Here the AIP should be in STORING_METADATA state
        AIP newAIP = aipDao.findOneByIpId(aip.getId().toString()).get();
        Assert.assertTrue("AIP should be in storing metadata state",
                          newAIP.getState().equals(AIPState.STORING_METADATA));
        int count = 0;
        while (Files.exists(Paths.get(oldDataFile.get().getUrl().toURI())) && (count < 40)) {
            Thread.sleep(1000);
            count++;
        }
        // After job is done, the old AIP metadata file should be deleted
        Assert.assertFalse("The old data file should not exists anymore!",
                           Files.exists(Paths.get(oldDataFile.get().getUrl().toURI())));
        newAIP = aipDao.findOneByIpId(aip.getId().toString()).get();
        count = 0;
        // After job is done, the AIP should be in STORED state.
        while (!newAIP.getState().equals(AIPState.STORED) && (count < 10)) {
            Thread.sleep(1000);
            newAIP = aipDao.findOneByIpId(aip.getId().toString()).get();
            count++;
        }
        // After job is done, the new AIP metadata file should be present in local datastorage
        DataFile file = dataFileDao.findByAipAndType(newAIP, DataType.AIP).get();
        Assert.assertTrue("The new data file should exist!", Files.exists(Paths.get(file.getUrl().toURI())));
    }

    @Test
    @Ignore
    public void testDeleteAip() throws InterruptedException, ModuleException, URISyntaxException {
        createSuccessTest();
        String aipIpId = aip.getId().toString();
        //lets get all the dataFile before deleting them for further verification
        Set<DataFile> aipFiles = dataFileDao.findAllByAip(aip);
        Set<UUID> jobIds = aipService.deleteAip(aipIpId);
        aip = aipDao.findOneByIpId(aipIpId).get();
        Assert.assertEquals("AIP state should be DELETED now", AIPState.DELETED, aip.getState());
        //now lets wait for the deletion job to be finished
        jobIds.forEach(job -> LOG.info("Waiting for job {} end", job.toString()));
        int wait = 0;
        while (!handler.getJobSucceeds().containsAll(jobIds) && !handler.isFailed() && (wait < MAX_WAIT_TEST)) {
            // lets wait for 1 sec before checking again if all our jobs has been done or not
            Thread.sleep(1000);
            wait = wait + 1000;
        }
        Assert.assertNotEquals("Message from AMQP should have been received by now!", MAX_WAIT_TEST, wait);
        Assert.assertFalse("The job failed while it should not have", handler.isFailed());
        LOG.info("All waiting JOB succeeded");

        Assert.assertFalse("AIP should not be referenced in the database", aipDao.findOneByIpId(aipIpId).isPresent());
        for (DataFile df : aipFiles) {
            if (df.getDataType() == DataType.AIP) {
                Assert.assertFalse("AIP metadata should not be on disk anymore",
                                   Files.exists(Paths.get(df.getUrl().toURI())));
            } else {
                Assert.assertFalse("AIP data should not be on disk anymore",
                                   Files.exists(Paths.get(df.getUrl().toURI())));
            }
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

    private void purgeAMQPqueues() {
        vHost.bind(DEFAULT_TENANT);
        try {
            amqpAdmin.purgeQueue(JobEvent.class, RestoreJobEventHandler.class, true);
        } catch (Exception e) {
            // Nothing to do
        }
        vHost.unbind();
    }

    private void unsubscribeAMQPEvents() {
        try {
            subscriber.unsubscribeFrom(JobEvent.class);
        } catch (Exception e) {
            // Nothing to do
            LOG.error("ERROR DURING UNSUBSCRIBE", e);
        }
        handler.reset();
    }

    @After
    public void cleanUp() throws URISyntaxException, IOException {
        purgeAMQPqueues();
        unsubscribeAMQPEvents();
        jobInfoRepo.deleteAll();
        dataFileDao.deleteAll();
        aipDao.deleteAll();
        pluginRepo.deleteAll();
        if (baseStorageLocation != null) {
            Files.walk(Paths.get(baseStorageLocation.toURI())).sorted(Comparator.reverseOrder()).map(Path::toFile)
                    .forEach(File::delete);
        }
    }

}
