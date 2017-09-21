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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Sets;
import com.google.gson.Gson;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.oais.builder.InformationObjectBuilder;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceTransactionalIT;
import fr.cnes.regards.modules.storage.dao.IAIPDao;
import fr.cnes.regards.modules.storage.dao.IDataFileDao;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPBuilder;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.EventType;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.domain.database.DataFileState;
import fr.cnes.regards.modules.storage.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.plugin.IOnlineDataStorage;
import fr.cnes.regards.modules.storage.plugin.local.LocalDataStorage;
import fr.cnes.regards.plugins.utils.PluginUtils;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@ContextConfiguration(classes = { StoreJobIT.Config.class })
@TestPropertySource(locations = "classpath:test.properties")
@ActiveProfiles("testAmqp")
public class AIPServiceIT extends AbstractRegardsServiceTransactionalIT {

    private static final Logger LOG = LoggerFactory.getLogger(AIPServiceIT.class);

    private static final String ALLOCATION_CONF_LABEL = "AIPServiceIT_ALLOCATION";

    private static final String DATA_STORAGE_CONF_LABEL = "AIPServiceIT_DATA_STORAGE";

    private boolean failed;

    @Autowired
    private Gson gson;

    @Autowired
    private IAIPService aipService;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IJobInfoRepository jobRepo;

    @Autowired
    private IPluginConfigurationRepository pluginRepo;

    @Autowired
    private IAIPDao aipDao;

    @Autowired
    private IDataFileDao dataFileDao;

    @Autowired
    private ISubscriber subscriber;

    @Value("${regards.storage.workspace}")
    private String workspace;

    private AIP aip;

    private Set<UUID> succeeded;

    private URL baseStorageLocation;

    @Before
    public void init() throws IOException, ModuleException, URISyntaxException {
        this.cleanUp();
        failed = false;
        succeeded = Sets.newConcurrentHashSet();
        subscriber.subscribeTo(JobEvent.class, new JobEventHandler());
        // first of all, lets get an AIP with accessible dataObjects and real checksums
        aip = getAIP();
        // second, lets create a plugin configuration for IAllocationStrategy
        pluginService.addPluginPackage(IAllocationStrategy.class.getPackage().getName());
        pluginService.addPluginPackage(DefaultAllocationStrategyPlugin.class.getPackage().getName());
        pluginService.addPluginPackage(IDataStorage.class.getPackage().getName());
        pluginService.addPluginPackage(IOnlineDataStorage.class.getPackage().getName());
        pluginService.addPluginPackage(LocalDataStorage.class.getPackage().getName());
        PluginMetaData allocationMeta = PluginUtils
                .createPluginMetaData(DefaultAllocationStrategyPlugin.class,
                                      DefaultAllocationStrategyPlugin.class.getPackage().getName());
        PluginConfiguration allocationConfiguration = new PluginConfiguration(allocationMeta, ALLOCATION_CONF_LABEL);
        allocationConfiguration.setIsActive(true);
        pluginService.savePluginConfiguration(allocationConfiguration);
        // third, lets create a plugin configuration of IDataStorage with the highest priority
        PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(LocalDataStorage.class,
                                                                      IDataStorage.class.getPackage().getName(),
                                                                      IOnlineDataStorage.class.getPackage().getName());
        baseStorageLocation = new URL("file", "", Paths.get("target/AIPServiceIT").toFile().getAbsolutePath());
        Files.createDirectories(Paths.get(baseStorageLocation.toURI()));
        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(LocalDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                              gson.toJson(baseStorageLocation))
                .getParameters();
        PluginConfiguration dataStorageConf = new PluginConfiguration(dataStoMeta, DATA_STORAGE_CONF_LABEL, parameters,
                0);
        dataStorageConf.setIsActive(true);
        pluginService.savePluginConfiguration(dataStorageConf);
    }

