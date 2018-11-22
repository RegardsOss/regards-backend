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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
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
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
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
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
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
import fr.cnes.regards.modules.storage.dao.IStorageDataFileRepository;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPBuilder;
import fr.cnes.regards.modules.storage.domain.AvailabilityRequest;
import fr.cnes.regards.modules.storage.domain.AvailabilityResponse;
import fr.cnes.regards.modules.storage.domain.database.AIPEntity;
import fr.cnes.regards.modules.storage.domain.database.AIPSession;
import fr.cnes.regards.modules.storage.domain.database.CachedFile;
import fr.cnes.regards.modules.storage.domain.database.CachedFileState;
import fr.cnes.regards.modules.storage.domain.database.PrioritizedDataStorage;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.event.DataFileEvent;
import fr.cnes.regards.modules.storage.domain.event.DataStorageEvent;
import fr.cnes.regards.modules.storage.plugin.datastorage.local.LocalDataStorage;
import fr.cnes.regards.modules.storage.service.plugins.CatalogSecurityDelegationTestPlugin;
import fr.cnes.regards.modules.storage.service.plugins.NearlineNoRetrieveDataStorage;
import fr.cnes.regards.modules.storage.service.plugins.SimpleNearLineStoragePlugin;

/**
 * Class to test all AIP service restore functions.
 *
 * In case of randomly test failure because of Thread.sleep, try to implement a {@link AsyncConfigurer}
 * and override {@link SimpleAsyncTaskExecutor#submitListenable(Callable)} to publish spring events to be listened to in tests.
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Sébastien Binda
 */
@ContextConfiguration(classes = AIPServiceRestoreIT.Config.class)
@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate.default_schema=storage_test", "regards.amqp.enabled=true" },
        locations = { "classpath:storage.properties" })
@ActiveProfiles({ "testAmqp", "disableStorageTasks", "noschdule" })
@DirtiesContext(hierarchyMode = HierarchyMode.EXHAUSTIVE, classMode = ClassMode.BEFORE_CLASS)
public class AIPServiceRestoreIT extends AbstractRegardsTransactionalIT {

    private static final Logger LOG = LoggerFactory.getLogger(AIPServiceRestoreIT.class);

    private static final String CATALOG_SECURITY_DELEGATION_LABEL = "AIPServiceRestoreIT";

    private static TestDataStorageEventHandler dataHandler = new TestDataStorageEventHandler();

    private static Path cacheDir = Paths.get("target/cache");

    private static final String SESSION = "Session 1";

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
    private ICachedFileService cachedFileService;

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

    @Autowired
    private IStorageDataFileRepository repository;

    @Value("${regards.storage.cache.size.limit.ko.per.tenant}")
    private Long cacheSizeLimitKo;

    @Value("${regards.storage.cache.minimum.time.to.live.hours}")
    private Long minTtl;

    private PluginConfiguration catalogSecuDelegConf;

    private PrioritizedDataStorage onlineDataStorageConf;

    private PrioritizedDataStorage nearlineDataStorageConf;

    private URL baseStorageLocation;

    private PrioritizedDataStorage nearlineNoRetrieveDataStorageConf;

    private PrioritizedDataStorage onlineNoRetrieveDataStorageConf;

    private final Set<StorageDataFile> nearlineFiles = Sets.newHashSet();

