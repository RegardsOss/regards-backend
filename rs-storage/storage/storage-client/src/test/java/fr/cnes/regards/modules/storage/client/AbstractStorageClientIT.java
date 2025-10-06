/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.dto.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.session.agent.dao.IStepPropertyUpdateRequestRepository;
import fr.cnes.regards.framework.modules.session.commons.dao.ISessionStepRepository;
import fr.cnes.regards.framework.modules.session.commons.dao.ISnapshotProcessRepository;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.fileaccess.dto.request.FileReferenceRequestDto;
import fr.cnes.regards.modules.fileaccess.dto.request.FileStorageRequestDto;
import fr.cnes.regards.modules.filecatalog.client.RequestInfo;
import fr.cnes.regards.modules.storage.dao.*;
import fr.cnes.regards.modules.storage.domain.database.StorageLocationConfiguration;
import fr.cnes.regards.modules.storage.domain.database.request.FileReferenceRequestAggregation;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequestAggregation;
import fr.cnes.regards.modules.storage.service.file.FileReferenceService;
import fr.cnes.regards.modules.storage.service.location.StorageLocationConfigurationService;
import fr.cnes.regards.modules.storage.service.plugin.SimpleNearlineDataStorage;
import fr.cnes.regards.modules.storage.service.plugin.SimpleOnlineTestClient;
import lombok.SneakyThrows;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Parent class of all the StorageClientXxxIT where Xxx is the client operation being tested.
 *
 * @author sbinda
 * @author onavarro
 */
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@ActiveProfiles(value = { "default", "test", "testAmqp", "storageTest" }, inheritProfiles = false)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS, hierarchyMode = HierarchyMode.EXHAUSTIVE)
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_client_tests",
                                   "regards.amqp.enabled=true",
                                   "regards.storage.schedule.initial.delay=100",
                                   "regards.storage.schedule.delay=100",
                                   "regards.storage.storage.requests.per.job=15" },
                    locations = { "classpath:application-test.properties" })