    @Test
    public void createSuccessTest() throws ModuleException, InterruptedException {
        Set<UUID> jobIds = aipService.create(Sets.newHashSet(aip));
        jobIds.forEach(job -> LOG.info("Waiting for job {} end", job.toString()));
        int wait = 0;
        while (!succeeded.containsAll(jobIds) && !failed && (wait < 10000)) {
            // lets wait for 1 sec before checking again if all our jobs has been done or not
            Thread.sleep(1000);
            wait = wait + 1000;
        }
        LOG.info("All waiting JOB succeeded");
        Assert.assertFalse("The job failed while it should not have", failed);
        AIP aipFromDB = aipDao.findOneByIpId(aip.getIpId());
        // Wait for AIP set STORED status
        LOG.info("Waiting for AIP {} stored", aip.getIpId());
        wait = 0;
        while (!AIPState.STORED.equals(aipFromDB.getState()) && (wait < 10000)) {
            aipFromDB = aipDao.findOneByIpId(aip.getIpId());
            Thread.sleep(1000);
            wait = wait + 1000;
        }
        Assert.assertEquals(AIPState.STORED, aipFromDB.getState());
        LOG.info("AIP {} stored", aip.getIpId());
        Set<DataFile> dataFiles = dataFileDao.findAllByStateAndAip(DataFileState.STORED, aip);
        Assert.assertEquals(2, dataFiles.size());
    }

    @Test
    public void createFailOnDataTest() throws MalformedURLException, ModuleException, InterruptedException {
        // first lets change the data location to be sure it fails
        aip.getInformationObjects().get(0).getContentInformation().getDataObject().setUrl(new URL("file", "",
                Paths.get("src/test/resources/data_that_does_not_exists.txt").toFile().getAbsolutePath()));
        Set<UUID> jobIds = aipService.create(Sets.newHashSet(aip));
        int wait = 0;
        LOG.info("Waiting for jobs end ...");
        while (!succeeded.containsAll(jobIds) && !failed && (wait < 10000)) {
            // lets wait for 1 sec before checking again if all our jobs has been done or not
            Thread.sleep(1000);
            wait = wait + 1000;
        }
        Assert.assertTrue("The job succeeded while it should not have", failed);
        AIP aipFromDB = aipDao.findOneByIpId(aip.getIpId());
        wait = 0;
        LOG.info("Waiting for AIP {} error ...", aip.getIpId());
        while (!AIPState.STORAGE_ERROR.equals(aipFromDB.getState()) && (wait < 10000)) {
            aipFromDB = aipDao.findOneByIpId(aip.getIpId());
            Thread.sleep(1000);
            wait = wait + 1000;
        }
        Assert.assertEquals(AIPState.STORAGE_ERROR, aipFromDB.getState());
        LOG.info("AIP {} is in ERROR State", aip.getIpId());
        Set<DataFile> dataFiles = dataFileDao.findAllByStateAndAip(DataFileState.ERROR, aip);
        Assert.assertEquals(1, dataFiles.size());
    }

    @Test
    @Ignore("test ignored for now, time to get the CI running with a real user not root which bypass permissions on directories") //FIXME
    public void createFailOnMetadataTest() throws ModuleException, InterruptedException, IOException {
        // to make the process fail just on metadata storage, lets remove permissions from the workspace
        Path workspacePath = Paths.get(workspace, DEFAULT_TENANT);
        Set<PosixFilePermission> oldPermissions = Files.getPosixFilePermissions(workspacePath);
        Files.setPosixFilePermissions(workspacePath, Sets.newHashSet());
        try {
            Set<UUID> jobIds = aipService.create(Sets.newHashSet(aip));
            int wait = 0;
            while (!succeeded.containsAll(jobIds) && !failed && (wait < 10000)) {
                // lets wait for 1 sec before checking again if all our jobs has been done or not
                Thread.sleep(1000);
                wait = wait + 1000;
            }
            Assert.assertFalse("The job failed while it should not have", failed);
            LOG.info("Waiting for AIP {} error ...", aip.getIpId());
            AIP aipFromDB = aipDao.findOneByIpId(aip.getIpId());
            wait = 0;
            while (!AIPState.STORAGE_ERROR.equals(aipFromDB.getState()) && (wait < 10000)) {
                aipFromDB = aipDao.findOneByIpId(aip.getIpId());
                Thread.sleep(1000);
                wait = wait + 1000;
            }
            Assert.assertEquals(AIPState.STORAGE_ERROR, aipFromDB.getState());
            Set<DataFile> dataFiles = dataFileDao.findAllByStateAndAip(DataFileState.STORED, aip);
            Assert.assertEquals(1, dataFiles.size());
        } finally {
            // to avoid issues with following tests, lets set back the permissions
            Files.setPosixFilePermissions(workspacePath, oldPermissions);
        }
    }

