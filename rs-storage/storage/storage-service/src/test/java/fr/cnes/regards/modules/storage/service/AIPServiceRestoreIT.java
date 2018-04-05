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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MimeType;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.gson.Gson;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.RegardsAmqpAdmin;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
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
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.test.report.annotation.Requirements;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.notification.client.INotificationClient;
import fr.cnes.regards.modules.storage.dao.IAIPDao;
import fr.cnes.regards.modules.storage.dao.ICachedFileRepository;
import fr.cnes.regards.modules.storage.dao.IDataFileDao;
import fr.cnes.regards.modules.storage.dao.IPrioritizedDataStorageRepository;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPBuilder;
import fr.cnes.regards.modules.storage.domain.AvailabilityRequest;
import fr.cnes.regards.modules.storage.domain.AvailabilityResponse;
import fr.cnes.regards.modules.storage.domain.database.CachedFile;
import fr.cnes.regards.modules.storage.domain.database.CachedFileState;
import fr.cnes.regards.modules.storage.domain.database.PrioritizedDataStorage;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.event.DataFileEvent;
import fr.cnes.regards.modules.storage.domain.event.DataStorageEvent;
import fr.cnes.regards.modules.storage.domain.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.INearlineDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.IOnlineDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.ISecurityDelegation;
import fr.cnes.regards.modules.storage.plugin.datastorage.local.LocalDataStorage;
import fr.cnes.regards.modules.storage.service.plugins.CatalogSecurityDelegationTestPlugin;
import fr.cnes.regards.modules.storage.service.plugins.NearlineNoRetrieveDataStorage;
import fr.cnes.regards.modules.storage.service.plugins.SimpleNearLineStoragePlugin;

/**
 * Class to test all AIP service restore functions.
 * @author Sébastien Binda
 */
@ContextConfiguration(classes = AIPServiceRestoreIT.Config.class)
@TestPropertySource(locations = "classpath:test.properties")
@ActiveProfiles("testAmqp")
@Ignore("Borred of CI timing, should works in local, works for now in real life")
public class AIPServiceRestoreIT extends AbstractRegardsServiceTransactionalIT {

    private static final Logger LOG = LoggerFactory.getLogger(AIPServiceRestoreIT.class);

    private static final String CATALOG_SECURITY_DELEGATION_LABEL = "AIPServiceRestoreIT";

    private static RestoreJobEventHandler handler = new RestoreJobEventHandler();

    private static TestDataStorageEventHandler dataHandler = new TestDataStorageEventHandler();

    private static Path cacheDir = Paths.get("target/cache");

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

    @Autowired
    private ISubscriber subscriber;

    @SuppressWarnings("unused")
    @Autowired
    private IRabbitVirtualHostAdmin vHost;

    @SuppressWarnings("unused")
    @Autowired
    private RegardsAmqpAdmin amqpAdmin;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IPrioritizedDataStorageService prioritizedDataStorageService;

    @Autowired
    private IPrioritizedDataStorageRepository prioritizedDataStorageRepository;

    @Value("${regards.storage.cache.size.limit.ko.per.tenant}")
    private Long cacheSizeLimitKo;

    @Value("${regards.cache.restore.queued.rate.ms}")
    private Long restoreQueuedRate;

    @Value("${regards.cache.cleanup.rate.ms}")
    private Long cleanCacheRate;

    private PluginConfiguration catalogSecuDelegConf;

    private PrioritizedDataStorage onlineDataStorageConf;

    private PrioritizedDataStorage nearlineDataStorageConf;

    private URL baseStorageLocation;

    private PrioritizedDataStorage nearlineNoRetrieveDataStorageConf;

    private PrioritizedDataStorage onlineNoRetrieveDataStorageConf;

