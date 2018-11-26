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
import java.nio.file.attribute.PosixFilePermission;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.compress.utils.Lists;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.MimeType;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.modules.workspace.service.IWorkspaceService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.ContentInformation;
import fr.cnes.regards.framework.oais.Event;
import fr.cnes.regards.framework.oais.EventType;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.test.report.annotation.Requirements;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.notification.client.INotificationClient;
import fr.cnes.regards.modules.storage.dao.IAIPDao;
import fr.cnes.regards.modules.storage.dao.IAIPSessionRepository;
import fr.cnes.regards.modules.storage.dao.IAIPUpdateRequestRepository;
import fr.cnes.regards.modules.storage.dao.IDataFileDao;
import fr.cnes.regards.modules.storage.dao.IPrioritizedDataStorageRepository;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPBuilder;
import fr.cnes.regards.modules.storage.domain.AIPCollection;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.database.AIPSession;
import fr.cnes.regards.modules.storage.domain.database.DataFileState;
import fr.cnes.regards.modules.storage.domain.database.PrioritizedDataStorage;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.event.AIPEvent;
import fr.cnes.regards.modules.storage.domain.event.DataStorageEvent;
import fr.cnes.regards.modules.storage.plugin.allocation.strategy.DefaultMultipleAllocationStrategy;
import fr.cnes.regards.modules.storage.plugin.datastorage.local.LocalDataStorage;
import fr.cnes.regards.modules.storage.service.plugins.CatalogSecurityDelegationTestPlugin;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { TestConfig.class, AIPServiceIT.Config.class })
@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate.default_schema=storage_test", "regards.amqp.enabled=true" },
        locations = { "classpath:storage.properties" })
@ActiveProfiles({ "testAmqp", "disableStorageTasks", "noschdule" })
@DirtiesContext(hierarchyMode = HierarchyMode.EXHAUSTIVE, classMode = ClassMode.BEFORE_CLASS)
@EnableAsync
public class AIPServiceIT extends AbstractRegardsIT {

    private static final Logger LOG = LoggerFactory.getLogger(AIPServiceIT.class);

    private static final String ALLOCATION_CONF_LABEL = "AIPServiceIT_ALLOCATION";

    private static final String DATA_STORAGE_1_CONF_LABEL = "AIPServiceIT_DATA_STORAGE_LOCAL1";

    private static final String DATA_STORAGE_2_CONF_LABEL = "AIPServiceIT_DATA_STORAGE_LOCAL2";

    private static final int WAITING_TIME_MS = 1000;

    private static final String SESSION = "Session 1";

    private static final String CHECKSUM = "de89a907d33a9716d11765582102b2e0";

    private final MockEventHandler mockEventHandler = new MockEventHandler();

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

    @Autowired
    private IPrioritizedDataStorageService prioritizedDataStorageService;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IWorkspaceService workspaceService;

    @Autowired
    private IPrioritizedDataStorageRepository prioritizedDataStorageRepository;

    private PluginConfiguration catalogSecuDelegConf;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IAIPUpdateRequestRepository aipUpdateRepo;

    @Autowired
    private IAIPSessionRepository sessionRepo;

    private AIP aip;

    private URL baseStorage1Location;

    private URL baseStorage2Location;

    private PluginConfiguration dsConfWithDeleteDisabled;

