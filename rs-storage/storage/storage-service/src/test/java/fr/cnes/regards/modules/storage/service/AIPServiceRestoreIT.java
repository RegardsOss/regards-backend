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
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.assertj.core.util.Sets;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MimeType;

import com.google.gson.Gson;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.RegardsAmqpAdmin;
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
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.storage.dao.IAIPDao;
import fr.cnes.regards.modules.storage.dao.ICachedFileRepository;
import fr.cnes.regards.modules.storage.dao.IDataFileDao;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPBuilder;
import fr.cnes.regards.modules.storage.domain.EventType;
import fr.cnes.regards.modules.storage.domain.database.AvailabilityRequest;
import fr.cnes.regards.modules.storage.domain.database.AvailabilityResponse;
import fr.cnes.regards.modules.storage.domain.database.CachedFile;
import fr.cnes.regards.modules.storage.domain.database.CachedFileState;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.domain.event.DataFileEvent;
import fr.cnes.regards.modules.storage.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.plugin.INearlineDataStorage;
import fr.cnes.regards.modules.storage.plugin.IOnlineDataStorage;
import fr.cnes.regards.modules.storage.plugin.SimpleNearLineStoragePlugin;
import fr.cnes.regards.modules.storage.plugin.local.LocalDataStorage;

@ContextConfiguration(classes = { TestConfig.class })
@TestPropertySource(locations = "classpath:test.properties")
@ActiveProfiles("testAmqp")
public class AIPServiceRestoreIT extends AbstractRegardsServiceTransactionalIT {

    private static final Logger LOG = LoggerFactory.getLogger(AIPServiceRestoreIT.class);

    @Autowired
    private IAIPService aipService;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private Gson gson;

    @Autowired
    private IDataFileDao dataFileDao;

    @Autowired
    private IAIPDao aipDao;

    @Autowired
    private IPluginConfigurationRepository pluginRepo;

    @Autowired
    private ICachedFileRepository cachedFileRepository;

    @Autowired
    private IJobInfoRepository jobInfoRepo;

    private PluginConfiguration dataStorageConf;

    private PluginConfiguration nearLineConf;

    private URL baseStorageLocation;

    @Autowired
    private ISubscriber subscriber;

    @Value("${regards.storage.cache.size.limit.ko}")
    private Long cacheSizeLimitKo;

    private static RestoreJobEventHandler handler = new RestoreJobEventHandler();

    private static TestDataStorageEventHandler dataHandler = new TestDataStorageEventHandler();

    @Autowired
    private IRabbitVirtualHostAdmin vHost;

    @Autowired
    private RegardsAmqpAdmin amqpAdmin;

    @Before
    public void init() throws Exception {
        cleanUp();
        subscriber.subscribeTo(JobEvent.class, handler);
        subscriber.subscribeTo(DataFileEvent.class, dataHandler);
        initDb();
    }

    private void initDb() throws Exception {
        baseStorageLocation = new URL("file", "", Paths.get("target/AIPServiceIT").toFile().getAbsolutePath());
        Files.createDirectories(Paths.get(baseStorageLocation.toURI()));

        // second, lets create a plugin configuration for IAllocationStrategy
        pluginService.addPluginPackage(IDataStorage.class.getPackage().getName());
        pluginService.addPluginPackage(IOnlineDataStorage.class.getPackage().getName());
        pluginService.addPluginPackage(INearlineDataStorage.class.getPackage().getName());
        pluginService.addPluginPackage(LocalDataStorage.class.getPackage().getName());
        pluginService.addPluginPackage(SimpleNearLineStoragePlugin.class.getPackage().getName());

        PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(LocalDataStorage.class,
                                                                      IDataStorage.class.getPackage().getName(),
                                                                      IOnlineDataStorage.class.getPackage().getName());
        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(LocalDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                              gson.toJson(baseStorageLocation))
                .getParameters();
        dataStorageConf = new PluginConfiguration(dataStoMeta, "dsConfLabel", parameters, 0);
        dataStorageConf.setIsActive(true);
        pluginService.savePluginConfiguration(dataStorageConf);

        PluginMetaData nearlineMeta = PluginUtils
                .createPluginMetaData(SimpleNearLineStoragePlugin.class, IDataStorage.class.getPackage().getName(),
                                      INearlineDataStorage.class.getPackage().getName());
        parameters = PluginParametersFactory.build().getParameters();
        nearLineConf = new PluginConfiguration(nearlineMeta, "nearlineConfLabel", parameters, 0);
        nearLineConf.setIsActive(true);

        pluginService.savePluginConfiguration(nearLineConf);
    }