    public void initCacheDir() throws IOException {
        if (cacheDir.toFile().exists()) {
            Files.walk(cacheDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
        Files.createDirectory(cacheDir);
    }

    @Before
    public void init() throws Exception {
        tenantResolver.forceTenant(DEFAULT_TENANT);
        initCacheDir();
        this.cleanUp();

        subscriber.subscribeTo(JobEvent.class, handler);
        subscriber.subscribeTo(DataFileEvent.class, dataHandler);
        initDb();
    }

    /**
     * Init all informations needed by this tests in data base.
     * @throws Exception
     */
    private void initDb() throws Exception {
        baseStorageLocation = new URL("file", "", Paths.get("target/AIPServiceIT/normal").toFile().getAbsolutePath());
        Files.createDirectories(Paths.get(baseStorageLocation.toURI()));

        // second, lets storeAndCreate a plugin configuration for IAllocationStrategy
        pluginService.addPluginPackage(SimpleNearLineStoragePlugin.class.getPackage().getName());
        PluginMetaData catalogSecuDelegMeta = PluginUtils
                .createPluginMetaData(CatalogSecurityDelegationTestPlugin.class,
                                      CatalogSecurityDelegationTestPlugin.class.getPackage().getName(),
                                      ISecurityDelegation.class.getPackage().getName());
        catalogSecuDelegConf = new PluginConfiguration(catalogSecuDelegMeta, CATALOG_SECURITY_DELEGATION_LABEL);
        catalogSecuDelegConf = pluginService.savePluginConfiguration(catalogSecuDelegConf);

        PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(LocalDataStorage.class,
                                                                      IDataStorage.class.getPackage().getName(),
                                                                      IOnlineDataStorage.class.getPackage().getName());
        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(LocalDataStorage.LOCAL_STORAGE_TOTAL_SPACE, 10000000)
                .addParameter(LocalDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                              gson.toJson(baseStorageLocation))
                .getParameters();
        PluginConfiguration onlineDSConf = new PluginConfiguration(dataStoMeta, "dsConfLabel", parameters, 0);
        onlineDSConf.setIsActive(true);
        onlineDataStorageConf = prioritizedDataStorageService.create(onlineDSConf);

        parameters = PluginParametersFactory.build().addParameter(LocalDataStorage.LOCAL_STORAGE_TOTAL_SPACE, 10000000)
                .addParameter(LocalDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                              gson.toJson(baseStorageLocation))
                .getParameters();
        PluginMetaData onlineNoRetrieveDataStoMeta = PluginUtils
                .createPluginMetaData(LocalDataStorage.class, IDataStorage.class.getPackage().getName(),
                                      IOnlineDataStorage.class.getPackage().getName());
        PluginConfiguration onlineNoRetrieveDSConf = new PluginConfiguration(onlineNoRetrieveDataStoMeta,
                "onlineNoRetrieveDsConfLabel", parameters, 1);
        onlineNoRetrieveDSConf.setIsActive(true);
        onlineNoRetrieveDataStorageConf = prioritizedDataStorageService.create(onlineNoRetrieveDSConf);

        parameters = PluginParametersFactory.build().addParameter(LocalDataStorage.LOCAL_STORAGE_TOTAL_SPACE, 10000000)
                .addParameter(LocalDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                              gson.toJson(baseStorageLocation))
                .getParameters();
        PluginMetaData nearlineMeta = PluginUtils
                .createPluginMetaData(SimpleNearLineStoragePlugin.class, IDataStorage.class.getPackage().getName(),
                                      INearlineDataStorage.class.getPackage().getName());
        parameters = PluginParametersFactory.build().getParameters();
        PluginConfiguration nearlineDSConf = new PluginConfiguration(nearlineMeta, "nearlineConfLabel", parameters, 0);
        nearlineDSConf.setIsActive(true);

        nearlineDataStorageConf = prioritizedDataStorageService.create(nearlineDSConf);

        PluginMetaData dataStoNoRetrieveMeta = PluginUtils
                .createPluginMetaData(NearlineNoRetrieveDataStorage.class, IDataStorage.class.getPackage().getName(),
                                      INearlineDataStorage.class.getPackage().getName());
        PluginConfiguration nearlineNoRetrieveDSConf = new PluginConfiguration(dataStoNoRetrieveMeta,
                "dsNoRetrieveConfLabel");
        nearlineNoRetrieveDSConf.setIsActive(true);
        nearlineNoRetrieveDataStorageConf = prioritizedDataStorageService.create(nearlineNoRetrieveDSConf);
    }

    /**
     * Verify that errors are handled when an avaibility request is sent for files that does not exists.<br/>
     * Expected result : The {@link AvailabilityResponse} contains all files in error.
     */
    @Test
    public void loadUnavailableFilesTest() throws ModuleException {
        LOG.info("Start test loadUnavailableFilesTest ...");
        AvailabilityRequest request = new AvailabilityRequest(OffsetDateTime.now().plusDays(10), "1", "2", "3");
        AvailabilityResponse response = aipService.loadFiles(request);
        Assert.assertTrue("No file should be directly available after AIPService::loadFiles. Cause : files to load does not exists !",
                          response.getAlreadyAvailable().isEmpty());
        Assert.assertTrue("All files should be in error after AIPService::loadFiles. Cause : files to load does not exists !",
                          response.getErrors().size() == 3);
        LOG.info("End test loadUnavailableFilesTest ...");
    }

    /**
     * Verify that online files are directly available from an avaibility request.<br/>
     * Expected result : The {@link AvailabilityResponse} contains all files available from online storage.
     * @throws MalformedURLException
     */
    @Test
    public void loadOnlineFilesTest() throws MalformedURLException, ModuleException {
        LOG.info("Start test loadOnlineFilesTest ...");
        fillOnlineDataFileDb(50L);
        AvailabilityRequest request = new AvailabilityRequest(OffsetDateTime.now().plusDays(10), "1", "2", "3");
        AvailabilityResponse response = aipService.loadFiles(request);
        Assert.assertTrue("All files should be directly available after AIPService::loadFiles. Cause : files to load are online.",
                          response.getAlreadyAvailable().size() == 3);
        Assert.assertTrue("No file should be in error after AIPService::loadFiles. Cause : All files exists !.",
                          response.getErrors().isEmpty());
        LOG.info("End test loadOnlineFilesTest ...");
    }

    /**
     * Verify that files stored online and nearline are directly available from an avaibility request thanks to the
     * online storage.<br/>
     * Expected result : The {@link AvailabilityResponse} contains all files available from online storage.
     * @throws MalformedURLException
     */
    @Test
    public void loadOnlineNNearlineFilesTest() throws MalformedURLException, ModuleException {
        LOG.info("Start test loadOnlineNNearlineFilesTest ...");
        fillOnlineNNearlineDataFileDb(50L);
        AvailabilityRequest request = new AvailabilityRequest(OffsetDateTime.now().plusDays(10), "1", "2", "3");
        AvailabilityResponse response = aipService.loadFiles(request);
        Assert.assertTrue("All files should be directly available after AIPService::loadFiles. Cause : files to load are online.",
                          response.getAlreadyAvailable().size() == 3);
        Assert.assertTrue("No file should be in error after AIPService::loadFiles. Cause : All files exists !.",
                          response.getErrors().isEmpty());
        LOG.info("End test loadOnlineNNearlineFilesTest ...");
    }

    /**
     * Verify that nearline files are not directly available from an avaibility request.
     * But, asynchonous jobs are scheduled for restoration. Verify also that when jobs are ended well,
     * events are sent to indicate that the restore files are available.<br/>
     * Expected results :
     * <ul>
     * <li>The {@link AvailabilityResponse} is empty. No error, no file available</li>
     * <li>One restoration job is scheduled and run</li>
     * <li>After job execution ended, one event per available file is sent</li>
     * </ul>
     *
     * @throws MalformedURLException
     * @throws InterruptedException
     */
    @Test
    @Requirements({ @Requirement("REGARDS_DSL_STO_CMD_110") })
    public void loadNearlineFilesTest() throws MalformedURLException, InterruptedException, ModuleException {
        LOG.info("Start test loadNearlineFilesTest ...");
        fillNearlineDataFileDb(50L, "");

        Assert.assertTrue("Initialization error. The test shouldn't start with cachd files in AVAILABLE status.",
                          cachedFileRepository.findAllByState(CachedFileState.AVAILABLE).isEmpty());
        AvailabilityRequest request = new AvailabilityRequest(OffsetDateTime.now().plusDays(10), "10", "20", "30");
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
        // just add a sleep of one sec so event should have been handled
        Thread.sleep(1000);
        Optional<CachedFile> ocf = cachedFileRepository.findOneByChecksum("10");
        Assert.assertTrue("The nearLine file 10 should be present in db as a cachedFile", ocf.isPresent());
        Assert.assertTrue(String.format("The nearLine file 10 should be have status AVAILABLE not %s.",
                                        ocf.get().getState()),
                          ocf.get().getState().equals(CachedFileState.AVAILABLE));
        Assert.assertTrue("The file should be physicly in cache directory",
                          Paths.get(ocf.get().getLocation().getPath()).toFile().exists());

        ocf = cachedFileRepository.findOneByChecksum("20");
        Assert.assertTrue("The nearLine file 20 should be present in db as a cachedFile", ocf.isPresent());
        Assert.assertTrue(String.format("The nearLine file 20 should be have status AVAILABLE not %s.",
                                        ocf.get().getState()),
                          ocf.get().getState().equals(CachedFileState.AVAILABLE));
        Assert.assertTrue("The file should be physicly in cache directory",
                          Paths.get(ocf.get().getLocation().getPath()).toFile().exists());

        ocf = cachedFileRepository.findOneByChecksum("30");
        Assert.assertTrue("The nearLine file 30 should be present in db as a cachedFile", ocf.isPresent());
        Assert.assertTrue(String.format("The nearLine file 30 should be have status AVAILABLE not %s.",
                                        ocf.get().getState()),
                          ocf.get().getState().equals(CachedFileState.AVAILABLE));
        Assert.assertTrue("The file should be physicly in cache directory",
                          Paths.get(ocf.get().getLocation().getPath()).toFile().exists());

        count = 0;
        while (dataHandler.getRestoredChecksum().isEmpty() && (count < 6)) {
            count++;
            Thread.sleep(1000);
        }
        Assert.assertTrue("There should be 3 DataEvent received.", dataHandler.getRestoredChecksum().size() == 3);

        // Check that all requested files are in cache

        LOG.info("End test loadNearlineFilesTest ...");
    }

    /**
     * Verify that nearline files are not directly available from an avaibility request.
     * But, asynchronous jobs are scheduled for restoration.<br/>
     * Verify also that if there isnt enought space in cache to restore all files then restoration jobs are
     * scheduled to retrieve as much as possible of files and remaining files to restore are set
     * in QUEUED status and are waiting for space free.<br/>
     * Verify also that when jobs are ended well, events are sent to indicate that the restored
     * files are available.<br/>
     * Expected results :
     * <ul>
     * <li>The {@link AvailabilityResponse} is empty. No error, no file available</li>
     * <li>One restoration job is scheduled and run</li>
     * <li>After job execution ended, one event per available file is sent</li>
     * </ul>
     *
     * @throws MalformedURLException
     * @throws InterruptedException
     */
    @Test
    public void loadNearlineFilesWithQueuedTest() throws MalformedURLException, InterruptedException, ModuleException {
        LOG.info("Start test loadNearlineFilesWithQueuedTest ...");
        // Force each file to restore to a big size to simulate cache overflow.
        fillNearlineDataFileDb((this.cacheSizeLimitKo * 1024) / 2, "");

        Assert.assertTrue("Initialization error. The test shouldn't start with cachd files in AVAILABLE status.",
                          cachedFileRepository.findAllByState(CachedFileState.AVAILABLE).isEmpty());
        AvailabilityRequest request = new AvailabilityRequest(OffsetDateTime.now().plusDays(10), "10", "20", "30");
        AvailabilityResponse response = aipService.loadFiles(request);
        Assert.assertTrue(String.format("Invalid number of available files %d", response.getAlreadyAvailable().size()),
                          response.getAlreadyAvailable().isEmpty());
        Assert.assertTrue(String.format("Invalid number of error files %d", response.getAlreadyAvailable().size()),
                          response.getErrors().isEmpty());
        // Wait for jobs ends or fails
        int count = 0;
        while (!handler.isFailed() && handler.getJobSucceeds().isEmpty() && (count < 8)) {
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

    /**
     * Verify that nearline files are not directly available from an avaibility request.
     * Verify also that when the cache is full no restoration jobs are sent and all files are set
     * in QUEUED status.
     * Verify also that when jobs are ended well, events are sent to indicate that the restored
     * files are available.<br/>
     * Expected results :
     * <ul>
     * <li>The {@link AvailabilityResponse} is empty. No error, no file available</li>
     * <li>One restoration job is scheduled and run</li>
     * <li>After job execution ended, one event per available file is sent</li>
     * </ul>
     *
     * @throws MalformedURLException
     * @throws InterruptedException
     */
    @Test
    public void loadNearlineFilesWithFullCache() throws MalformedURLException, InterruptedException, ModuleException {
        LOG.info("Start test loadNearlineFilesWithFullCache ...");

        // Data initialization :
        // -> 6 DataFiles in db with checksum : 10, 20, 30, 100, 200, 300
        // -> 3 files already in cache : 100, 200, 300
        // The 3 files in cache have to simulate that the cache is full in order to test that a new load
        AIP aip = fillNearlineDataFileDb(50L, "");
        Assert.assertTrue("Initialization error. The test shouldn't start with cachd files in AVAILABLE status.",
                          cachedFileRepository.findAllByState(CachedFileState.AVAILABLE).isEmpty());
        // Simulate cache size full by adding files with big size.
        Long fileSize = ((this.cacheSizeLimitKo * 1024) / 3);
        fillCache(aip, "test1", "100", fileSize, OffsetDateTime.now().plusDays(10), OffsetDateTime.now(),
                  "target/cache");
        fillCache(aip, "test2", "200", fileSize, OffsetDateTime.now().plusDays(10), OffsetDateTime.now(),
                  "target/cache");
        fillCache(aip, "test3", "300", fileSize, OffsetDateTime.now().plusDays(10), OffsetDateTime.now(),
                  "target/cache");

        // All files to restore should be initialized in QUEUED state waiting for available size into cache
        AvailabilityRequest request = new AvailabilityRequest(OffsetDateTime.now().plusDays(10), "10", "20", "30");
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

        // Wait for clear cron proceed to ensure that the older files in cache are not deleted. Files minimum ttl is
        // 2 hours. Files created in cache are created with last request date fixed to OffsetDatetime.now().
        Thread.sleep(this.cleanCacheRate + 1000);

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

    /**
     * Test that the cache can be clean by removing the expired files.
     * @throws InterruptedException
     * @throws IOException
     */
    @Test
    @Requirements({ @Requirement("REGARDS_DSL_STO_ARC_450") })
    public void cleanCacheDeleteExpiredFilesTest() throws InterruptedException, IOException {
        LOG.info("Start test testCleanCacheDeleteExpiredFiles ...");
        Long fileSize = (this.cacheSizeLimitKo * 1024) / 2;
        AIP aip = fillNearlineDataFileDb(fileSize, "");
        Assert.assertTrue("Initialization error. The test shouldn't start with cachd files in AVAILABLE status.",
                          cachedFileRepository.findAllByState(CachedFileState.AVAILABLE).isEmpty());
        Path file1 = Files.createFile(Paths.get(cacheDir.toString(), "test1"));
        fillCache(aip, file1.getFileName().toString(), "100", fileSize, OffsetDateTime.now().minusDays(1),
                  OffsetDateTime.now(), cacheDir.toFile().getAbsolutePath());
        Path file2 = Files.createFile(Paths.get(cacheDir.toString(), "test2"));
        fillCache(aip, file2.getFileName().toString(), "200", fileSize, OffsetDateTime.now().minusDays(2),
                  OffsetDateTime.now(), cacheDir.toFile().getAbsolutePath());
        Path file3 = Files.createFile(Paths.get(cacheDir.toString(), "test3"));
        fillCache(aip, file3.getFileName().toString(), "300", fileSize, OffsetDateTime.now(), OffsetDateTime.now(),
                  cacheDir.toFile().getAbsolutePath());
        Path file4 = Paths.get(cacheDir.toString(), "test4");
        fillCache(aip, file4.getFileName().toString(), "400", fileSize, OffsetDateTime.now().plusDays(1),
                  OffsetDateTime.now(), cacheDir.toFile().getAbsolutePath());

        Assert.assertTrue("Init error. File does not exists", file1.toFile().exists());
        Assert.assertTrue("Init error. File does not exists", file2.toFile().exists());
        Assert.assertTrue("Init error. File does not exists", file3.toFile().exists());
        Assert.assertFalse("Init error. File exists", file4.toFile().exists());
        Assert.assertTrue("Initialization error. The test should be 4 cached files in AVAILABLE status.",
                          cachedFileRepository.findAllByState(CachedFileState.AVAILABLE).size() == 4);

        // Wait for scheduled clean process run
        Thread.sleep(cleanCacheRate);
        Thread.sleep(2000);
        int size = cachedFileRepository.findAllByState(CachedFileState.AVAILABLE).size();
        Assert.assertTrue(String.format(
                                        "After the cache clean process ran, there should be only one AVAILABLE file remaining not %s.",
                                        size),
                          size == 1);

        Assert.assertFalse("File should be deleted", file1.toFile().exists());
        Assert.assertFalse("File should be deleted", file2.toFile().exists());
        Assert.assertFalse("File should be deleted", file3.toFile().exists());

        LOG.info("End test testCleanCacheDeleteExpiredFiles");
    }

    /**
     * Test that the cache can be clean even if there is no expired files. In this case,
     * the older files are removed.
     * @throws InterruptedException
     * @throws IOException
     */
    @Test
    @Requirements({ @Requirement("REGARDS_DSL_STO_ARC_450") })
    public void cleanCacheDeleteOlderFilesTest() throws InterruptedException, IOException {
        LOG.info("Start test testCleanCacheDeleteOlderFiles ...");
        // Simulate each file size as the cache is full with 4 files and fill it.
        Long fileSize = (this.cacheSizeLimitKo * 1024) / 4;
        AIP aip = fillNearlineDataFileDb(fileSize, "");
        Assert.assertTrue("Initialization error. The test shouldn't start with cachd files in AVAILABLE status.",
                          cachedFileRepository.findAllByState(CachedFileState.AVAILABLE).isEmpty());
        Path file1 = Files.createFile(Paths.get(cacheDir.toString(), "test1"));
        fillCache(aip, file1.getFileName().toString(), "100", fileSize, OffsetDateTime.now().plusDays(1),
                  OffsetDateTime.now().minusDays(2), cacheDir.toFile().getAbsolutePath());
        Path file2 = Files.createFile(Paths.get(cacheDir.toString(), "test2"));
        fillCache(aip, file2.getFileName().toString(), "200", fileSize, OffsetDateTime.now().plusDays(2),
                  OffsetDateTime.now().minusDays(5), cacheDir.toFile().getAbsolutePath());
        Path file3 = Files.createFile(Paths.get(cacheDir.toString(), "test3"));
        fillCache(aip, file3.getFileName().toString(), "300", fileSize, OffsetDateTime.now().plusDays(3),
                  OffsetDateTime.now().minusDays(4), cacheDir.toFile().getAbsolutePath());
        Path file4 = Files.createFile(Paths.get(cacheDir.toString(), "test4"));
        fillCache(aip, file4.getFileName().toString(), "400", fileSize, OffsetDateTime.now().plusDays(4),
                  OffsetDateTime.now().minusDays(3), cacheDir.toFile().getAbsolutePath());

        Assert.assertTrue("Init error. File does not exists", file1.toFile().exists());
        Assert.assertTrue("Init error. File does not exists", file2.toFile().exists());
        Assert.assertTrue("Init error. File does not exists", file3.toFile().exists());
        Assert.assertTrue("Init error. File does not exists", file4.toFile().exists());
        Assert.assertTrue("Initialization error. The test should be 4 cached files in AVAILABLE status.",
                          cachedFileRepository.findAllByState(CachedFileState.AVAILABLE).size() == 4);

        // Wait for scheduled clean process run
        // The cache is full, no files are expired, so the older files
        // should be deleted to reach the lower threshold of cache size.
        // x = file size
        // cache size = 4x
        // upper threshold = 3x
        // lower threshold = 2x.
        // Conclusion : this method should delete the 2 older files.
        // The older files are calcualted with the lastRequestDate of the files.
        Thread.sleep(cleanCacheRate);
        Thread.sleep(2000);
        int size = cachedFileRepository.findAllByState(CachedFileState.AVAILABLE).size();
        Assert.assertTrue(String
                .format("After the cache clean process ran, there should be 2 AVAILABLE files remaining not %s.", size),
                          size == 2);

        Assert.assertTrue("File should not be deleted", file1.toFile().exists());
        Assert.assertFalse("File should be deleted", file2.toFile().exists());
        Assert.assertFalse("File should be deleted", file3.toFile().exists());
        Assert.assertTrue("File should not be deleted", file4.toFile().exists());

        LOG.info("End test testCleanCacheDeleteOlderFiles.");
    }

    /**
     * Check that the process of retrieving files in QUEUED status is well execute periodically to
     * schedule restoration job when there is enough space left.
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    @Requirement("REGARDS_DSL_STO_ARC_440")
    public void loadAlreadyQueuedFilesTest() throws IOException, InterruptedException, ModuleException {
        LOG.info("Start test testStoreQueuedFiles ...");

        // Simulate fill cache with an old expiration date in order to be sure that files will be deleted
        // by the cache deletion process
        Long fileSize = (this.cacheSizeLimitKo * 1024) / 3;
        AIP aip = fillNearlineDataFileDb(fileSize, "oldOnes");
        fillNearlineDataFileDb(100L, "newOnes");
        Assert.assertTrue("Initialization error. The test shouldn't start with cachd files in AVAILABLE status.",
                          cachedFileRepository.findAllByState(CachedFileState.AVAILABLE).isEmpty());
        Path file1 = Files.createFile(Paths.get(cacheDir.toString(), "test1"));
        fillCache(aip, file1.getFileName().toString(), "oldOnes10", fileSize, OffsetDateTime.now().minusDays(10),
                  OffsetDateTime.now().minusDays(20), cacheDir.toFile().getAbsolutePath());
        Path file2 = Files.createFile(Paths.get(cacheDir.toString(), "test2"));
        fillCache(aip, file2.getFileName().toString(), "oldOnes20", fileSize, OffsetDateTime.now().minusDays(10),
                  OffsetDateTime.now().minusDays(20), cacheDir.toFile().getAbsolutePath());
        Path file3 = Files.createFile(Paths.get(cacheDir.toString(), "test3"));
        fillCache(aip, file3.getFileName().toString(), "oldOnes30", fileSize, OffsetDateTime.now().minusDays(10),
                  OffsetDateTime.now().minusDays(20), cacheDir.toFile().getAbsolutePath());
        Assert.assertTrue("Initialization error. The test should be 3 cached files in AVAILABLE status.",
                          cachedFileRepository.findAllByState(CachedFileState.AVAILABLE).size() == 3);
        Assert.assertTrue("Initialization error. The test shouldn't start with cached files in QUEUED status.",
                          cachedFileRepository.findAllByState(CachedFileState.QUEUED).isEmpty());

        // Run a restore process (files should be set in QUEUED mode)
        AvailabilityRequest request = new AvailabilityRequest(OffsetDateTime.now().plusDays(15), "newOnes10",
                "newOnes20");
        aipService.loadFiles(request);
        Set<CachedFile> queuedFiles = cachedFileRepository.findAllByState(CachedFileState.QUEUED);
        Assert.assertEquals(String.format("After loadfiles process there should 2 files in QUEUED mode not %s",
                                          queuedFiles.size()),
                            2, queuedFiles.size());
        queuedFiles.forEach(f -> LOG.info("Queued File exp date={}", f.getExpiration()));

        // Wait from cache clean. The cache clean should be run as the cache is full and a restore request is sent.
        // Then the files requested can be restore
        // Some space should have been set in cache (all available files deleted)
        int availableFiles = cachedFileRepository.findAllByState(CachedFileState.AVAILABLE).size();
        int count = 0;
        while ((availableFiles != 2) && (count < 20)) {
            LOG.info("Waiting new files restoration in cache  ...");
            Thread.sleep(1000);
            availableFiles = cachedFileRepository.findAllByState(CachedFileState.AVAILABLE).size();
            count++;
        }
        Assert.assertEquals("All files should be available in cache", 2, availableFiles);

        cachedFileRepository.findAll().stream()
                .forEach(f -> LOG.info("CACHED FILE : {} - {}", f.getLocation().toString(), f.getState().toString()));
        // Files should be create successfully.
        Assert.assertEquals("The new 2 loaded files should be AVAILABLE", 2,
                            cachedFileRepository.findAllByState(CachedFileState.AVAILABLE).size());
        Assert.assertTrue("There should not be QUEUED files remaining",
                          cachedFileRepository.findAllByState(CachedFileState.QUEUED).isEmpty());

        Optional<CachedFile> ocf = cachedFileRepository.findOneByChecksum("newOnes20");
        Assert.assertTrue("The nearLine file newOnes20 should be present in db as a cachedFile", ocf.isPresent());

        Optional<CachedFile> ocf2 = cachedFileRepository.findOneByChecksum("oldOnes20");
        Assert.assertFalse("The nearLine file oldOnes20 should not be present in db as a cachedFile", ocf2.isPresent());

        LOG.info("End test testStoreQueuedFiles.");
    }

    @Test
    @Requirement("REGARDS_DSL_STO_AIP_440")
    @Purpose("The system keeps only one copy of a file into its cache")
    public void testLoadAlreadyCached() throws IOException, ModuleException {
        Long fileSize = 100L;
        AIP aip = fillNearlineDataFileDb(fileSize, "oldOnes");
        Path file1 = Files.createFile(Paths.get(cacheDir.toString(), "test1"));
        fillCache(aip, file1.getFileName().toString(), "oldOnes10", fileSize, OffsetDateTime.now().plusDays(5),
                  OffsetDateTime.now(), cacheDir.toFile().getAbsolutePath());
        Path file2 = Files.createFile(Paths.get(cacheDir.toString(), "test2"));
        fillCache(aip, file2.getFileName().toString(), "oldOnes20", fileSize, OffsetDateTime.now().plusDays(5),
                  OffsetDateTime.now(), cacheDir.toFile().getAbsolutePath());
        Path file3 = Files.createFile(Paths.get(cacheDir.toString(), "test3"));
        fillCache(aip, file3.getFileName().toString(), "oldOnes30", fileSize, OffsetDateTime.now().plusDays(5),
                  OffsetDateTime.now(), cacheDir.toFile().getAbsolutePath());

        AvailabilityRequest request = new AvailabilityRequest(OffsetDateTime.now().plusDays(15), "oldOnes10",
                "oldOnes20", "oldOnes30");
        AvailabilityResponse response = aipService.loadFiles(request);
        Assert.assertTrue(String
                .format("Files with checksum: oldOnes10, oldOnes20, oldOnes30 should already be available. For now there is only %s available according to the database. From the resonse: %s",
                        Iterables.toString(cachedFileRepository.findAllByState(CachedFileState.AVAILABLE)),
                        Iterables.toString(response.getAlreadyAvailable())),
                          response.getAlreadyAvailable()
                                  .containsAll(Sets.newHashSet("oldOnes10", "oldOnes20", "oldOnes30")));

    }

    /**
     * Test method to simulate file in cache
     * @param aip Associated {@link AIP}
     * @param fileName
     * @param checksum
     * @param fileSize
     * @param expiration
     * @param lastRequestDate
     * @param location
     * @throws MalformedURLException
     */
    private void fillCache(AIP aip, String fileName, String checksum, Long fileSize, OffsetDateTime expiration,
            OffsetDateTime lastRequestDate, String location) throws MalformedURLException {
        // Simulate cache files to force cache limit size reached before restoring new files.
        // First create StorageDataFile
        StorageDataFile df = new StorageDataFile(Sets.newHashSet(new URL("file://test/" + fileName)), checksum, "MD5",
                DataType.RAWDATA, fileSize, MimeType.valueOf("application/text"), aip, fileName, null);
        df.addDataStorageUsed(nearlineDataStorageConf);
        dataFileDao.save(df);
        // Then create cached file associated
        CachedFile f = new CachedFile(df, expiration, CachedFileState.AVAILABLE);
        if (location != null) {
            f.setLocation(new URL("file://" + Paths.get(location, fileName).toString()));
        }
        if (lastRequestDate != null) {
            f.setLastRequestDate(lastRequestDate);
        } else {
            f.setLastRequestDate(OffsetDateTime.now());
        }
        cachedFileRepository.save(f);
    }

    /**
     * Test method to simulate creation of 3 new {@link StorageDataFile} in Db as there where stored with a online
     * storage
     * plugin.
     * @param fileSize
     * @throws MalformedURLException
     */
    private void fillOnlineDataFileDb(Long fileSize) throws MalformedURLException {
        AIP aip = getAIP();
        aipDao.save(aip);
        Set<StorageDataFile> datafiles = Sets.newHashSet();
        URL url = new URL(Paths.get(baseStorageLocation.toString(), "file1.test").toString());
        StorageDataFile df = new StorageDataFile(Sets.newHashSet(url), "1", "MD5", DataType.RAWDATA, fileSize,
                MimeType.valueOf("application/text"), aip, "file1.test", null);
        df.addDataStorageUsed(onlineDataStorageConf);
        df.addDataStorageUsed(onlineNoRetrieveDataStorageConf);
        datafiles.add(df);
        url = new URL(Paths.get(baseStorageLocation.toString(), "file2.test").toString());
        df = new StorageDataFile(Sets.newHashSet(url), "2", "MD5", DataType.RAWDATA, fileSize,
                MimeType.valueOf("application/text"), aip, "file2.test", null);
        df.addDataStorageUsed(onlineDataStorageConf);
        df.addDataStorageUsed(onlineNoRetrieveDataStorageConf);
        datafiles.add(df);
        url = new URL(Paths.get(baseStorageLocation.toString(), "file3.test").toString());
        df = new StorageDataFile(Sets.newHashSet(url), "3", "MD5", DataType.RAWDATA, fileSize,
                MimeType.valueOf("application/text"), aip, "file3.test", null);
        df.addDataStorageUsed(onlineDataStorageConf);
        df.addDataStorageUsed(onlineNoRetrieveDataStorageConf);
        datafiles.add(df);
        dataFileDao.save(datafiles);
    }

    /**
     * Test method to simulate ceration of 3 new {@link StorageDataFile} in Db as there where stored with a online
     * storage
     * plugin.
     * @param fileSize
     * @throws MalformedURLException
     */
    private void fillOnlineNNearlineDataFileDb(Long fileSize) throws MalformedURLException {
        AIP aip = getAIP();
        aipDao.save(aip);
        Set<StorageDataFile> datafiles = Sets.newHashSet();
        URL url = new URL(Paths.get(baseStorageLocation.toString(), "file1.test").toString());
        URL urlNearline = new URL("file://PLOP/Node/file1.test");
        StorageDataFile df = new StorageDataFile(Sets.newHashSet(url, urlNearline), "1", "MD5", DataType.RAWDATA,
                fileSize, MimeType.valueOf("application/text"), aip, "file1.test", null);
        df.addDataStorageUsed(onlineDataStorageConf);
        df.addDataStorageUsed(nearlineDataStorageConf);
        datafiles.add(df);
        url = new URL(Paths.get(baseStorageLocation.toString(), "file2.test").toString());
        urlNearline = new URL("file://PLOP/Node/file2.test");
        df = new StorageDataFile(Sets.newHashSet(url, urlNearline), "2", "MD5", DataType.RAWDATA, fileSize,
                MimeType.valueOf("application/text"), aip, "file2.test", null);
        df.addDataStorageUsed(onlineDataStorageConf);
        df.addDataStorageUsed(nearlineDataStorageConf);
        datafiles.add(df);
        url = new URL(Paths.get(baseStorageLocation.toString(), "file3.test").toString());
        urlNearline = new URL("file://PLOP/Node/file3.test");
        df = new StorageDataFile(Sets.newHashSet(url, urlNearline), "3", "MD5", DataType.RAWDATA, fileSize,
                MimeType.valueOf("application/text"), aip, "file3.test", null);
        df.addDataStorageUsed(onlineDataStorageConf);
        df.addDataStorageUsed(nearlineDataStorageConf);
        datafiles.add(df);
        dataFileDao.save(datafiles);
    }

    /**
     * Test method to simulate ceration of 3 new {@link StorageDataFile} in Db as there where stored with a nearline
     * storage
     * plugin.
     * @param fileSize
     * @throws MalformedURLException
     */
    private AIP fillNearlineDataFileDb(Long fileSize, String checksumPrefix) throws MalformedURLException {
        AIP aip = getAIP();
        aipDao.save(aip);
        Set<StorageDataFile> datafiles = Sets.newHashSet();
        URL url = Paths.get("src/test/resources/income/file10.txt").toUri().toURL();
        StorageDataFile df = new StorageDataFile(Sets.newHashSet(url), checksumPrefix + "10", "MD5", DataType.RAWDATA,
                fileSize, MimeType.valueOf("application/text"), aip, "file10.test", null);
        df.addDataStorageUsed(nearlineDataStorageConf);
        df.addDataStorageUsed(nearlineNoRetrieveDataStorageConf);
        datafiles.add(df);
        url = Paths.get("src/test/resources/income/file20.txt").toUri().toURL();
        df = new StorageDataFile(Sets.newHashSet(url), checksumPrefix + "20", "MD5", DataType.RAWDATA, fileSize,
                MimeType.valueOf("application/text"), aip, "file20.test", null);
        df.addDataStorageUsed(nearlineDataStorageConf);
        df.addDataStorageUsed(nearlineNoRetrieveDataStorageConf);
        datafiles.add(df);
        Paths.get("src/test/resources/income/file30.txt").toUri().toURL();
        df = new StorageDataFile(Sets.newHashSet(url), checksumPrefix + "30", "MD5", DataType.RAWDATA, fileSize,
                MimeType.valueOf("application/text"), aip, "file30.test", null);
        df.addDataStorageUsed(nearlineDataStorageConf);
        df.addDataStorageUsed(nearlineNoRetrieveDataStorageConf);
        datafiles.add(df);
        dataFileDao.save(datafiles);
        return aip;
    }

    /**
     * Create a new AIP.
     * @throws MalformedURLException
     */
    private AIP getAIP() throws MalformedURLException {

        AIPBuilder aipBuilder = new AIPBuilder(
                new UniformResourceName(OAISIdentifier.AIP, EntityType.DATA, DEFAULT_TENANT, UUID.randomUUID(), 1),
                null, EntityType.DATA);

        String path = System.getProperty("user.dir") + "/src/test/resources/data.txt";
        aipBuilder.getContentInformationBuilder().setDataObject(DataType.RAWDATA, new URL("file", "", path), "MD5",
                                                                "de89a907d33a9716d11765582102b2e0");
        aipBuilder.getContentInformationBuilder().setSyntax("text", "description", MediaType.valueOf("text/plain"));
        aipBuilder.addContentInformation();

        aipBuilder.getPDIBuilder().setAccessRightInformation("public");
        aipBuilder.getPDIBuilder().setFacility("CS");
        aipBuilder.getPDIBuilder().addProvenanceInformationEvent(EventType.SUBMISSION.name(), "test event",
                                                                 OffsetDateTime.now());
        AIP aip = aipBuilder.build();
        aip.addEvent(EventType.SUBMISSION.name(), "submission");
        return aip;
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
        subscriber.purgeQueue(JobEvent.class, RestoreJobEventHandler.class);
        subscriber.purgeQueue(DataFileEvent.class, TestDataStorageEventHandler.class);
        subscriber.purgeQueue(DataStorageEvent.class, DataStorageEventHandler.class);
        unsubscribeAMQPEvents();
        jobInfoRepo.deleteAll();
        dataFileDao.deleteAll();
        aipDao.deleteAll();
        prioritizedDataStorageRepository.deleteAll();
        pluginRepo.deleteAll();
        cachedFileRepository.deleteAll();
        if (baseStorageLocation != null) {
            Files.walk(Paths.get(baseStorageLocation.toURI())).sorted(Comparator.reverseOrder()).map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    @Configuration
    static class Config {

        @Bean
        public INotificationClient notificationClient() {
            return Mockito.mock(INotificationClient.class);
        }

        @Bean
        public IResourceService resourceService() {
            return Mockito.mock(IResourceService.class);
        }

    }

}