    @Before
    public void init() throws IOException, ModuleException, URISyntaxException, InterruptedException {
        tenantResolver.forceTenant(getDefaultTenant());
        cleanUp();
        mockEventHandler.clear();
        subscriber.subscribeTo(AIPEvent.class, mockEventHandler, true);
        initDb();
        if (baseStorage1Location != null) {
            LOG.info("Deleting dir {}", baseStorage1Location.toString());
            Files.walk(Paths.get(baseStorage1Location.toURI())).sorted(Comparator.reverseOrder()).map(Path::toFile)
                    .forEach(File::delete);
        }
        if (baseStorage2Location != null) {
            LOG.info("Deleting dir {}", baseStorage2Location.toString());
            Files.walk(Paths.get(baseStorage2Location.toURI())).sorted(Comparator.reverseOrder()).map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    private Set<AIPEvent> waitForEventsReceived(AIPState state, int nbExpectedEvents) throws InterruptedException {
        Set<AIPEvent> events = mockEventHandler.getReceivedEvents().stream().filter(e -> e.getAipState().equals(state))
                .collect(Collectors.toSet());
        int waitCount = 0;
        LOG.info("Waiting for {} events {}. Current={}", nbExpectedEvents, state.getName(), events.size());
        while ((events.size() < nbExpectedEvents) && (waitCount < 10)) {
            Thread.sleep(WAITING_TIME_MS);
            events = mockEventHandler.getReceivedEvents().stream().filter(e -> e.getAipState().equals(state))
                    .collect(Collectors.toSet());
            waitCount++;
        }
        mockEventHandler.log(gson);
        LOG.info("After waiting {}/{} events {}", events.size(), nbExpectedEvents, state.getName());
        return events;
    }

    private void initDb() throws ModuleException, IOException, URISyntaxException {
        clearDb();

        // first of all, lets get an AIP with accessible dataObjects and real checksums
        aip = getAIP();

        // second, lets storeAndCreate a plugin configuration of IDataStorage with the highest priority
        PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(LocalDataStorage.class);
        baseStorage1Location = new URL("file", "", Paths.get("target/AIPServiceIT/Local1").toFile().getAbsolutePath());
        Files.createDirectories(Paths.get(baseStorage1Location.toURI()));
        Set<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(LocalDataStorage.LOCAL_STORAGE_TOTAL_SPACE, 9000000000000L)
                .addParameter(LocalDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME, baseStorage1Location.toString())
                .addParameter(LocalDataStorage.LOCAL_STORAGE_DELETE_OPTION, true).getParameters();
        PluginConfiguration dataStorageConf = new PluginConfiguration(dataStoMeta,
                                                                      DATA_STORAGE_1_CONF_LABEL,
                                                                      parameters,
                                                                      0);
        dataStorageConf.setIsActive(true);
        PrioritizedDataStorage pds = prioritizedDataStorageService.create(dataStorageConf);

        Set<Long> dataStorageIds = Sets.newHashSet(pds.getId());

        // third, lets create a second local storage
        baseStorage2Location = new URL("file", "", Paths.get("target/AIPServiceIT/Local2").toFile().getAbsolutePath());
        Files.createDirectories(Paths.get(baseStorage2Location.toURI()));
        parameters = PluginParametersFactory.build()
                .addParameter(LocalDataStorage.LOCAL_STORAGE_TOTAL_SPACE, 9000000000000L)
                .addParameter(LocalDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME, baseStorage2Location.toString())
                .addParameter(LocalDataStorage.LOCAL_STORAGE_DELETE_OPTION, false).getParameters();
        dsConfWithDeleteDisabled = new PluginConfiguration(dataStoMeta, DATA_STORAGE_2_CONF_LABEL, parameters, 0);
        dsConfWithDeleteDisabled.setIsActive(true);
        pds = prioritizedDataStorageService.create(dsConfWithDeleteDisabled);
        dataStorageIds.add(pds.getId());

        // forth, lets create a plugin configuration for IAllocationStrategy
        PluginMetaData allocationMeta = PluginUtils.createPluginMetaData(DefaultMultipleAllocationStrategy.class);
        Set<PluginParameter> allocationParameter = PluginParametersFactory.build()
                .addParameter(DefaultMultipleAllocationStrategy.DATA_STORAGE_IDS_PARAMETER_NAME, dataStorageIds)
                .getParameters();
        PluginConfiguration allocationConfiguration = new PluginConfiguration(allocationMeta,
                                                                              ALLOCATION_CONF_LABEL,
                                                                              allocationParameter,
                                                                              0);
        allocationConfiguration.setIsActive(true);
        pluginService.savePluginConfiguration(allocationConfiguration);

        PluginMetaData catalogSecuDelegMeta = PluginUtils
                .createPluginMetaData(CatalogSecurityDelegationTestPlugin.class);
        catalogSecuDelegConf = new PluginConfiguration(catalogSecuDelegMeta, "AIPServiceIT");
        catalogSecuDelegConf = pluginService.savePluginConfiguration(catalogSecuDelegConf);
    }

    private void waitForJobsFinished() throws InterruptedException {
        LOG.info("Waiting jobs succeed ....");
        List<JobInfo> jobs = Lists.newArrayList();
        long nbUnsucceedJobs = 0;
        do {
            Thread.sleep(WAITING_TIME_MS);
            jobs = jobInfoService.retrieveJobs();
            LOG.info("#############################");
            LOG.info("Nb jobs : {}", jobs.size());
            LOG.info("-----------------------------");
            jobs.stream().forEach(f -> LOG.info("JOB -> {}", f.getStatus().getStatus()));
            LOG.info("#############################");
            nbUnsucceedJobs = jobs.stream()
                    .filter(f -> !f.getStatus().getStatus().equals(JobStatus.SUCCEEDED) && !f.getStatus().getStatus()
                            .equals(JobStatus.FAILED)).count();
        } while (jobs.isEmpty() || (nbUnsucceedJobs > 0));
        // Wait for events handled.
        Thread.sleep(2000);
        LOG.info("Waiting jobs succeed ok");
    }

    private void storeAIP(AIP aipToStore, Boolean storeMeta) throws ModuleException, InterruptedException {
        aipService.validateAndStore(new AIPCollection(aipToStore));
        aipService.storePage(new PageRequest(0, 100));
        waitForJobsFinished();
        if (storeMeta) {
            aipService.storeMetadata();
            waitForJobsFinished();
        }
    }

    private void updateAIP(AIP aipToUpdate, boolean partialProcess) throws InterruptedException, ModuleException {
        aipService.updateAip(aip.getId().toString(), aip, "Test update AIP");
        waitForJobsFinished();
        if (!partialProcess) {
            processStorage();
        }
    }

    private void processStorage() throws ModuleException, InterruptedException {
        // Simulate store scheduler
        aipService.storePage(new PageRequest(0, 500));
        waitForJobsFinished();
        // Simulate storeMetadata scheduler
        aipService.storeMetadata();
        waitForJobsFinished();
        // Simulate deletion of data files scheduler
        aipService.doDelete();
        waitForJobsFinished();
    }

    // Test for storage performance with 500 AIPs to store.
    //@Test
    public void performanceTest() throws ModuleException, InterruptedException, MalformedURLException {

        Date date = new Date();
        LOG.info("Starting creating AIPs");
        AIPCollection col = new AIPCollection();
        // 1. Generate 500 AIPs in db
        for (int i = 0; i < 500; i++) {
            col.add(getAIP());
        }
        aipService.validateAndStore(col);
        Date dateAfter = new Date();
        LOG.info("AIPs created in {}ms", dateAfter.getTime() - date.getTime());

        Assert.assertEquals(500, aipDao.findAllByState(AIPState.VALID, new PageRequest(0, 500)).getTotalElements());

        date = new Date();
        LOG.info("Start storage ...");
        aipService.storePage(new PageRequest(0, 500));
        dateAfter = new Date();
        LOG.info("Start storage run after {}ms", dateAfter.getTime() - date.getTime());
        waitForJobsFinished();
        dateAfter = new Date();
        LOG.info("Jobs done after {}ms", dateAfter.getTime() - date.getTime());

        date = new Date();
        LOG.info("Storing metadata");
        aipService.storeMetadata();
        dateAfter = new Date();
        LOG.info("Storing metadata run after {}ms", dateAfter.getTime() - date.getTime());
        waitForJobsFinished();
        dateAfter = new Date();
        LOG.info("Jobs done after {}ms", dateAfter.getTime() - date.getTime());

    }

    @Test
    @Requirements({ @Requirement("REGARDS_DSL_STO_AIP_010"), @Requirement("REGARDS_DSL_STOP_AIP_070") })
    public void createSuccessTest() throws ModuleException, InterruptedException {
        storeAIP(aip, true);
        Set<AIPEvent> events = waitForEventsReceived(AIPState.STORED, 1);
        Assert.assertEquals("There whould be only one datastorage success event", 1, events.size());

        AIPEvent event = events.stream().findFirst().get();
        Assert.assertEquals(aip.getId().toString(), event.getAipId());
        Optional<AIP> aipFromDB = aipDao.findOneByAipId(aip.getId().toString());
        Assert.assertEquals(AIPState.STORED, aipFromDB.get().getState());
        LOG.info("AIP {} stored", aip.getId().toString());

        // Check for metadata stored
        Set<StorageDataFile> dataFiles = dataFileDao.findAllByStateAndAip(DataFileState.STORED, aip);
        Assert.assertEquals(2, dataFiles.size());
        Assert.assertNotNull("AIP metadata checksum should be stored into DB",
                             dataFiles.stream()
                                     .filter(storageDataFile -> storageDataFile.getDataType().equals(DataType.AIP))
                                     .findFirst().get().getChecksum());
        // lets check that the data has been successfully stored into the two storages and nothing else
        StorageDataFile dataFile = dataFiles.stream().filter(df -> df.getDataType().equals(DataType.RAWDATA))
                .findFirst().get();
        Assert.assertTrue("stored raw data should have only 2 urls", dataFile.getUrls().size() == 2);
        String storedLocation1 = Paths
                .get(baseStorage1Location.getPath(), dataFile.getChecksum().substring(0, 3), dataFile.getChecksum())
                .toString();
        String storedLocation2 = Paths
                .get(baseStorage2Location.getPath(), dataFile.getChecksum().substring(0, 3), dataFile.getChecksum())
                .toString();
        Assert.assertTrue(dataFile.getUrls().stream().map(url -> url.getPath()).collect(Collectors.toSet())
                                  .containsAll(Sets.newHashSet(storedLocation1, storedLocation2)));
        // same for the aips
        StorageDataFile aip = dataFiles.stream().filter(df -> df.getDataType().equals(DataType.AIP)).findFirst().get();
        Assert.assertTrue("stored metadata should have only 2 urls", dataFile.getUrls().size() == 2);
        storedLocation1 = Paths
                .get(baseStorage1Location.getPath(), aip.getChecksum().substring(0, 3), aip.getChecksum()).toString();
        storedLocation2 = Paths
                .get(baseStorage2Location.getPath(), aip.getChecksum().substring(0, 3), aip.getChecksum()).toString();
        Assert.assertTrue(aip.getUrls().stream().map(url -> url.getPath()).collect(Collectors.toSet())
                                  .containsAll(Sets.newHashSet(storedLocation1, storedLocation2)));
    }

    @Test
    public void createFailOnDataTest() throws MalformedURLException, ModuleException, InterruptedException {
        // first lets change the data location to be sure it fails
        aip.getProperties().getContentInformations()
                .toArray(new ContentInformation[aip.getProperties().getContentInformations().size()])[0].getDataObject()
                .setUrls(Sets.newHashSet(new URL("file",
                                                 "",
                                                 Paths.get("src/test/resources/data_that_does_not_exists.txt").toFile()
                                                         .getAbsolutePath())));
        storeAIP(aip, true);

        // Wait for error event
        Set<AIPEvent> events = waitForEventsReceived(AIPState.STORAGE_ERROR, 2);
        Assert.assertEquals("There should be two error events. One per storage location (multistorage).",
                            2,
                            events.size());
        Optional<AIP> aipFromDB = aipDao.findOneByAipId(aip.getId().toString());
        Assert.assertEquals(AIPState.STORAGE_ERROR, aipFromDB.get().getState());
        LOG.info("AIP {} is in ERROR State", aip.getId().toString());
        Set<StorageDataFile> dataFiles = dataFileDao.findAllByStateAndAip(DataFileState.ERROR, aip);
        Assert.assertEquals(1, dataFiles.size());
        StorageDataFile dataFile = dataFiles.iterator().next();
        Assert.assertFalse("The data file should contains its error", dataFile.getFailureCauses().isEmpty());
    }

    @Test
    public void createFailOnMetadataTest() throws ModuleException, InterruptedException, IOException {
        LOG.info("");
        LOG.info("START -> createFailOnMetadataTest");
        LOG.info("---------------------------------");
        LOG.info("");
        Path workspacePath = workspaceService.getMicroserviceWorkspace();
        Set<PosixFilePermission> oldPermissions = Files.getPosixFilePermissions(workspacePath);
        try {
            // Run AIP storage
            aipService.validateAndStore(new AIPCollection(aip));
            aipService.storePage(new PageRequest(0, 100));
            waitForJobsFinished();
            LOG.info("Waiting for storage jobs ends OK");
            // to make the process fail just on metadata storage, lets remove permissions from the workspace
            Files.setPosixFilePermissions(workspacePath, Sets.newHashSet());
            aipService.storeMetadata();
            waitForJobsFinished();
            // Wait for error event
            Set<AIPEvent> events = waitForEventsReceived(AIPState.STORAGE_ERROR, 1);
            Assert.assertEquals("There should be one storage error event", 1, events.size());

            // Check state of AIP
            Optional<AIP> aipFromDB = aipDao.findOneByAipId(aip.getId().toString());
            Assert.assertNotEquals("Test failed because storage didn't failed! It succeeded!",
                                   AIPState.STORED,
                                   aipFromDB.get().getState());
            Assert.assertEquals(AIPState.STORAGE_ERROR, aipFromDB.get().getState());
            Set<StorageDataFile> dataFiles = dataFileDao.findAllByStateAndAip(DataFileState.STORED, aip);
            Assert.assertEquals("File should have been stored but not the metadatas", 1, dataFiles.size());
        } finally {
            // to avoid issues with following tests, lets set back the permissions
            Files.setPosixFilePermissions(workspacePath, oldPermissions);
            LOG.info("");
            LOG.info("STOP -> createFailOnMetadataTest");
            LOG.info("---------------------------------");
            LOG.info("");
        }
    }

    @Test
    @Requirements({ @Requirement("REGARDS_DSL_STO_AIP_030"), @Requirement("REGARDS_DSL_STO_AIP_040") })
    public void testUpdate() throws InterruptedException, ModuleException, URISyntaxException {
        // Allow deletion for all files to allow update.
        dsConfWithDeleteDisabled.getParameter(LocalDataStorage.LOCAL_STORAGE_DELETE_OPTION)
                .setValue(Boolean.TRUE.toString());
        pluginService.updatePluginConfiguration(dsConfWithDeleteDisabled);
        // first lets storeAndCreate the aip
        createSuccessTest();
        mockEventHandler.clear();
        // now that it is correctly created, lets say it has been updated and add a tag
        aip = aipDao.findOneByAipId(aip.getId().toString()).get();
        String newTag = "Exemple Tag For Fun";
        aip.getTags().add(newTag);
        Set<StorageDataFile> oldDataFiles = dataFileDao.findByAipAndType(aip, DataType.AIP);
        updateAIP(aip, false);
        Set<AIPEvent> events = waitForEventsReceived(AIPState.STORED, 1);
        Assert.assertEquals("There should be only one stored event for updated AIP", 1, events.size());
        Assert.assertTrue(!oldDataFiles.isEmpty());
        for (StorageDataFile oldDataFile : oldDataFiles) {
            for (URL url : oldDataFile.getUrls()) {
                Assert.assertFalse("The old data file should not exists anymore !" + url.getPath(),
                                   Files.exists(Paths.get(url.getPath())));
            }
        }

        AIP updatedAip = aipDao.findOneByAipId(aip.getId().toString()).get();
        Assert.assertEquals("AIP should be in STORED state", AIPState.STORED, updatedAip.getState());

        Assert.assertTrue("Updated AIP should contains new tag", updatedAip.getTags().contains(newTag));
        Set<Event> updateEvents = updatedAip.getHistory().stream()
                .filter(e -> e.getType().equals(EventType.UPDATE.toString())).collect(Collectors.toSet());
        // 3 update events : 1 update for update request, 2 updates for deletion of two metadata file in two storage
        Assert.assertEquals("There should be 3 update event in the updated aip history", 3, updateEvents.size());

        // After job is done, the new AIP metadata file should be present in local datastorage
        Set<StorageDataFile> metadatas = dataFileDao.findByAipAndType(updatedAip, DataType.AIP);
        Assert.assertEquals(1, metadatas.size());

        for (URL url : metadatas.stream().findFirst().get().getUrls()) {
            Assert.assertTrue("The new data file should exists !" + url.getPath(),
                              Files.exists(Paths.get(url.getPath())));
        }
    }

    @Test
    @Requirements({ @Requirement("REGARDS_DSL_STO_AIP_030"), @Requirement("REGARDS_DSL_STO_AIP_040") })
    public void testMultipleUpdates() throws InterruptedException, ModuleException, URISyntaxException {
        // Allow deletion for all files to allow update.
        dsConfWithDeleteDisabled.getParameter(LocalDataStorage.LOCAL_STORAGE_DELETE_OPTION)
                .setValue(Boolean.TRUE.toString());
        pluginService.updatePluginConfiguration(dsConfWithDeleteDisabled);
        // first lets storeAndCreate the aip
        LOG.info("==================> 1. Store new AIP");
        createSuccessTest();
        LOG.info("==================> 2. Store new AIP OK");
        mockEventHandler.clear();
        // now that it is correctly created, lets say it has been updated and add a tag
        aip = aipDao.findOneByAipId(aip.getId().toString()).get();
        String newTag = "Exemple Tag For Fun";
        aip.getTags().add(newTag);
        // Run first update request
        LOG.info("==================> 3. Update AIP Request");
        updateAIP(aip, true);
        LOG.info("==================> 4. Update AIP Request done");
        // Run second update request
        String newTag2 = "Another tag for fun";
        aip.getTags().add(newTag2);
        LOG.info("==================> 5. Update AIP Request");
        updateAIP(aip, true);
        LOG.info("==================> 6. Update AIP Request done");
        // Run a third update request
        String newTag3 = "Another tag for more fun";
        aip.getTags().add(newTag3);
        LOG.info("==================> 7. Update AIP Request");
        updateAIP(aip, true);
        LOG.info("==================> 8. Update AIP Request done");

        // Result  should be
        // - AIP should be in VALID state. Ready to be handled by storage process (StorePage -> StoreMeta)
        // - Only last update request should be saved in db. This request will be handled by update scheduler.
        Optional<AIP> runningAip = aipDao.findOneByAipId(aip.getId().toString());
        Assert.assertEquals("After an update request the AIP should be in VALID state",
                            AIPState.VALID.toString(),
                            runningAip.get().getState().toString());
        Assert.assertEquals("There should be only one AIP update request in db", 1, aipUpdateRepo.count());

        // Run update scheduler to check that the request cannot be handled yet cause the AIP is still not in STORED state.
        aipService.handleUpdateRequests();
        runningAip = aipDao.findOneByAipId(aip.getId().toString());
        Assert.assertEquals("After an update request the AIP should be in VALID state",
                            AIPState.VALID.toString(),
                            runningAip.get().getState().toString());
        Assert.assertEquals("There should be only one AIP update request in db", 1, aipUpdateRepo.count());

        // Run process to end first update pending.
        LOG.info("==================> 9. Process first Update AIP Request");
        processStorage();
        LOG.info("==================> 10. First Update AIP Request done");
        waitForEventsReceived(AIPState.STORED, 1);
        mockEventHandler.clear();

        runningAip = aipDao.findOneByAipId(aip.getId().toString());
        Assert.assertEquals("After storage process the AIP should be in STORED state",
                            AIPState.STORED.toString(),
                            runningAip.get().getState().toString());
        Assert.assertEquals("There should be only one AIP update request in db", 1, aipUpdateRepo.count());

        Assert.assertTrue("Updated AIP should contains new tag", runningAip.get().getTags().contains(newTag));
        Assert.assertFalse("Updated AIP should not contains second update new tag",
                           runningAip.get().getTags().contains(newTag2));
        Assert.assertFalse("Updated AIP should not contains third update new tag",
                           runningAip.get().getTags().contains(newTag3));

        // Run update scheduler to handle the update request nw that the aip is in STORED state
        LOG.info("==================> 11. Process second Update AIP Request");
        aipService.handleUpdateRequests();
        runningAip = aipDao.findOneByAipId(aip.getId().toString());
        Assert.assertEquals("After an update request the AIP should be in VALID state",
                            AIPState.VALID.toString(),
                            runningAip.get().getState().toString());
        Assert.assertEquals("There should be no more AIP update request in db.", 0, aipUpdateRepo.count());

        LOG.info("==================> 12. Process second Update AIP Request done");
        // Run process to end last update request
        processStorage();

        Set<AIPEvent> events = waitForEventsReceived(AIPState.STORED, 1);
        Assert.assertEquals("There should be one stored events for updated AIP", 1, events.size());

        Optional<AIP> updatedAip = aipDao.findOneByAipId(aip.getId().toString());

        Assert.assertTrue("Updated AIP should contains new tag", updatedAip.get().getTags().contains(newTag));
        Assert.assertTrue("Updated AIP should contains new tag", updatedAip.get().getTags().contains(newTag2));
        Assert.assertTrue("Updated AIP should contains new tag", updatedAip.get().getTags().contains(newTag3));

    }

    @Test
    @Requirements({ @Requirement("REGARDS_DSL_STO_ARC_100"), @Requirement("REGARDS_DSL_STO_AIP_310") })
    public void testPartialDeleteAip() throws InterruptedException, ModuleException, URISyntaxException {
        createSuccessTest();
        String aipIpId = aip.getId().toString();
        // lets get all the dataFile before deleting them for further verification
        Set<StorageDataFile> aipFiles = dataFileDao.findAllByAip(aip);
        Assert.assertEquals(0, aipService.deleteAip(aipIpId).size());
        waitForJobsFinished();

        aipService.removeDeletedAIPMetadatas();
        aipService.doDelete();

        // Wait for AIP deleteion
        Set<AIPEvent> events = waitForEventsReceived(AIPState.DELETED, 2);
        Assert.assertEquals("There should not been any AIP delete event ", 1, events.size());
        Assert.assertTrue("AIP should be referenced in the database", aipDao.findOneByAipId(aipIpId).isPresent());
        for (StorageDataFile df : aipFiles) {
            // As only one of the two storage system allow deletion, only one file should be deleted on disk
            if (df.getDataType().equals(DataType.AIP)) {
                for (URL fileLocation : df.getUrls()) {
                    Assert.assertTrue(
                            "AIP metadata should be on disk. As a datafile cannot be deleted metadata should never be deleted.",
                            Files.exists(Paths.get(fileLocation.toURI())));
                }
            } else {
                for (URL fileLocation : df.getUrls()) {
                    if (fileLocation.toString().contains(baseStorage1Location.toString())) {
                        Assert.assertFalse("AIP data should not be on disk anymore",
                                           Files.exists(Paths.get(fileLocation.toURI())));
                    } else if (fileLocation.toString().contains(baseStorage2Location.toString())) {
                        Assert.assertTrue("AIP data should be on disk. The storage configuration do not allow deletion",
                                          Files.exists(Paths.get(fileLocation.toURI())));
                    } else {
                        Assert.fail("The file should not be stored in " + fileLocation.toString());
                    }
                }
            }
        }
    }

    @Test
    @Requirements({ @Requirement("REGARDS_DSL_STO_ARC_100"), @Requirement("REGARDS_DSL_STO_AIP_115") })
    public void testDeleteAip() throws InterruptedException, ModuleException, URISyntaxException {

        dsConfWithDeleteDisabled.getParameter(LocalDataStorage.LOCAL_STORAGE_DELETE_OPTION)
                .setValue(Boolean.TRUE.toString());
        pluginService.updatePluginConfiguration(dsConfWithDeleteDisabled);

        createSuccessTest();
        String aipIpId = aip.getId().toString();
        // lets get all the dataFile before deleting them for further verification
        Set<StorageDataFile> aipFiles = dataFileDao.findAllByAip(aip);
        Assert.assertEquals(0, aipService.deleteAip(aipIpId).size());
        AIP deleted = aipService.retrieveAip(aipIpId);
        Optional<Event> deletionEventOpt = deleted.getHistory().stream()
                .filter(evt -> evt.getType().equals(EventType.DELETION.toString())).findAny();
        Assert.assertTrue("Deletion event should be present into AIP history", deletionEventOpt.isPresent());
        Assert.assertNotNull("Deletion event date should not be null", deletionEventOpt.get().getDate());
        aipService.doDelete();
        waitForJobsFinished();

        aipService.removeDeletedAIPMetadatas();
        aipService.doDelete();
        waitForJobsFinished();

        // Wait for AIP deletion
        Set<AIPEvent> events = waitForEventsReceived(AIPState.DELETED, 1);
        waitForJobsFinished();
        Assert.assertEquals("There should been only one AIP delete event ", 1, events.size());
        Assert.assertFalse("AIP should not be referenced in the database", aipDao.findOneByAipId(aipIpId).isPresent());
        for (StorageDataFile df : aipFiles) {
            // As only one of the two storage system allow deletion, only one file should be deleted on disk
            for (URL fileLocation : df.getUrls()) {
                Assert.assertFalse("AIP data should not be on disk anymore",
                                   Files.exists(Paths.get(fileLocation.toURI())));
            }
        }
    }

    @Test
    public void testDeleteFileFromSingleDS() throws InterruptedException, ModuleException {
        createSuccessTest();
        Map<StorageDataFile, String> undeletableFileCauseMap = aipService
                .deleteFilesFromDataStorage(Sets.newHashSet(aip.getId().toString()),
                                            pluginService.getPluginConfigurationByLabel(DATA_STORAGE_1_CONF_LABEL)
                                                    .getId());
        Assert.assertEquals("All data file should be deletable from first data storage",
                            0,
                            undeletableFileCauseMap.size());
        for (StorageDataFile sdf : dataFileDao.findAllByAip(aip)) {
            Assert.assertEquals("Data files should be waiting to be deleted from 1 archive",
                                (Long) 1L,
                                sdf.getNotYetDeletedBy());
        }
        AIP dbAIP = aipDao.findOneByAipId(aip.getId().toString()).get();
        Assert.assertEquals("AIP should be in state STORED", AIPState.STORED, dbAIP.getState());
        // once the job has finished, file should be deleted from data storage 1 and not 2, aip should be in state STORED,
        // data files should be in state STORED too
        waitForJobsFinished();
        dbAIP = aipDao.findOneByAipId(aip.getId().toString()).get();
        Assert.assertEquals("AIP should be in state STORED", AIPState.STORED, dbAIP.getState());
        Optional<Event> deletionEventOpt = dbAIP.getHistory().stream()
                .filter(evt -> evt.getType().equals(EventType.DELETION.toString())).findAny();
        Assert.assertTrue("Deletion event should be present into AIP history", deletionEventOpt.isPresent());
        Assert.assertNotNull("Deletion event date should not be null", deletionEventOpt.get().getDate());
        Set<StorageDataFile> dataFiles = dataFileDao.findAllByAip(dbAIP);
        for (StorageDataFile dataFile : dataFiles) {
            Assert.assertEquals("Data file should have been deleted from all archive it should have been",
                                (Long) 0L,
                                dataFile.getNotYetDeletedBy());
            Assert.assertEquals("Data file should have only 1 url", 1, dataFile.getUrls().size());
            Assert.assertEquals("Data file should be stored by only one data storage",
                                1,
                                dataFile.getPrioritizedDataStorages().size());
            Assert.assertEquals("Data file should be in state STORED", DataFileState.STORED, dataFile.getState());
            Path storedLocation1 = Paths.get(baseStorage1Location.getPath(),
                                             dataFile.getChecksum().substring(0, 3),
                                             dataFile.getChecksum());
            Assert.assertTrue("Data file should not be stored in data storage 1 anymore",
                              !storedLocation1.toFile().exists());
        }

    }

    /**
     * Tries to remove files from last data storage.
     */
    @Test
    public void testDeleteFileFromSingleDSFail() throws InterruptedException, ModuleException {
        createSuccessTest();
        // lets remove it from the first
        testDeleteFileFromSingleDS();
        Map<StorageDataFile, String> undeletableFileCauseMap = aipService
                .deleteFilesFromDataStorage(Sets.newHashSet(aip.getId().toString()),
                                            pluginService.getPluginConfigurationByLabel(DATA_STORAGE_2_CONF_LABEL)
                                                    .getId());
        Assert.assertEquals("None of the data file(2: metadata and data) should be deletable",
                            2,
                            undeletableFileCauseMap.size());
        // check that everything is as before i.e: All StorageDataFiles are in state STORED,
        // have 1 url(2 urls after createSuccessTest - 1 after testDeleteFileFromSingleDS()), are stored on 2 locations
        // and AIP is in state stored
        AIP dbAIP = aipDao.findOneByAipId(aip.getId().toString()).get();
        Assert.assertEquals("AIP should be in state STORED", AIPState.STORED, dbAIP.getState());
        Set<StorageDataFile> dataFiles = dataFileDao.findAllByAip(dbAIP);
        for (StorageDataFile dataFile : dataFiles) {
            Assert.assertEquals(
                    "Data file are undeletable so they should not have been marked to be deleted by any archive",
                    (Long) 0L,
                    dataFile.getNotYetDeletedBy());
            Assert.assertEquals("Data file should have only 1 url", 1, dataFile.getUrls().size());
            Assert.assertEquals("Data file should be stored by only one data storage",
                                1,
                                dataFile.getPrioritizedDataStorages().size());
            Assert.assertEquals("Data file should be in state STORED", DataFileState.STORED, dataFile.getState());
            Path storedLocation1 = Paths.get(baseStorage1Location.getPath(),
                                             dataFile.getChecksum().substring(0, 3),
                                             dataFile.getChecksum());
            Assert.assertTrue("Data file should not be stored in data storage 1 anymore",
                              !storedLocation1.toFile().exists());
        }
    }

    @Test
    public void searchSession() throws ModuleException, InterruptedException {
        createSuccessTest();
        Page<AIPSession> sessions = aipService.searchSessions(SESSION,
                                                              OffsetDateTime.now().minusDays(1L),
                                                              OffsetDateTime.now().plusDays(1L),
                                                              new PageRequest(0, 100));
        Assert.assertEquals(1, sessions.getTotalElements());
        Assert.assertEquals(SESSION, sessions.getContent().stream().findFirst().get().getId());
        Assert.assertEquals(2, sessions.getContent().stream().findFirst().get().getDataFilesCount());
        Assert.assertEquals(2, sessions.getContent().stream().findFirst().get().getStoredDataFilesCount());
        Assert.assertEquals(1, sessions.getContent().stream().findFirst().get().getAipsCount());
        Assert.assertEquals(1, sessions.getContent().stream().findFirst().get().getStoredAipsCount());
    }

    @Test
    @Requirements({ @Requirement("REGARDS_DSL_STO_ARC_100") })
    public void testDeleteSip() throws ModuleException, InterruptedException, MalformedURLException {

        dsConfWithDeleteDisabled.getParameter(LocalDataStorage.LOCAL_STORAGE_DELETE_OPTION)
                .setValue(Boolean.TRUE.toString());
        pluginService.updatePluginConfiguration(dsConfWithDeleteDisabled);

        // store a first AIP
        UniformResourceName sipId = new UniformResourceName(OAISIdentifier.SIP,
                                                            EntityType.COLLECTION,
                                                            getDefaultTenant(),
                                                            UUID.randomUUID(),
                                                            1);
        aip.setSipId(sipId);
        storeAIP(aip, true);
        String aipIpId = aip.getId().toString();

        // store a second AIP with the same sipId
        AIP newAip = getAIP();
        newAip.setSipId(aip.getSipId().orElse(null));
        storeAIP(newAip, true);

        // delete the two AIP with the same sipId
        aipService.deleteAipFromSip(aip.getSipIdUrn().get());

        Thread.sleep(5000);

        boolean exceptionThrow = false;

        // the data files should be deleted
        try {
            aipService.getAIPDataFile(aipIpId, CHECKSUM);
        } catch (EntityNotFoundException | IOException e) {
            exceptionThrow = true;
        }

        Assert.assertTrue(exceptionThrow);

        // delete the AIP metadata
        aipService.removeDeletedAIPMetadatas();
        aipService.doDelete();
        waitForJobsFinished();

        // Wait for AIP deletion events
        Set<AIPEvent> events = waitForEventsReceived(AIPState.DELETED, 2);
        waitForJobsFinished();
        Assert.assertEquals("There should been only two AIP delete event ", 2, events.size());
        Assert.assertFalse("AIP should not be referenced in the database", aipDao.findOneByAipId(aipIpId).isPresent());
    }

    @Test
    public void testDeleteErrorAip() throws InterruptedException, ModuleException, URISyntaxException {
        dsConfWithDeleteDisabled.getParameter(LocalDataStorage.LOCAL_STORAGE_DELETE_OPTION)
                .setValue(Boolean.TRUE.toString());
        pluginService.updatePluginConfiguration(dsConfWithDeleteDisabled);

        // Store a new AIP
        storeAIP(aip, false);

        aip = aipService.retrieveAip(aip.getId().toString());
        // Simulate aip state to STORE_ERROR
        aip.setState(AIPState.STORAGE_ERROR);
        aip = aipService.save(aip, false);

        // lets get all the dataFile before deleting them for further verification
        String aipIpId = aip.getId().toString();
        Set<StorageDataFile> aipFiles = dataFileDao.findAllByAip(aip);

        // Delete AIP
        Assert.assertEquals(0, aipService.deleteAip(aipIpId).size());
        waitForJobsFinished();
        aipService.removeDeletedAIPMetadatas();
        aipService.doDelete();
        waitForJobsFinished();

        // Wait for AIP deletion
        Set<AIPEvent> events = waitForEventsReceived(AIPState.DELETED, 1);
        Assert.assertEquals("There should been only one AIP delete event ", 1, events.size());
        Assert.assertFalse("AIP should not be referenced in the database", aipDao.findOneByAipId(aipIpId).isPresent());
        for (StorageDataFile df : aipFiles) {
            // All files should be deleted. But no AIP metadata as it was not stored
            Assert.assertFalse("No AIP metadata should be stored", DataType.AIP.equals(df.getDataType()));
            for (URL fileLocation : df.getUrls()) {
                Assert.assertFalse("AIP data should not be on disk anymore",
                                   Files.exists(Paths.get(fileLocation.toURI())));
            }
        }
    }

    @Test
    @Requirement("REGARDS_DSL_STO_AIP_210")
    public void testUpdatePDI()
            throws MalformedURLException, EntityNotFoundException, EntityInconsistentIdentifierException {
        AIP aip = getAIP();
        aip.setState(AIPState.STORED);
        AIPSession aipSession = aipService.getSession(aip.getSession(), true);
        aip = aipDao.save(aip, aipSession);
        // we are going to add an update event, so lets get the old event
        int oldHistorySize = aip.getHistory().size();
        AIPBuilder updated = new AIPBuilder(aip);
        updated.addEvent(EventType.UPDATE.name(), "lets test update", OffsetDateTime.now());
        AIP preUpdateAIP = updated.build();
        AIP updatedAip = aipService.updateAip(aip.getId().toString(), preUpdateAIP, "Test update AIP").get();
        Assert.assertEquals("new history size should be oldhistorysize + 2",
                            oldHistorySize + 2,
                            updatedAip.getHistory().size());
    }

    private AIP getAIP() throws MalformedURLException {

        UniformResourceName sipId = new UniformResourceName(OAISIdentifier.SIP,
                                                            EntityType.DATA,
                                                            getDefaultTenant(),
                                                            UUID.randomUUID(),
                                                            1);
        UniformResourceName aipId = new UniformResourceName(OAISIdentifier.AIP,
                                                            EntityType.DATA,
                                                            getDefaultTenant(),
                                                            sipId.getEntityId(),
                                                            1);
        AIPBuilder aipBuilder = new AIPBuilder(aipId, Optional.of(sipId), "providerId", EntityType.DATA, SESSION);

        Path path = Paths.get("src", "test", "resources", "data.txt");
        aipBuilder.getContentInformationBuilder().setDataObject(DataType.RAWDATA, path, "MD5", CHECKSUM);
        aipBuilder.getContentInformationBuilder().setSyntax("text", "description", MimeType.valueOf("text/plain"));
        aipBuilder.addContentInformation();
        aipBuilder.getPDIBuilder().setAccessRightInformation("public");
        aipBuilder.getPDIBuilder().setFacility("CS");
        aipBuilder.getPDIBuilder()
                .addProvenanceInformationEvent(EventType.SUBMISSION.name(), "test event", OffsetDateTime.now());
        aipBuilder.addTags("tag");

        return aipBuilder.build();
    }

    @After
    public void cleanUp() throws URISyntaxException, IOException {
        pluginService.cleanPluginCache();
        subscriber.unsubscribeFrom(AIPEvent.class);
        subscriber.purgeQueue(AIPEvent.class, mockEventHandler.getClass());
        subscriber.purgeQueue(DataStorageEvent.class, DataStorageEventHandler.class);
        clearDb();
    }

    private void clearDb() {
        jobInfoRepo.deleteAll();
        dataFileDao.deleteAll();
        aipUpdateRepo.deleteAll();
        aipDao.deleteAll();
        prioritizedDataStorageRepository.deleteAll();
        pluginRepo.deleteAll();
        sessionRepo.deleteAll();
    }

    @Configuration
    static class Config {

        @Bean
        public INotificationClient notificationClient() {
            return Mockito.mock(INotificationClient.class);
        }
    }

}