    @Test
    public void loadUnavailableFilesTest() {
        LOG.info("Start test loadUnavailableFilesTest ...");
        AvailabilityRequest request = new AvailabilityRequest(OffsetDateTime.now(), "1", "2", "3");
        AvailabilityResponse response = aipService.loadFiles(request);
        Assert.assertTrue("No file should be directly available after AIPService::locafiles. Cause : files to load does not exists !",
                          response.getAlreadyAvailable().isEmpty());
        Assert.assertTrue("All files should be in error after AIPService::locafiles. Cause : files to load does not exists !",
                          response.getErrors().size() == 3);
        LOG.info("End test loadUnavailableFilesTest ...");
    }

    @Test
    public void loadOnlineFilesTest() throws MalformedURLException {
        LOG.info("Start test loadOnlineFilesTest ...");
        fillOnlineDataFileDb(50L);
        AvailabilityRequest request = new AvailabilityRequest(OffsetDateTime.now(), "1", "2", "3");
        AvailabilityResponse response = aipService.loadFiles(request);
        Assert.assertTrue("All files should be directly available after AIPService::locafiles. Cause : files to load are online.",
                          response.getAlreadyAvailable().size() == 3);
        Assert.assertTrue("No file should be in error after AIPService::locafiles. Cause : All files exists !.",
                          response.getErrors().isEmpty());
        LOG.info("End test loadOnlineFilesTest ...");
    }

    @Test
    public void loadNearlineFilesTest() throws MalformedURLException, InterruptedException {
        LOG.info("Start test loadNearlineFilesTest ...");
        fillNearlineDataFileDb(50L);

        Assert.assertTrue("Initialization error. The test shouldn't start with cachd files in AVAILABLE status.",
                          cachedFileRepository.findByState(CachedFileState.AVAILABLE).isEmpty());
        AvailabilityRequest request = new AvailabilityRequest(OffsetDateTime.now(), "10", "20", "30");
        AvailabilityResponse response = aipService.loadFiles(request);
        Assert.assertTrue(String.format("Invalid number of available files %d", response.getAlreadyAvailable().size()),
                          response.getAlreadyAvailable().isEmpty());
        Assert.assertTrue(String.format("Invalid number of error files %d", response.getErrors().size()),
                          response.getErrors().isEmpty());
        // Wait for jobs ends or fails
        int count = 0;
        while (!handler.isFailed() && handler.getJobSucceeds().isEmpty()
                && (dataHandler.getRestoredChecksum().size() < 3) && (count < 6)) {
            count++;
            Thread.sleep(1000);
        }
        Assert.assertTrue("There should be 1 JobEvent succeed received for nearline files to restore.",
                          handler.getJobSucceeds().size() == 1);
        Assert.assertFalse("There shouldn't be a FAIL jobEvent. Cause : All files nearLine are available !",
                           handler.isFailed());

        Optional<CachedFile> ocf = cachedFileRepository.findOneByChecksum("10");
        Assert.assertTrue("The nearLine file 10 should be present in db as a cachedFile", ocf.isPresent());
        Assert.assertTrue(String.format("The nearLine file 10 should be have status AVAILABLE not %s.",
                                        ocf.get().getState()),
                          ocf.get().getState().equals(CachedFileState.AVAILABLE));

        ocf = cachedFileRepository.findOneByChecksum("20");
        Assert.assertTrue("The nearLine file 20 should be present in db as a cachedFile", ocf.isPresent());
        Assert.assertTrue(String.format("The nearLine file 20 should be have status AVAILABLE not %s.",
                                        ocf.get().getState()),
                          ocf.get().getState().equals(CachedFileState.AVAILABLE));

        ocf = cachedFileRepository.findOneByChecksum("30");
        Assert.assertTrue("The nearLine file 30 should be present in db as a cachedFile", ocf.isPresent());
        Assert.assertTrue(String.format("The nearLine file 30 should be have status AVAILABLE not %s.",
                                        ocf.get().getState()),
                          ocf.get().getState().equals(CachedFileState.AVAILABLE));

        count = 0;
        while (dataHandler.getRestoredChecksum().isEmpty() && (count < 6)) {
            count++;
            Thread.sleep(1000);
        }
        Assert.assertTrue("There should be 3 DataEvent received.", dataHandler.getRestoredChecksum().size() == 3);
        LOG.info("End test loadNearlineFilesTest ...");
    }