public abstract class AbstractStorageClientIT extends AbstractMultitenantServiceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStorageClientIT.class);

    @Autowired
    protected StorageListener listener;

    @Autowired
    protected StorageClient client;

    @Autowired
    protected FileReferenceService fileRefService;

    @Autowired
    protected IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    protected StorageLocationConfigurationService storageLocationConfService;

    @Autowired
    protected IFileStorageRequestRepository storageReqRepo;

    @Autowired
    protected IFileReferenceRequestRepository referenceReqRepo;

    @Autowired
    protected IFileReferenceWithOwnersRepository fileReferenceWithOwnersRepository;

    @Autowired
    protected IFileCopyRequestRepository copyReqRepo;

    @Autowired
    protected IFileCacheRequestRepository cacheReqRepo;

    @Autowired
    protected IRequestGroupRepository reqGroupRepo;

    @Autowired
    protected IGroupRequestInfoRepository reqInfoRepo;

    @Autowired
    protected IFileReferenceRepository fileRefRepo;

    @Autowired
    protected IJobInfoRepository jobInfoRepo;

    @Autowired
    protected ISnapshotProcessRepository snapshotProcessRepository;

    @Autowired
    protected ISessionStepRepository sessionStepRepository;

    @Autowired
    protected IStepPropertyUpdateRequestRepository stepPropertyUpdateRequestRepository;

    @Autowired
    protected IPublisher publisher;

    protected Path fileToStore;

    private static final int DEFAULT_MAX_WAITING_DURATION_IN_SEC = 60;

    protected static final String ONLINE_CONF = "ONLINE_CONF";

    protected static final String NEARLINE_CONF = "NEARLINE_CONF";

    protected static final String NEARLINE_CONF_2 = "NEARLINE_CONF_2";

    protected final Set<String> storedFileChecksums = Sets.newHashSet();

    protected final Set<String> restorableFileChecksums = Sets.newHashSet();

    protected final Set<String> unrestorableFileChecksums = Sets.newHashSet();

    protected final Set<String> referenceFileChecksums = Sets.newHashSet();

    protected static final String SESSION_OWNER = "SOURCE 1";

    protected static final String SESSION = "SESSION 1";

    @After
    public void cleanup() throws IOException {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        clearRepositories();
        listener.reset();
    }

    private void clearRepositories() {
        // Delete FileStorageRequest and related owners
        List<FileStorageRequestAggregation> fileStorageRequests = storageReqRepo.findAll();
        storageReqRepo.deleteAll(fileStorageRequests);
        List<FileReferenceRequestAggregation> fileRefenceRequests = referenceReqRepo.findAll();
        referenceReqRepo.deleteAll(fileRefenceRequests);
        copyReqRepo.deleteAll();
        cacheReqRepo.deleteAll();

        reqInfoRepo.deleteAll();
        reqGroupRepo.deleteAll();
        fileRefRepo.deleteAll();
        jobInfoRepo.deleteAll();
        snapshotProcessRepository.deleteAllInBatch();
        stepPropertyUpdateRequestRepository.deleteAllInBatch();
        sessionStepRepository.deleteAllInBatch();
    }

    @Before
    public void init() throws IOException, ModuleException {
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        clearRepositories();

        fileToStore = Paths.get("target/file-to-store.test");
        if (!Files.exists(fileToStore)) {
            Files.createFile(fileToStore);
        }

        runtimeTenantResolver.forceTenant(getDefaultTenant());
        if (!storageLocationConfService.search(ONLINE_CONF).isPresent()) {
            initDataStorageOnlinePluginConfiguration();
        }
        if (!storageLocationConfService.search(NEARLINE_CONF).isPresent()) {
            initDataStorageNearLinePluginConfiguration(NEARLINE_CONF, "target/nearline-storage-1");
        }
        if (!storageLocationConfService.search(NEARLINE_CONF_2).isPresent()) {
            initDataStorageNearLinePluginConfiguration(NEARLINE_CONF_2, "target/nearline-storage-2");
        }

        Assert.assertTrue(storageLocationConfService.search(ONLINE_CONF).isPresent());
        Assert.assertTrue(storageLocationConfService.search(NEARLINE_CONF).isPresent());
        Assert.assertTrue(storageLocationConfService.search(NEARLINE_CONF_2).isPresent());

        listener.reset();
        simulateApplicationReadyEvent();
        simulateApplicationStartedEvent();
        listener.reset();
    }

    @SneakyThrows
    protected String newUrl() {
        return new URL("file", null, fileToStore.toFile().getAbsolutePath()).toString();
    }

    protected FileStorageRequestDto newFileStorageRequestDto(String filename, String configuration) {
        return newFileStorageRequestDto(filename, UUID.randomUUID().toString(), configuration);
    }

    protected FileStorageRequestDto newFileStorageRequestDto(String filename,
                                                             String uuidChecksum,
                                                             String configuration) {
        return FileStorageRequestDto.build(filename,
                                           uuidChecksum,
                                           "UUID",
                                           MediaType.APPLICATION_OCTET_STREAM_VALUE,
                                           "owner",
                                           SESSION_OWNER,
                                           SESSION,
                                           newUrl(),
                                           configuration,
                                           null);
    }

    protected void storeFile() {
        // GIVEN  a list of 5 FileStorageRequest
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        final FileStorageRequestDto file1 = newFileStorageRequestDto("file.test", ONLINE_CONF);
        final FileStorageRequestDto file2 = newFileStorageRequestDto("file2.test", ONLINE_CONF);
        final FileStorageRequestDto file3 = newFileStorageRequestDto("restoError.file3.test", NEARLINE_CONF);
        final FileStorageRequestDto file4 = newFileStorageRequestDto("file4.test", NEARLINE_CONF);
        final FileStorageRequestDto file5 = newFileStorageRequestDto(AvailabilityUpdateCustomTestAction.FILE_TO_UPDATE_NAME,
                                                                     AvailabilityUpdateCustomTestAction.FILE_TO_UPDATE_CHECKSUM,
                                                                     NEARLINE_CONF);
        final Set<FileStorageRequestDto> files = Set.of(file1, file2, file3, file4, file5);

        // WHEN store
        listener.reset();
        final Collection<RequestInfo> infos = client.store(files);

        // THEN
        // expect 1 group
        Assert.assertEquals(1, infos.size());
        final RequestInfo info = infos.iterator().next();
        Assert.assertNotNull(info);

        // expect all request in group granted and successful
        waitRequestEnds(1);
        Assert.assertTrue("Request should be granted", listener.getGranted().contains(info));
        Assert.assertTrue("Request should be successful", listener.getSuccess().containsKey(info));
        Assert.assertEquals("Group request should contains 5 success request",
                            files.size(),
                            listener.getSuccess().get(info).size());
        Assert.assertFalse("Request should not be error", listener.getErrors().containsKey(info));

        // collect the checksums to potentially be used later in some test
        // first 4 checksums
        Stream.of(file1, file2, file3, file4).map(FileStorageRequestDto::getChecksum).forEach(storedFileChecksums::add);

        // not checksum at index 2 and 4
        Stream.of(file1, file2, file4).map(FileStorageRequestDto::getChecksum).forEach(restorableFileChecksums::add);

        // checksum at index 2
        unrestorableFileChecksums.add(file3.getChecksum());

        listener.reset();
    }

    private FileReferenceRequestDto newFileReferenceRequestDto(String filename) {
        String owner = "refe-test";
        String sessionOwner = "source1";
        String session = "session1";
        String storage = "somewhere";
        String baseURl = "file://here/it/is/";
        return FileReferenceRequestDto.build(filename,
                                             UUID.randomUUID().toString(),
                                             "UUID",
                                             MediaType.APPLICATION_OCTET_STREAM_VALUE,
                                             10L,
                                             owner,
                                             storage,
                                             baseURl + filename,
                                             sessionOwner,
                                             session);
    }

    protected void referenceMultipleFiles() {

        final Set<FileReferenceRequestDto> files = IntStream.of(1, 2, 3)
                                                            .mapToObj(i -> "file" + i + ".test")
                                                            .map(this::newFileReferenceRequestDto)
                                                            .collect(Collectors.toUnmodifiableSet());

        final Collection<RequestInfo> infos = client.reference(files);
        Assert.assertEquals(1, infos.size());
        RequestInfo info = infos.iterator().next();
        waitRequestEnds(1);

        Assert.assertTrue("Request should be granted", listener.getGranted().contains(info));
        Assert.assertTrue("Request should be successful", listener.getSuccess().containsKey(info));
        Assert.assertFalse("Request should not be error", listener.getErrors().containsKey(info));

        files.stream().map(FileReferenceRequestDto::getChecksum).forEach(referenceFileChecksums::add);
    }

    private StorageLocationConfiguration initDataStorageOnlinePluginConfiguration() {
        try {
            PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(SimpleOnlineTestClient.class);
            Files.createDirectories(Paths.get("target/online-storage/"));

            Set<IPluginParam> parameters = IPluginParam.set(IPluginParam.build(SimpleOnlineTestClient.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                                                                               "target/online-storage/"),
                                                            IPluginParam.build(SimpleOnlineTestClient.HANDLE_STORAGE_ERROR_FILE_PATTERN,
                                                                               "error.*"),
                                                            IPluginParam.build(SimpleOnlineTestClient.HANDLE_DELETE_ERROR_FILE_PATTERN,
                                                                               "delErr.*"));
            PluginConfiguration dataStorageConf = new PluginConfiguration(ONLINE_CONF,
                                                                          parameters,
                                                                          0,
                                                                          dataStoMeta.getPluginId());
            return storageLocationConfService.create(ONLINE_CONF, dataStorageConf, 1_000_000L);
        } catch (IOException | ModuleException e) {
            Assert.fail(e.getMessage());
            return null;
        }
    }

    private StorageLocationConfiguration initDataStorageNearLinePluginConfiguration(String name,
                                                                                    String storageDirectory)
        throws ModuleException {
        try {
            PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(SimpleNearlineDataStorage.class);
            Files.createDirectories(Paths.get(storageDirectory));
            Set<IPluginParam> parameters = IPluginParam.set(IPluginParam.build(SimpleNearlineDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                                                                               storageDirectory),
                                                            IPluginParam.build(SimpleNearlineDataStorage.HANDLE_STORAGE_ERROR_FILE_PATTERN,
                                                                               "error.*"),
                                                            IPluginParam.build(SimpleNearlineDataStorage.HANDLE_RESTORATION_ERROR_FILE_PATTERN,
                                                                               "restoError.*"),
                                                            IPluginParam.build(SimpleNearlineDataStorage.HANDLE_DELETE_ERROR_FILE_PATTERN,
                                                                               "delErr.*"));
            PluginConfiguration dataStorageConf = new PluginConfiguration(name,
                                                                          parameters,
                                                                          0,
                                                                          dataStoMeta.getPluginId());
            return storageLocationConfService.create(name, dataStorageConf, 1_000_000L);
        } catch (IOException e) {
            throw new ModuleException(e.getMessage(), e);
        }
    }

    protected void waitRequestEnds(int nbRequests) {
        waitRequestEnds(nbRequests, DEFAULT_MAX_WAITING_DURATION_IN_SEC);
    }

    protected void waitRequestEnds(int nbRequests, int maxDurationSec) {
        Awaitility.await().atMost(maxDurationSec, TimeUnit.SECONDS).until(() -> {
            long count = listener.getNbRequestEnds();
            LOGGER.info("Waiting Number of requests requested for end {}/{}", count, nbRequests);
            return count >= nbRequests;
        });
    }
}