    @Test
    public void testUpdate() throws InterruptedException, ModuleException, URISyntaxException {
        // first lets create the aip
        createSuccessTest();
        // now that it is correctly created, lets say it has been updated and add a tag
        DataFile oldDataFile = dataFileDao.findByAipAndType(aip, DataType.AIP);
        aip = aipDao.findOneByIpId(aip.getIpId());
        aip.getTags().add("Exemple Tag For Fun");
        aip.setState(AIPState.UPDATED);
        aipDao.save(aip);
        Assert.assertTrue("The old data file should exists !", Files.exists(Paths.get(oldDataFile.getUrl().toURI())));
        // now lets launch the method without scheduling
        aipService.updateAlreadyStoredMetadata();

        // Here the AIP should be in STORING_METADATA state
        AIP newAIP = aipDao.findOneByIpId(aip.getIpId());
        Assert.assertTrue("AIP should be in storing metadata state",
                          newAIP.getState().equals(AIPState.STORING_METADATA));
        int count = 0;
        while (Files.exists(Paths.get(oldDataFile.getUrl().toURI())) && (count < 40)) {
            Thread.sleep(1000);
            count++;
        }
        Assert.assertFalse("The old data file should not exists anymore!",
                           Files.exists(Paths.get(oldDataFile.getUrl().toURI())));
        newAIP = aipDao.findOneByIpId(aip.getIpId());
        count = 0;
        while (!newAIP.getState().equals(AIPState.STORED) && (count < 10)) {
            Thread.sleep(1000);
            newAIP = aipDao.findOneByIpId(aip.getIpId());
            count++;
        }
        DataFile file = dataFileDao.findByAipAndType(newAIP, DataType.AIP);
        Assert.assertTrue("The new data file should exists!", Files.exists(Paths.get(file.getUrl().toURI())));
    }

    private class JobEventHandler implements IHandler<JobEvent> {

        @Override
        public void handle(TenantWrapper<JobEvent> wrapper) {
            JobEvent event = wrapper.getContent();
            switch (event.getJobEventType()) {
                case ABORTED:
                case FAILED:

                    failed = true;
                    getLogger().error("Job " + event.getJobId() + " failed or was aborted");
                    break;
                case SUCCEEDED:
                    LOG.info("JOB succeeded {}", event.getJobId());
                    succeeded.add(event.getJobId());
                    break;
                default:
                    break;
            }
        }
    }

    private AIP getAIP() throws MalformedURLException {

        AIPBuilder aipBuilder = new AIPBuilder(EntityType.DATA,
                new UniformResourceName(OAISIdentifier.AIP, EntityType.DATA, DEFAULT_TENANT, UUID.randomUUID(), 1)
                        .toString(),
                null);

        // Build IO
        InformationObjectBuilder ioBuilder = new InformationObjectBuilder();

        String path = System.getProperty("user.dir") + "/src/test/resources/data.txt";
        ioBuilder.getContentInformationBuilder().setDataObject(DataType.RAWDATA, new URL("file", "", path));
        ioBuilder.getContentInformationBuilder().setSyntax("text", "description", "text/plain");

        ioBuilder.getPDIBuilder().setAccessRightInformation("publisherDID", "publisherID", "public");
        ioBuilder.getPDIBuilder().setFixityInformation("de89a907d33a9716d11765582102b2e0", "MD5");
        ioBuilder.getPDIBuilder().setProvenanceInformation("CS");
        ioBuilder.getPDIBuilder().addProvenanceInformationEvent(EventType.SUBMISSION.name(), "test event",
                                                                OffsetDateTime.now());

        aipBuilder.addInformationObjects(ioBuilder.build());
        return aipBuilder.build();
    }

    @After
    public void cleanUp() throws URISyntaxException, IOException {
        jobRepo.deleteAll();
        pluginRepo.deleteAll();
        dataFileDao.deleteAll();
        aipDao.deleteAll();
        Path defaultTenantWorkspace = Paths.get(workspace, DEFAULT_TENANT);
        // in other word, remove everything inside defaultTenantWorkspace but the directory defaultTenantWorkspace,
        // service is not created on each test so workspace is not either
        Files.walk(defaultTenantWorkspace).sorted(Comparator.reverseOrder()).map(Path::toFile)
                .filter(f -> !f.equals(defaultTenantWorkspace.toFile())).forEach(File::delete);
        if (baseStorageLocation != null) {
            Files.walk(Paths.get(baseStorageLocation.toURI())).sorted(Comparator.reverseOrder()).map(Path::toFile)
                    .forEach(File::delete);
        }
    }

}