    @Test
    public void loadNearlineFilesWithQueuedTest() throws MalformedURLException, InterruptedException {
        LOG.info("Start test loadNearlineFilesWithQueuedTest ...");
        // Force each file to restore to a big size to simulate cache overflow.
        fillNearlineDataFileDb((this.cacheSizeLimitKo * 1024) / 2);

        Assert.assertTrue("Initialization error. The test shouldn't start with cachd files in AVAILABLE status.",
                          cachedFileRepository.findByState(CachedFileState.AVAILABLE).isEmpty());
        AvailabilityRequest request = new AvailabilityRequest(OffsetDateTime.now(), "10", "20", "30");
        AvailabilityResponse response = aipService.loadFiles(request);
        Assert.assertTrue(String.format("Invalid number of available files %d", response.getAlreadyAvailable().size()),
                          response.getAlreadyAvailable().isEmpty());
        Assert.assertTrue(String.format("Invalid number of error files %d", response.getAlreadyAvailable().size()),
                          response.getErrors().isEmpty());
        // Wait for jobs ends or fails
        int count = 0;
        while (!handler.isFailed() && handler.getJobSucceeds().isEmpty() && (count < 6)) {
            count++;
            Thread.sleep(1000);
        }
        Assert.assertTrue("There should be 1 JobEvent succeed received for nearline files to restore.",
                          handler.getJobSucceeds().size() == 1);
        Assert.assertFalse("There shouldn't be a FAIL jobEvent. Cause : All files nearLine are available !",
                           handler.isFailed());

        Optional<CachedFile> ocf = cachedFileRepository.findOneByChecksum("10");
        Assert.assertTrue("The nearLine file 10 should be present in db as a cachedFile", ocf.isPresent());

        Optional<CachedFile> ocf2 = cachedFileRepository.findOneByChecksum("20");
        Assert.assertTrue("The nearLine file 20 should be present in db as a cachedFile", ocf2.isPresent());

        Optional<CachedFile> ocf3 = cachedFileRepository.findOneByChecksum("30");
        Assert.assertTrue("The nearLine file 30 should be present in db as a cachedFile", ocf3.isPresent());

        int nbOfQueued = 0;
        int nbOfAvailable = 0;
        if (ocf.get().getState().equals(CachedFileState.QUEUED)) {
            nbOfQueued++;
        }
        if (ocf.get().getState().equals(CachedFileState.AVAILABLE)) {
            nbOfAvailable++;
        }
        if (ocf2.get().getState().equals(CachedFileState.QUEUED)) {
            nbOfQueued++;
        }
        if (ocf2.get().getState().equals(CachedFileState.AVAILABLE)) {
            nbOfAvailable++;
        }
        if (ocf3.get().getState().equals(CachedFileState.QUEUED)) {
            nbOfQueued++;
        }
        if (ocf3.get().getState().equals(CachedFileState.AVAILABLE)) {
            nbOfAvailable++;
        }

        Assert.assertTrue(String.format("There should be 2 files in status QUEUED not %d.", nbOfQueued),
                          nbOfQueued == 2);
        Assert.assertTrue(String.format("There should be 1 file in status AVAILABLE not %d.", nbOfAvailable),
                          nbOfAvailable == 1);

        count = 0;
        while (dataHandler.getRestoredChecksum().isEmpty() && (count < 6)) {
            count++;
            Thread.sleep(1000);
        }
        Assert.assertTrue(String.format("There should be one DataEvent recieved not %s",
                                        dataHandler.getRestoredChecksum().size()),
                          dataHandler.getRestoredChecksum().size() == 1);
        LOG.info("End test loadNearlineFilesWithQueuedTest ...");
    }