    public void initCacheDir() throws IOException {
        if (cacheDir.toFile().exists()) {
            Files.walk(cacheDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
        Files.createDirectory(cacheDir);
    }

    @Before
    public void init() throws Exception {
        tenantResolver.forceTenant(getDefaultTenant());
        initCacheDir();
        this.cleanUp();
        subscriber.subscribeTo(DataFileEvent.class, dataHandler, true);
        initDb();
    }

    private void waitRestorationJobEnds(int nbRestoredFiles) throws InterruptedException {
        // Wait for jobs ends or fails
        int count = 0;
        // while (!handler.isFailed() && handler.getJobSucceeds().isEmpty()
        // && (dataHandler.getRestoredChecksum().size() < nbRestoredFiles) && (count < 50)) {
        LOG.info("Waiting for {} restored files ...", nbRestoredFiles);
        while ((dataHandler.getRestoredChecksum().size() < nbRestoredFiles) && (count < 10)) {
            count++;
            Thread.sleep(500);
        }
        LOG.info("End of wait for {} restored files. Nb restored files={}", nbRestoredFiles,
                 dataHandler.getRestoredChecksum().size());
    }

    /**
     * Init all informations needed by this tests in data base.
     * @throws Exception
     */
    private void initDb() throws Exception {
        nearlineFiles.clear();
        baseStorageLocation = new URL("file", "",
                Paths.get("target/AIPServiceRestoreIT/normal").toFile().getAbsolutePath());
        Files.createDirectories(Paths.get(baseStorageLocation.toURI()));

        // second, lets storeAndCreate a plugin configuration for IAllocationStrategy
        PluginMetaData catalogSecuDelegMeta = PluginUtils
                .createPluginMetaData(CatalogSecurityDelegationTestPlugin.class);
        catalogSecuDelegConf = new PluginConfiguration(catalogSecuDelegMeta, CATALOG_SECURITY_DELEGATION_LABEL);
        catalogSecuDelegConf = pluginService.savePluginConfiguration(catalogSecuDelegConf);

        PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(LocalDataStorage.class);
        Set<PluginParameter> parameters = PluginParametersFactory.build()
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
        PluginMetaData onlineNoRetrieveDataStoMeta = PluginUtils.createPluginMetaData(LocalDataStorage.class);
        PluginConfiguration onlineNoRetrieveDSConf = new PluginConfiguration(onlineNoRetrieveDataStoMeta,
                "onlineNoRetrieveDsConfLabel", parameters, 1);
        onlineNoRetrieveDSConf.setIsActive(true);
        onlineNoRetrieveDataStorageConf = prioritizedDataStorageService.create(onlineNoRetrieveDSConf);

        parameters = PluginParametersFactory.build().addParameter(LocalDataStorage.LOCAL_STORAGE_TOTAL_SPACE, 10000000)
                .addParameter(LocalDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                              gson.toJson(baseStorageLocation))
                .getParameters();
        PluginMetaData nearlineMeta = PluginUtils.createPluginMetaData(SimpleNearLineStoragePlugin.class);
        // parameters = PluginParametersFactory.build().getParameters();
        LOG.info("parameters {}", parameters.size());
        PluginConfiguration nearlineDSConf = new PluginConfiguration(nearlineMeta, "nearlineConfLabel", parameters, 0);
        nearlineDSConf.setIsActive(true);

        nearlineDataStorageConf = prioritizedDataStorageService.create(nearlineDSConf);

        PluginMetaData dataStoNoRetrieveMeta = PluginUtils.createPluginMetaData(NearlineNoRetrieveDataStorage.class);
        PluginConfiguration nearlineNoRetrieveDSConf = new PluginConfiguration(dataStoNoRetrieveMeta,
                "dsNoRetrieveConfLabel");
        nearlineNoRetrieveDSConf.setIsActive(true);
        nearlineNoRetrieveDataStorageConf = prioritizedDataStorageService.create(nearlineNoRetrieveDSConf);
    }

    /**
     * Verify that errors are handled when an avaibility request is sent for files that does not exists.<br/>
     * Expected result : The {@link AvailabilityResponse} contains all files in error.
     * @throws ModuleException
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
     * @throws ModuleException
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
     * @throws ModuleException
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

    @Test
    public void testRetrieveDistinctSotageDataFiles() throws MalformedURLException, EntityNotFoundException {
        fillNearlineDataFileDb(50L, 3, "dataFile");
        Set<String> checksums = nearlineFiles.stream().map(f -> f.getChecksum()).collect(Collectors.toSet());
        Page<Long> ids = repository.findIdPageByChecksumIn(checksums, new PageRequest(0, 500));
        Set<StorageDataFile> result = repository.findAllDistinctByIdIn(ids.getContent());
        Assert.assertEquals("There should be only 3 storage data files found", 3, result.size());
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
     * @throws ModuleException
     */
    @Test
    @Requirements({ @Requirement("REGARDS_DSL_STO_CMD_110") })
    public void loadNearlineFilesTest() throws MalformedURLException, InterruptedException, ModuleException {
        LOG.info("Start test loadNearlineFilesTest ...");
        fillNearlineDataFileDb(50L, 3, "dataFile");

        Assert.assertTrue("Initialization error. The test shouldn't start with cachd files in AVAILABLE status.",
                          cachedFileRepository.countByState(CachedFileState.AVAILABLE) == 0);
        AvailabilityRequest request = new AvailabilityRequest(OffsetDateTime.now().plusDays(10), "dataFile1",
                "dataFile2", "dataFile3");
        AvailabilityResponse response = aipService.loadFiles(request);
        Assert.assertTrue(String.format("Invalid number of available files %d", response.getAlreadyAvailable().size()),
                          response.getAlreadyAvailable().isEmpty());
        Assert.assertTrue(String.format("Invalid number of error files %d", response.getErrors().size()),
                          response.getErrors().isEmpty());
        // Wait for jobs ends or fails
        waitRestorationJobEnds(3);

        Assert.assertEquals("There should be 3 file restored.", 3, dataHandler.getRestoredChecksum().size());

        Optional<CachedFile> ocf = cachedFileRepository.findOneByChecksum("dataFile1");
        Assert.assertTrue("The nearLine file dataFile1 should be present in db as a cachedFile", ocf.isPresent());
        Assert.assertTrue(String.format("The nearLine file dataFile1 should be have status AVAILABLE not %s.",
                                        ocf.get().getState()),
                          ocf.get().getState().equals(CachedFileState.AVAILABLE));
        Assert.assertTrue("The file should be physicly in cache directory",
                          Paths.get(ocf.get().getLocation().getPath()).toFile().exists());

        ocf = cachedFileRepository.findOneByChecksum("dataFile2");
        Assert.assertTrue("The nearLine file dataFile2 should be present in db as a cachedFile", ocf.isPresent());
        Assert.assertTrue(String.format("The nearLine file dataFile2 should be have status AVAILABLE not %s.",
                                        ocf.get().getState()),
                          ocf.get().getState().equals(CachedFileState.AVAILABLE));
        Assert.assertTrue("The file should be physicly in cache directory",
                          Paths.get(ocf.get().getLocation().getPath()).toFile().exists());

        ocf = cachedFileRepository.findOneByChecksum("dataFile3");
        Assert.assertTrue("The nearLine file dataFile3 should be present in db as a cachedFile", ocf.isPresent());
        Assert.assertTrue(String.format("The nearLine file dataFile3 should be have status AVAILABLE not %s.",
                                        ocf.get().getState()),
                          ocf.get().getState().equals(CachedFileState.AVAILABLE));
        Assert.assertTrue("The file should be physicly in cache directory",
                          Paths.get(ocf.get().getLocation().getPath()).toFile().exists());

        waitRestorationJobEnds(0);

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
     * @throws ModuleException
     */
    @Test
    public void loadNearlineFilesWithQueuedTest() throws MalformedURLException, InterruptedException, ModuleException {
        // Force each file to restore to a big size to simulate cache overflow.
        Long fileSize = ((this.cacheSizeLimitKo * 1024) / 2) - 1;
        fillNearlineDataFileDb(fileSize, 4, "dataFile");

        Assert.assertTrue("Initialization error. The test shouldn't start with cached files in AVAILABLE status.",
                          cachedFileRepository.countByState(CachedFileState.AVAILABLE) == 0);
        Assert.assertTrue("Initialization error. The test shouldn't start with cached files in QUEUED status.",
                          cachedFileRepository.countByState(CachedFileState.QUEUED) == 0);
        AvailabilityRequest request = new AvailabilityRequest(OffsetDateTime.now().plusDays(10), "dataFile1",
                "dataFile2", "dataFile3", "dataFile4");
        AvailabilityResponse response = aipService.loadFiles(request);
        Assert.assertTrue(String.format("Invalid number of available files %d", response.getAlreadyAvailable().size()),
                          response.getAlreadyAvailable().isEmpty());
        Assert.assertTrue(String.format("Invalid number of error files %d", response.getAlreadyAvailable().size()),
                          response.getErrors().isEmpty());

        // There should be 2 file restored, the other ones should be in QUEUED state.
        // There is only space for 2 files in cache
        waitRestorationJobEnds(2);

        Assert.assertEquals("There should be 2 file restored.", 2, dataHandler.getRestoredChecksum().size());

        // All 4 files should be initialized in cache system. 2 as AVAILABLE, 2 as QUEUED
        Optional<CachedFile> ocf = cachedFileRepository.findOneByChecksum("dataFile1");
        Assert.assertTrue("The nearLine file dataFile1 should be present in db as a cachedFile", ocf.isPresent());

        Optional<CachedFile> ocf2 = cachedFileRepository.findOneByChecksum("dataFile2");
        Assert.assertTrue("The nearLine file dataFile2 should be present in db as a cachedFile", ocf2.isPresent());

        Optional<CachedFile> ocf3 = cachedFileRepository.findOneByChecksum("dataFile3");
        Assert.assertTrue("The nearLine file dataFile3 should be present in db as a cachedFile", ocf3.isPresent());

        Optional<CachedFile> ocf4 = cachedFileRepository.findOneByChecksum("dataFile4");
        Assert.assertTrue("The nearLine file dataFile4 should be present in db as a cachedFile", ocf4.isPresent());

        Long nbOfQueued = cachedFileRepository.countByState(CachedFileState.QUEUED);
        Long nbOfAvailable = cachedFileRepository.countByState(CachedFileState.AVAILABLE);

        Assert.assertEquals("Invalid numver of QUEUED cached files.", 2L, nbOfQueued.longValue());
        Assert.assertEquals("Invalid numver of AVAILABLE cached files.", 2L, nbOfAvailable.longValue());
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
     * @throws ModuleException
     */
    @Test
    public void loadNearlineFilesWithFullCache() throws MalformedURLException, InterruptedException, ModuleException {
        LOG.info("Start test loadNearlineFilesWithFullCache ...");

        // Data initialization :
        // -> 6 DataFiles in db with checksum : dataFile1, dataFile2, dataFile3, dataFile4, dataFile5, dataFile6
        // -> 3 files already in cache : dataFile4, dataFile5, dataFile6
        // The 3 files in cache have to simulate that the cache is full
        AIP aip = fillNearlineDataFileDb(50L, 6, "dataFile");
        AIPSession aipSession = aipService.getSession(aip.getSession(), true);

        Assert.assertTrue("Initialization error. The test shouldn't start with cachd files in AVAILABLE status.",
                          cachedFileRepository.countByState(CachedFileState.AVAILABLE) == 0);
        // Simulate cache size full by adding files with big size.
        Long fileSize = ((this.cacheSizeLimitKo * 1024) / 3);
        fillCache(aip, aipSession, "dataFile4", "dataFile4", fileSize, OffsetDateTime.now().plusDays(10),
                  OffsetDateTime.now(), "target/cache");
        fillCache(aip, aipSession, "dataFile5", "dataFile5", fileSize, OffsetDateTime.now().plusDays(10),
                  OffsetDateTime.now(), "target/cache");
        fillCache(aip, aipSession, "dataFile6", "dataFile6", fileSize, OffsetDateTime.now().plusDays(10),
                  OffsetDateTime.now(), "target/cache");

        // All files to restore should be initialized in QUEUED state waiting for available size into cache
        AvailabilityRequest request = new AvailabilityRequest(OffsetDateTime.now().plusDays(10), "dataFile1",
                "dataFile2", "dataFile3");
        AvailabilityResponse response = aipService.loadFiles(request);
        Assert.assertTrue(String.format("Invalid number of available files %d", response.getAlreadyAvailable().size()),
                          response.getAlreadyAvailable().isEmpty());
        Assert.assertTrue(String.format("Invalid number of error files %d", response.getErrors().size()),
                          response.getErrors().isEmpty());
        // Wait for jobs ends or fails
        waitRestorationJobEnds(3);

        Assert.assertEquals("There should be no file restored.", 0, dataHandler.getRestoredChecksum().size());

        // Run clear proceed to ensure that the older files in cache are not deleted. Files minimum ttl is
        // 2 hours. Files created in cache are created with last request date fixed to OffsetDatetime.now().
        cachedFileService.purge();

        Optional<CachedFile> ocf = cachedFileRepository.findOneByChecksum("dataFile1");
        Assert.assertTrue("The nearLine file dataFile1 should be present in db as a cachedFile", ocf.isPresent());

        Optional<CachedFile> ocf2 = cachedFileRepository.findOneByChecksum("dataFile2");
        Assert.assertTrue("The nearLine file dataFile2 should be present in db as a cachedFile", ocf2.isPresent());

        Optional<CachedFile> ocf3 = cachedFileRepository.findOneByChecksum("dataFile3");
        Assert.assertTrue("The nearLine file dataFile3 should be present in db as a cachedFile", ocf3.isPresent());

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

        // Simulate run of restore queued files. No file can be restored, cause the cache is full
        cachedFileService.restoreQueued();
        Thread.sleep(5000);
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
    public void cleanCacheDeleteExpiredFilesTest() throws InterruptedException, IOException, EntityNotFoundException {
        LOG.info("Start test testCleanCacheDeleteExpiredFiles ...");
        Long fileSize = (this.cacheSizeLimitKo * 1024) / 2;
        AIP aip = fillNearlineDataFileDb(fileSize, 3, "dataFile");
        AIPSession aipSession = aipService.getSession(aip.getSession(), true);

        Assert.assertTrue("Initialization error. The test shouldn't start with cachd files in AVAILABLE status.",
                          cachedFileRepository.countByState(CachedFileState.AVAILABLE) == 0);
        Path file1 = Files.createFile(Paths.get(cacheDir.toString(), "dataFile1"));
        fillCache(aip, aipSession, file1.getFileName().toString(), file1.getFileName().toString(), fileSize,
                  OffsetDateTime.now().minusDays(1), OffsetDateTime.now(), cacheDir.toFile().getAbsolutePath());
        Path file2 = Files.createFile(Paths.get(cacheDir.toString(), "dataFile2"));
        fillCache(aip, aipSession, file2.getFileName().toString(), file2.getFileName().toString(), fileSize,
                  OffsetDateTime.now().minusDays(2), OffsetDateTime.now(), cacheDir.toFile().getAbsolutePath());
        Path file3 = Files.createFile(Paths.get(cacheDir.toString(), "dataFile3"));
        fillCache(aip, aipSession, file3.getFileName().toString(), file3.getFileName().toString(), fileSize,
                  OffsetDateTime.now(), OffsetDateTime.now(), cacheDir.toFile().getAbsolutePath());
        Path file4 = Paths.get(cacheDir.toString(), "dataFile4");
        fillCache(aip, aipSession, file4.getFileName().toString(), file4.getFileName().toString(), fileSize,
                  OffsetDateTime.now().plusDays(1), OffsetDateTime.now(), cacheDir.toFile().getAbsolutePath());

        Assert.assertTrue("Init error. File does not exists", file1.toFile().exists());
        Assert.assertTrue("Init error. File does not exists", file2.toFile().exists());
        Assert.assertTrue("Init error. File does not exists", file3.toFile().exists());
        Assert.assertFalse("Init error. File exists", file4.toFile().exists());
        Assert.assertTrue("Initialization error. The test should be 4 cached files in AVAILABLE status.",
                          cachedFileRepository.countByState(CachedFileState.AVAILABLE) == 4);

        // Run clean process
        cachedFileService.purge();
        Long size = cachedFileRepository.countByState(CachedFileState.AVAILABLE);
        Assert.assertTrue(String
                .format("After the cache clean process ran, there should be only one AVAILABLE file remaining not %s.",
                        size), size == 1);

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
    public void cleanCacheDeleteOlderFilesTest() throws InterruptedException, IOException, EntityNotFoundException {
        LOG.info("Start test testCleanCacheDeleteOlderFiles ...");
        // Simulate each file size as the cache is full with 4 files and fill it.
        Long fileSize = (this.cacheSizeLimitKo * 1024) / 4;
        AIP aip = fillNearlineDataFileDb(fileSize, 5, "dataFile");
        AIPSession aipSession = aipService.getSession(aip.getSession(), true);

        Assert.assertTrue("Initialization error. The test shouldn't start with cache files in AVAILABLE status.",
                          cachedFileRepository.countByState(CachedFileState.AVAILABLE) == 0);
        // Simulate files in cache
        // 1. expirationDate: now+1day requestDate : now-2days
        Path file1 = Files.createFile(Paths.get(cacheDir.toString(), "dataFile1"));
        fillCache(aip, aipSession, file1.getFileName().toString(), file1.getFileName().toString(), fileSize,
                  OffsetDateTime.now().plusDays(1), OffsetDateTime.now().minusDays(2),
                  cacheDir.toFile().getAbsolutePath());
        // 2. expirationDate: now+2day requestDate : now-5days
        Path file2 = Files.createFile(Paths.get(cacheDir.toString(), "dataFile2"));
        fillCache(aip, aipSession, file2.getFileName().toString(), file2.getFileName().toString(), fileSize,
                  OffsetDateTime.now().plusDays(2), OffsetDateTime.now().minusDays(5),
                  cacheDir.toFile().getAbsolutePath());
        // 3. expirationDate: now+3day requestDate : now-4days
        Path file3 = Files.createFile(Paths.get(cacheDir.toString(), "dataFile3"));
        fillCache(aip, aipSession, file3.getFileName().toString(), file3.getFileName().toString(), fileSize,
                  OffsetDateTime.now().plusDays(3), OffsetDateTime.now().minusDays(4),
                  cacheDir.toFile().getAbsolutePath());
        // 3. expirationDate: now+4day requestDate : now-3days
        Path file4 = Files.createFile(Paths.get(cacheDir.toString(), "dataFile4"));
        fillCache(aip, aipSession, file4.getFileName().toString(), file4.getFileName().toString(), fileSize,
                  OffsetDateTime.now().plusDays(4), OffsetDateTime.now().minusDays(3),
                  cacheDir.toFile().getAbsolutePath());

        Assert.assertTrue("Init error. File does not exists", file1.toFile().exists());
        Assert.assertTrue("Init error. File does not exists", file2.toFile().exists());
        Assert.assertTrue("Init error. File does not exists", file3.toFile().exists());
        Assert.assertTrue("Init error. File does not exists", file4.toFile().exists());
        Assert.assertTrue("Initialization error. The test should be 4 cached files in AVAILABLE status.",
                          cachedFileRepository.countByState(CachedFileState.AVAILABLE) == 4);

        // Simulate a new file request to force cache purge on next call cause of queued files to restore.
        cachedFileService.restore(nearlineFiles, OffsetDateTime.now().plusDays(10));
        Thread.sleep(1000);
        // There should be only one file in QUEUD cause all other files are already in cach
        Assert.assertEquals("There should be only one QUEUED file to restore in cache", 1L,
                            cachedFileRepository.countByState(CachedFileState.QUEUED).longValue());

        // Run clean process
        // The cache is full, no files are expired, so the older files
        // should be deleted to reach the lower threshold of cache size.
        // x = file size
        // cache size = 4x
        // upper threshold = 3x
        // lower threshold = 2x.
        // Conclusion : this method should delete the 2 older files.
        // The older files are calcualted with the lastRequestDate of the files.
        cachedFileService.purge();

        Long size = cachedFileRepository.countByState(CachedFileState.AVAILABLE);
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
     * @throws ModuleException
     */
    @Test
    @Requirement("REGARDS_DSL_STO_ARC_440")
    public void loadQueuedFilesTest() throws IOException, InterruptedException, ModuleException {
        LOG.info("Start test testStoreQueuedFiles ...");

        int nbFiles = 5;
        // Simulare 5 files for an AIP at STORED state with size calculated to full the restoration cache directory if
        // restored.
        float fullFileSize = ((this.cacheSizeLimitKo * 1024) / nbFiles) - 1;
        Long fileSize = (long) Math.ceil(fullFileSize);
        int nbFilesToDeleteToReachLimit = 3;
        // Simulate other 5 files on the same AIP at STORED state with same size. To simulate a restoration request when
        // the restoration cache directory is full.
        fillNearlineDataFileDb(fileSize, nbFiles, "fileInCache");
        fillNearlineDataFileDb(fileSize, nbFiles, "fileNotInCache");

        Assert.assertTrue("Initialization error. The test shouldn't start with cachd files in AVAILABLE status.",
                          cachedFileRepository.countByState(CachedFileState.AVAILABLE) == 0);

        // Run a restore process to cache all files with two availbility requests.
        AvailabilityRequest request = new AvailabilityRequest(OffsetDateTime.now().plusDays(15), "fileInCache1",
                "fileInCache2");
        aipService.loadFiles(request);
        request = new AvailabilityRequest(OffsetDateTime.now().plusDays(15), "fileInCache3", "fileInCache4",
                "fileInCache5");
        aipService.loadFiles(request);
        waitRestorationJobEnds(nbFiles);

        Assert.assertEquals("There should be 5 cached files in AVAILABLE status.", nbFiles,
                            cachedFileRepository.countByState(CachedFileState.AVAILABLE).longValue());
        Assert.assertTrue("There should not be cached files in QUEUED status.",
                          cachedFileRepository.countByState(CachedFileState.QUEUED) == 0);

        // Run a new restore process with other files to simulare a request when the cahced directory is full.
        // All files request should stay in QUEUD state.
        request = new AvailabilityRequest(OffsetDateTime.now().plusDays(15), "fileNotInCache1", "fileNotInCache2");
        aipService.loadFiles(request);
        request = new AvailabilityRequest(OffsetDateTime.now().plusDays(15), "fileNotInCache3", "fileNotInCache4",
                "fileNotInCache5");
        aipService.loadFiles(request);
        Thread.sleep(1000);
        Page<CachedFile> queuedFiles = cachedFileRepository.findAllByState(CachedFileState.QUEUED,
                                                                           new PageRequest(0, 100));
        Assert.assertEquals(String.format("After loadfiles process there should 5 files in QUEUED mode not %s",
                                          queuedFiles.getNumberOfElements()),
                            nbFiles, queuedFiles.getNumberOfElements());
        queuedFiles.forEach(f -> LOG.info("Queued File exp date={}", f.getExpiration()));

        // Simulate run of cache purge with no expired files and only recent files (minimum time to live not reached).
        cachedFileService.purge();
        Assert.assertEquals("There should be 5 cached files in AVAILABLE status.", nbFiles,
                            cachedFileRepository.countByState(CachedFileState.AVAILABLE).longValue());
        Assert.assertEquals("All files requested should always be in queued mode, as the cache can not be cleared and is full",
                            nbFiles, cachedFileRepository.countByState(CachedFileState.QUEUED).longValue());

        // Simulate time pass by changing date of files in cache
        cachedFileRepository.findAllByState(CachedFileState.AVAILABLE, new PageRequest(0, 100)).getContent().stream()
                .forEach(fileInCache -> {
                    fileInCache.setLastRequestDate(OffsetDateTime.now().minusHours(this.minTtl + 1));
                    cachedFileRepository.save(fileInCache);
                });

        // Simulate run of cache purge with no expired files and only recent files (minimum time to live not reached).
        cachedFileService.purge();
        Assert.assertEquals("There should deletion of cached files in AVAILABLE status as the purge as cleared the older files to reach the limit avaialble size.",
                            nbFiles - nbFilesToDeleteToReachLimit,
                            cachedFileRepository.countByState(CachedFileState.AVAILABLE).longValue());
        Assert.assertEquals("All files requested should always be in queued mode, as the cache can not be cleared and is full",
                            nbFiles, cachedFileRepository.countByState(CachedFileState.QUEUED).longValue());

        dataHandler.reset();
        // Simulate handle of queued files
        cachedFileService.restoreQueued();
        // Wait for restoration jobs ends
        waitRestorationJobEnds(nbFiles);

        Assert.assertEquals("All files should be available in cache", nbFiles,
                            cachedFileRepository.countByState(CachedFileState.AVAILABLE).longValue());

        cachedFileRepository.findAll().stream()
                .forEach(f -> LOG.info("CACHED FILE : {} - {}", f.getLocation(), f.getState()));
        // Files should be create successfully.
        Assert.assertEquals("There should be 5 AVAILABLE files", nbFiles,
                            cachedFileRepository.countByState(CachedFileState.AVAILABLE).longValue());
        Assert.assertEquals("There should be 2 remaining files in QUEUED", 2,
                            cachedFileRepository.countByState(CachedFileState.QUEUED).longValue());

        Optional<CachedFile> ocf = cachedFileRepository.findOneByChecksum("fileNotInCache1");
        Assert.assertTrue("The nearLine file fileNotInCache1 should be present in db as an AVAILABLE cachedFile",
                          ocf.isPresent());

        Optional<CachedFile> ocf2 = cachedFileRepository.findOneByChecksum("fileNotInCache2");
        Assert.assertTrue("The nearLine file fileNotInCache2 should be present in db as a AVAILABLE cachedFile",
                          ocf2.isPresent());

        Optional<CachedFile> ocf3 = cachedFileRepository.findOneByChecksum("fileNotInCache3");
        Assert.assertTrue("The nearLine file fileNotInCache3 should be present in db as a QUEUED cachedFile",
                          ocf3.isPresent());

        Optional<CachedFile> ocf4 = cachedFileRepository.findOneByChecksum("fileNotInCache4");
        Assert.assertTrue("The nearLine file fileNotInCache4 should be present in db as a QUEUED cachedFile",
                          ocf4.isPresent());

        Optional<CachedFile> ocf5 = cachedFileRepository.findOneByChecksum("fileNotInCache5");
        Assert.assertTrue("The nearLine file fileNotInCache5 should be present in db as a QUEUED cachedFile",
                          ocf5.isPresent());

        LOG.info("End test testStoreQueuedFiles.");
    }

    @Test
    @Requirement("REGARDS_DSL_STO_AIP_440")
    @Purpose("The system keeps only one copy of a file into its cache")
    public void testLoadAlreadyCached() throws IOException, ModuleException, InterruptedException {
        Long fileSize = 100L;
        // Simulate 3 dataFiles already restored in cached
        AIP aip = fillNearlineDataFileDb(fileSize, 3, "dataFile");
        AIPSession aipSession = new AIPSession();
        aipSession.setId(aip.getSession());
        Path file1 = Files.createFile(Paths.get(cacheDir.toString(), "dataFile1"));
        fillCache(aip, aipSession, file1.getFileName().toString(), file1.getFileName().toString(), fileSize,
                  OffsetDateTime.now().plusDays(5), OffsetDateTime.now(), cacheDir.toFile().getAbsolutePath());
        Path file2 = Files.createFile(Paths.get(cacheDir.toString(), "dataFile2"));
        fillCache(aip, aipSession, file2.getFileName().toString(), file2.getFileName().toString(), fileSize,
                  OffsetDateTime.now().plusDays(5), OffsetDateTime.now(), cacheDir.toFile().getAbsolutePath());
        Path file3 = Files.createFile(Paths.get(cacheDir.toString(), "dataFile3"));
        fillCache(aip, aipSession, file3.getFileName().toString(), file3.getFileName().toString(), fileSize,
                  OffsetDateTime.now().plusDays(5), OffsetDateTime.now(), cacheDir.toFile().getAbsolutePath());

        // Now request to restore the same 3 files.
        AvailabilityRequest request = new AvailabilityRequest(OffsetDateTime.now().plusDays(15), "dataFile1",
                "dataFile2", "dataFile3");
        AvailabilityResponse response = aipService.loadFiles(request);
        waitRestorationJobEnds(3);
        // Expected result : No restoration jjob success cause the files are alrady in cache.
        Assert.assertTrue("No queued files should be in cache.",
                          cachedFileRepository.countByState(CachedFileState.QUEUED) == 0);
        Assert.assertTrue(String
                .format("Files with checksum: dataFile1, dataFile2, dataFile3 should already be available. For now there is only %s available according to the database. From the resonse: %s",
                        Iterables.toString(cachedFileRepository.findAllByState(CachedFileState.AVAILABLE,
                                                                               new PageRequest(0, 100))),
                        Iterables.toString(response.getAlreadyAvailable())),
                          response.getAlreadyAvailable()
                                  .containsAll(Sets.newHashSet("dataFile1", "dataFile2", "dataFile3")));

    }

    /**
     * Test method to simulate file in cache
     * @param aip Associated {@link AIP}
     * @param aipSession
     * @param fileName
     * @param checksum
     * @param fileSize
     * @param expiration
     * @param lastRequestDate
     * @param location
     * @throws MalformedURLException
     */
    private void fillCache(AIP aip, AIPSession aipSession, String fileName, String checksum, Long fileSize,
            OffsetDateTime expiration, OffsetDateTime lastRequestDate, String location) throws MalformedURLException {
        // Simulate cache files to force cache limit size reached before restoring new files.
        // First create StorageDataFile
        StorageDataFile df = new StorageDataFile(Sets.newHashSet(new URL("file://test/" + fileName)), checksum, "MD5",
                DataType.RAWDATA, fileSize, MimeType.valueOf("application/text"), new AIPEntity(aip, aipSession),
                aipSession, fileName, null);
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
    private void fillOnlineDataFileDb(Long fileSize) throws MalformedURLException, EntityNotFoundException {
        AIP aip = getAIP();
        AIPSession aipSession = aipService.getSession(aip.getSession(), true);
        aipDao.save(aip, aipSession);
        Set<StorageDataFile> datafiles = Sets.newHashSet();
        URL url = new URL(Paths.get(baseStorageLocation.toString(), "file1.test").toString());
        StorageDataFile df = new StorageDataFile(Sets.newHashSet(url), "1", "MD5", DataType.RAWDATA, fileSize,
                MimeType.valueOf("application/text"), new AIPEntity(aip, aipSession), aipSession, "file1.test", null);
        df.addDataStorageUsed(onlineDataStorageConf);
        df.addDataStorageUsed(onlineNoRetrieveDataStorageConf);
        datafiles.add(df);
        url = new URL(Paths.get(baseStorageLocation.toString(), "file2.test").toString());
        df = new StorageDataFile(Sets.newHashSet(url), "2", "MD5", DataType.RAWDATA, fileSize,
                MimeType.valueOf("application/text"), new AIPEntity(aip, aipSession), aipSession, "file2.test", null);
        df.addDataStorageUsed(onlineDataStorageConf);
        df.addDataStorageUsed(onlineNoRetrieveDataStorageConf);
        datafiles.add(df);
        url = new URL(Paths.get(baseStorageLocation.toString(), "file3.test").toString());
        df = new StorageDataFile(Sets.newHashSet(url), "3", "MD5", DataType.RAWDATA, fileSize,
                MimeType.valueOf("application/text"), new AIPEntity(aip, aipSession), aipSession, "file3.test", null);
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
    private void fillOnlineNNearlineDataFileDb(Long fileSize) throws MalformedURLException, EntityNotFoundException {
        AIP aip = getAIP();
        AIPSession aipSession = aipService.getSession(aip.getSession(), true);
        aipDao.save(aip, aipSession);
        Set<StorageDataFile> datafiles = Sets.newHashSet();
        URL url = new URL(Paths.get(baseStorageLocation.toString(), "file1.test").toString());
        URL urlNearline = new URL("file://PLOP/Node/file1.test");
        StorageDataFile df = new StorageDataFile(Sets.newHashSet(url, urlNearline), "1", "MD5", DataType.RAWDATA,
                fileSize, MimeType.valueOf("application/text"), new AIPEntity(aip, aipSession), aipSession,
                "file1.test", null);
        df.addDataStorageUsed(onlineDataStorageConf);
        df.addDataStorageUsed(nearlineDataStorageConf);
        datafiles.add(df);
        url = new URL(Paths.get(baseStorageLocation.toString(), "file2.test").toString());
        urlNearline = new URL("file://PLOP/Node/file2.test");
        df = new StorageDataFile(Sets.newHashSet(url, urlNearline), "2", "MD5", DataType.RAWDATA, fileSize,
                MimeType.valueOf("application/text"), new AIPEntity(aip, aipSession), aipSession, "file2.test", null);
        df.addDataStorageUsed(onlineDataStorageConf);
        df.addDataStorageUsed(nearlineDataStorageConf);
        datafiles.add(df);
        url = new URL(Paths.get(baseStorageLocation.toString(), "file3.test").toString());
        urlNearline = new URL("file://PLOP/Node/file3.test");
        df = new StorageDataFile(Sets.newHashSet(url, urlNearline), "3", "MD5", DataType.RAWDATA, fileSize,
                MimeType.valueOf("application/text"), new AIPEntity(aip, aipSession), aipSession, "file3.test", null);
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
    private AIP fillNearlineDataFileDb(Long fileSize, int nbFilesToFill, String checksumPrefix)
            throws MalformedURLException, EntityNotFoundException {
        AIP aip = getAIP();
        AIPSession aipSession = aipService.getSession(aip.getSession(), true);
        aipDao.save(aip, aipSession);
        Set<StorageDataFile> datafiles = Sets.newHashSet();
        for (int i = 0; i < nbFilesToFill; i++) {
            String fileName = String.format("file%d.txt", i + 1);
            URL url = Paths.get("src/test/resources/income/" + fileName).toUri().toURL();
            StorageDataFile df = new StorageDataFile(Sets.newHashSet(url), String.format("%s%d", checksumPrefix, i + 1),
                    "MD5", DataType.RAWDATA, fileSize, MimeType.valueOf("application/text"),
                    new AIPEntity(aip, aipSession), aipSession, fileName, null);
            df.addDataStorageUsed(nearlineDataStorageConf);
            df.addDataStorageUsed(nearlineNoRetrieveDataStorageConf);
            datafiles.add(df);
        }
        nearlineFiles.addAll(dataFileDao.save(datafiles));
        return aip;
    }

    /**
     * Create a new AIP.
     * @throws MalformedURLException
     */
    private AIP getAIP() throws MalformedURLException {

        UniformResourceName sipId = new UniformResourceName(OAISIdentifier.SIP, EntityType.DATA, getDefaultTenant(),
                UUID.randomUUID(), 1);
        UniformResourceName aipId = new UniformResourceName(OAISIdentifier.AIP, EntityType.DATA, getDefaultTenant(),
                sipId.getEntityId(), 1);
        AIPBuilder aipBuilder = new AIPBuilder(aipId, Optional.of(sipId), "providerId", EntityType.DATA, SESSION);

        Path path = Paths.get("src", "test", "resources", "data.txt");
        aipBuilder.getContentInformationBuilder().setDataObject(DataType.RAWDATA, path, "MD5",
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
        dataHandler.reset();
    }

    @After
    public void cleanUp() throws URISyntaxException, IOException {
        pluginService.cleanPluginCache();
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