    @Test
    public void loadNearlineFilesWithFullCache() throws MalformedURLException, InterruptedException {
        LOG.info("Start test loadNearlineFilesWithFullCache ...");
        AIP aip = fillNearlineDataFileDb(50L);
        Assert.assertTrue("Initialization error. The test shouldn't start with cachd files in AVAILABLE status.",
                          cachedFileRepository.findByState(CachedFileState.AVAILABLE).isEmpty());
        Long fileSize = (this.cacheSizeLimitKo * 1024) / 2;
        // Simulate cache files to force cache limit size reached before restoring new files.
        cachedFileRepository.save(new CachedFile(
                new DataFile(new URL("file://test"), "100", "MD5", DataType.RAWDATA, fileSize,
                        MimeType.valueOf("application/text"), aip, "test"),
                OffsetDateTime.now(), CachedFileState.AVAILABLE));
        cachedFileRepository.save(new CachedFile(
                new DataFile(new URL("file://test2"), "200", "MD5", DataType.RAWDATA, fileSize,
                        MimeType.valueOf("application/text"), aip, "test2"),
                OffsetDateTime.now(), CachedFileState.AVAILABLE));
        cachedFileRepository.save(new CachedFile(
                new DataFile(new URL("file://test3"), "300", "MD5", DataType.RAWDATA, fileSize,
                        MimeType.valueOf("application/text"), aip, "test3"),
                OffsetDateTime.now(), CachedFileState.AVAILABLE));

        // All files to restore should be initialized in QUEUED state waiting for available size into cache
        AvailabilityRequest request = new AvailabilityRequest(OffsetDateTime.now(), "10", "20", "30");
        AvailabilityResponse response = aipService.loadFiles(request);
        Assert.assertTrue(String.format("Invalid number of available files %d", response.getAlreadyAvailable().size()),
                          response.getAlreadyAvailable().isEmpty());
        Assert.assertTrue(String.format("Invalid number of error files %d", response.getAlreadyAvailable().size()),
                          response.getErrors().isEmpty());
        // Wait for jobs ends or fails
        int count = 0;
        while (!handler.isFailed() && handler.getJobSucceeds().isEmpty() && (count < 5)) {
            count++;
            Thread.sleep(1000);
        }
        Assert.assertTrue("There should be 0 JobEvent succeed received for nearline files to restore.",
                          handler.getJobSucceeds().isEmpty());
        Assert.assertFalse("There shouldn't be a FAIL jobEvent. Cause : All files nearLine are available !",
                           handler.isFailed());

        Optional<CachedFile> ocf = cachedFileRepository.findOneByChecksum("10");
        Assert.assertTrue("The nearLine file 10 should be present in db as a cachedFile", ocf.isPresent());

        Optional<CachedFile> ocf2 = cachedFileRepository.findOneByChecksum("20");
        Assert.assertTrue("The nearLine file 20 should be present in db as a cachedFile", ocf2.isPresent());

        Optional<CachedFile> ocf3 = cachedFileRepository.findOneByChecksum("30");
        Assert.assertTrue("The nearLine file 30 should be present in db as a cachedFile", ocf3.isPresent());

        int nbOfQueued = 0;
        int nbOfAvailable = 0;
        if (ocf.get().getState().equals(CachedFileState.QUEUED)) {
            nbOfQueued++;
        }
        if (ocf.get().getState().equals(CachedFileState.AVAILABLE)) {
            nbOfAvailable++;
        }
        if (ocf2.get().getState().equals(CachedFileState.QUEUED)) {
            nbOfQueued++;
        }
        if (ocf2.get().getState().equals(CachedFileState.AVAILABLE)) {
            nbOfAvailable++;
        }
        if (ocf3.get().getState().equals(CachedFileState.QUEUED)) {
            nbOfQueued++;
        }
        if (ocf3.get().getState().equals(CachedFileState.AVAILABLE)) {
            nbOfAvailable++;
        }

        // All files should be QUEUED as there is no space left in cache.
        Assert.assertTrue(String.format("There should be 3 files in status QUEUED not %d.", nbOfQueued),
                          nbOfQueued == 3);
        Assert.assertTrue(String.format("There should be 0 file in status AVAILABLE not %d.", nbOfAvailable),
                          nbOfAvailable == 0);

        count = 0;
        while (dataHandler.getRestoredChecksum().isEmpty() && (count < 5)) {
            count++;
            Thread.sleep(1000);
        }
        Assert.assertTrue(String.format("There should be 0 DataEvent recieved not %s",
                                        dataHandler.getRestoredChecksum().size()),
                          dataHandler.getRestoredChecksum().size() == 0);
        LOG.info("End test loadNearlineFilesWithFullCache ...");
    }

    private void fillOnlineDataFileDb(Long fileSize) throws MalformedURLException {
        AIP aip = getAIP();
        aipDao.save(aip);
        Set<DataFile> datafiles = Sets.newHashSet();
        URL url = new URL(Paths.get(baseStorageLocation.toString(), "file1.test").toString());
        DataFile df = new DataFile(url, "1", "MD5", DataType.RAWDATA, fileSize, MimeType.valueOf("application/text"),
                aip, "file1.test");
        df.setDataStorageUsed(dataStorageConf);
        datafiles.add(df);
        url = new URL(Paths.get(baseStorageLocation.toString(), "file2.test").toString());
        df = new DataFile(url, "2", "MD5", DataType.RAWDATA, fileSize, MimeType.valueOf("application/text"), aip,
                "file2.test");
        df.setDataStorageUsed(dataStorageConf);
        datafiles.add(df);
        url = new URL(Paths.get(baseStorageLocation.toString(), "file3.test").toString());
        df = new DataFile(url, "3", "MD5", DataType.RAWDATA, fileSize, MimeType.valueOf("application/text"), aip,
                "file3.test");
        df.setDataStorageUsed(dataStorageConf);
        datafiles.add(df);
        dataFileDao.save(datafiles);
    }

    private AIP fillNearlineDataFileDb(Long fileSize) throws MalformedURLException {
        AIP aip = getAIP();
        aipDao.save(aip);
        Set<DataFile> datafiles = Sets.newHashSet();
        URL url = new URL("file://PLOP/Node/file10.test");
        DataFile df = new DataFile(url, "10", "MD5", DataType.RAWDATA, fileSize, MimeType.valueOf("application/text"),
                aip, "file10.test");
        df.setDataStorageUsed(nearLineConf);
        datafiles.add(df);
        url = new URL("file://PLOP/Node/file20.test");
        df = new DataFile(url, "20", "MD5", DataType.RAWDATA, fileSize, MimeType.valueOf("application/text"), aip,
                "file20.test");
        df.setDataStorageUsed(nearLineConf);
        datafiles.add(df);
        url = new URL("file://PLOP/Node/file30.test");
        df = new DataFile(url, "30", "MD5", DataType.RAWDATA, fileSize, MimeType.valueOf("application/text"), aip,
                "file30.test");
        df.setDataStorageUsed(nearLineConf);
        datafiles.add(df);
        dataFileDao.save(datafiles);
        return aip;
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
        AIP aip = aipBuilder.build();
        aip.addEvent(EventType.SUBMISSION.name(), "submission");
        return aip;
    }

    private void purgeAMQPqueues() {
        vHost.bind(DEFAULT_TENANT);
        try {
            amqpAdmin.purgeQueue(JobEvent.class, RestoreJobEventHandler.class, true);
            amqpAdmin.purgeQueue(DataFileEvent.class, TestDataStorageEventHandler.class, true);
        } catch (Exception e) {
            // Nothing to do
        }
        vHost.unbind();
    }

    private void unsubscribeAMQPEvents() {
        try {
            subscriber.unsubscribeFrom(JobEvent.class);
            subscriber.unsubscribeFrom(DataFileEvent.class);
        } catch (Exception e) {
            // Nothing to do
        }
        handler.reset();
        dataHandler.reset();
    }

    @After
    public void cleanUp() throws URISyntaxException, IOException {
        purgeAMQPqueues();
        unsubscribeAMQPEvents();
        jobInfoRepo.deleteAll();
        dataFileDao.deleteAll();
        pluginRepo.deleteAll();
        cachedFileRepository.deleteAll();
        if (baseStorageLocation != null) {
            Files.walk(Paths.get(baseStorageLocation.toURI())).sorted(Comparator.reverseOrder()).map(Path::toFile)
                    .forEach(File::delete);
        }
    }

}
