/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesDeletionEvent;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesReferenceEvent;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesRestorationRequestEvent;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesStorageRequestEvent;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileRequestsGroupEvent;
import fr.cnes.regards.modules.filecatalog.client.RequestInfo;
import fr.cnes.regards.modules.filecatalog.dto.*;
import fr.cnes.regards.modules.filecatalog.dto.request.*;
import fr.cnes.regards.modules.storage.dao.*;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.StorageLocationConfiguration;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequestAggregation;
import fr.cnes.regards.modules.storage.service.file.FileReferenceService;
import fr.cnes.regards.modules.storage.service.location.StorageLocationConfigurationService;
import fr.cnes.regards.modules.storage.service.plugin.SimpleNearlineDataStorage;
import fr.cnes.regards.modules.storage.service.plugin.SimpleOnlineTestClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author sbinda
 */
@ActiveProfiles(value = { "default", "test", "testAmqp", "storageTest" }, inheritProfiles = false)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS, hierarchyMode = HierarchyMode.EXHAUSTIVE)
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_client_tests",
                                   "regards.amqp.enabled=true",
                                   "regards.storage.schedule.initial.delay=100",
                                   "regards.storage.schedule.delay=100",
                                   "regards.storage.storage.requests.per.job=15" },
                    locations = { "classpath:application-test.properties" })
public class StorageClientIT extends AbstractMultitenantServiceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageClientIT.class);

    @Autowired
    private StorageListener listener;

    @Autowired
    private StorageClient client;

    @Autowired
    private FileReferenceService fileRefService;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private StorageLocationConfigurationService storageLocationConfService;

    @Autowired
    private IFileStorageRequestRepository storageReqRepo;

    @Autowired
    private IFileReferenceWithOwnersRepository fileReferenceWithOwnersRepository;

    @Autowired
    private IFileCopyRequestRepository copyReqRepo;

    @Autowired
    private IFileCacheRequestRepository cacheReqRepo;

    @Autowired
    private IRequestGroupRepository reqGroupRepo;

    @Autowired
    private IGroupRequestInfoRepository reqInfoRepo;

    @Autowired
    private IFileReferenceRepository fileRefRepo;

    @Autowired
    protected IJobInfoRepository jobInfoRepo;

    @Autowired
    private ISnapshotProcessRepository snapshotProcessRepository;

    @Autowired
    private ISessionStepRepository sessionStepRepository;

    @Autowired
    private IStepPropertyUpdateRequestRepository stepPropertyUpdateRequestRepository;

    @Autowired
    private IPublisher publisher;

    private Path fileToStore;

    private static final String ONLINE_CONF = "ONLINE_CONF";

    private static final String NEARLINE_CONF = "NEARLINE_CONF";

    private static final String NEARLINE_CONF_2 = "NEARLINE_CONF_2";

    private final Set<String> storedFileChecksums = Sets.newHashSet();

    private final Set<String> restorableFileChecksums = Sets.newHashSet();

    private final Set<String> unrestorableFileChecksums = Sets.newHashSet();

    private final Set<String> referenceFileChecksums = Sets.newHashSet();

    private static final String SESSION_OWNER = "SOURCE 1";

    private static final String SESSION = "SESSION 1";

    @Before
    public void init() throws IOException, ModuleException {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Delete FileStorageRequest and related owners
        List<FileStorageRequestAggregation> fileStorageRequests = storageReqRepo.findAll();
        storageReqRepo.deleteAll(fileStorageRequests);
        copyReqRepo.deleteAll();
        cacheReqRepo.deleteAll();
        reqInfoRepo.deleteAll();
        reqGroupRepo.deleteAll();
        fileRefRepo.deleteAll();
        jobInfoRepo.deleteAll();
        snapshotProcessRepository.deleteAllInBatch();
        stepPropertyUpdateRequestRepository.deleteAllInBatch();
        sessionStepRepository.deleteAllInBatch();

        fileToStore = Paths.get("target/file-to-store.test");
        if (!Files.exists(fileToStore)) {
            Files.createFile(fileToStore);
        }

        runtimeTenantResolver.forceTenant(getDefaultTenant());
        if (!storageLocationConfService.search(ONLINE_CONF).isPresent()) {
            initDataStoragePluginConfiguration();
        }
        if (!storageLocationConfService.search(NEARLINE_CONF).isPresent()) {
            initDataStorageNLPluginConfiguration(NEARLINE_CONF, "target/nearline-storage-1");
        }
        if (!storageLocationConfService.search(NEARLINE_CONF_2).isPresent()) {
            initDataStorageNLPluginConfiguration(NEARLINE_CONF_2, "target/nearline-storage-2");
        }

        Assert.assertTrue(storageLocationConfService.search(ONLINE_CONF).isPresent());
        Assert.assertTrue(storageLocationConfService.search(NEARLINE_CONF).isPresent());
        Assert.assertTrue(storageLocationConfService.search(NEARLINE_CONF_2).isPresent());

        listener.reset();
        simulateApplicationReadyEvent();
        simulateApplicationStartedEvent();

    }

    @Test
    public void eventListenerTest() throws InterruptedException {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        listener.reset();
        // Simulate multiples message from storage service
        int nbMessages = 1_000;
        for (int i = 0; i < nbMessages; i++) {
            String groupId = "group_" + i;
            String checksum = UUID.randomUUID().toString();
            FileReferenceMetaInfoDto metaInfo = new FileReferenceMetaInfoDto(checksum,
                                                                             "UUID",
                                                                             "file" + i,
                                                                             10L,
                                                                             null,
                                                                             null,
                                                                             MediaType.APPLICATION_JSON.toString(),
                                                                             null);

            FileLocationDto FileLocationDto = new FileLocationDto("storage", "path");
            HashSet<String> owners = Sets.newHashSet("owner");

            FileReferenceDto ref = new FileReferenceDto(OffsetDateTime.now(), metaInfo, FileLocationDto, owners);

            RequestResultInfoDto resultInfo = RequestResultInfoDto.build(groupId,
                                                                         checksum,
                                                                         "storage",
                                                                         "path",
                                                                         owners,
                                                                         ref,
                                                                         null);

            publisher.publish(FileRequestsGroupEvent.build(groupId,
                                                           FileRequestType.STORAGE,
                                                           FileGroupRequestStatus.SUCCESS,
                                                           Sets.newHashSet(resultInfo)));
        }
        LOGGER.info(" -------> Start waiting for all responses received !!!!!!!!!");
        // Wait for all events received
        waitRequestEnds(nbMessages, 60);
    }

    @Test
    public void storeWithMultipleRequests() throws MalformedURLException, InterruptedException {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        Set<FileStorageRequestDto> files = Sets.newHashSet();
        for (int i = 0; i < (FilesStorageRequestEvent.MAX_REQUEST_PER_GROUP + 1); i++) {
            files.add(FileStorageRequestDto.build("file.test",
                                                  UUID.randomUUID().toString(),
                                                  "UUID",
                                                  "application/octet-stream",
                                                  "owner",
                                                  SESSION_OWNER,
                                                  SESSION,
                                                  new URL("file",
                                                          null,
                                                          fileToStore.toFile().getAbsolutePath()).toString(),
                                                  ONLINE_CONF,
                                                  null));
        }
        Collection<RequestInfo> infos = client.store(files);
        // Wait for storage ends
        waitRequestEnds(2, 60);
        Assert.assertEquals("Two requests should be created", 2, infos.size());
    }

    @Test
    public void storeBulk() throws NoSuchAlgorithmException, IOException, InterruptedException {

        runtimeTenantResolver.forceTenant(getDefaultTenant());

        FileSystemUtils.deleteRecursively(Paths.get("target/store"));
        Files.createDirectory(Paths.get("target/store"));
        int nbGroups = 20;
        Set<Path> filesToStore = Sets.newHashSet();
        for (int i = 0; i < nbGroups; i++) {
            Path path = Paths.get("target/store/file_" + i + ".txt");
            String str = "fichier de test storeBulk" + i;
            byte[] strToBytes = str.getBytes();
            Files.write(path, strToBytes);
            filesToStore.add(path);
        }

        Path fileCommon = Paths.get("target/store/file_common.txt");
        String str = "fichier de test commun";
        byte[] strToBytes = str.getBytes();
        Files.write(fileCommon, strToBytes);
        String csCommon = ChecksumUtils.computeHexChecksum(fileCommon, "MD5");

        int cpt = 0;
        List<String> groupIds = new ArrayList<>();
        // Clear listener if any requests
        listener.reset();
        for (Path file : filesToStore) {
            cpt++;
            String owner = "owner-" + cpt;
            String sessionOwner = "SOURCE " + cpt;
            Set<FileStorageRequestDto> files = Sets.newHashSet();
            String cs = ChecksumUtils.computeHexChecksum(file, "MD5");
            files.add(FileStorageRequestDto.build(file.getFileName().toString(),
                                                  cs,
                                                  "MD5",
                                                  "application/octet-stream",
                                                  owner,
                                                  sessionOwner,
                                                  SESSION,
                                                  (new URL("file", null, file.toAbsolutePath().toString())).toString(),
                                                  ONLINE_CONF,
                                                  null));
            files.add(FileStorageRequestDto.build(fileCommon.getFileName().toString(),
                                                  csCommon,
                                                  "MD5",
                                                  "application/octet-stream",
                                                  owner,
                                                  sessionOwner,
                                                  SESSION,
                                                  (new URL("file",
                                                           null,
                                                           fileCommon.toAbsolutePath().toString())).toString(),
                                                  ONLINE_CONF,
                                                  null));
            groupIds.addAll(client.store(files).stream().map(RequestInfo::getGroupId).toList());
        }

        Assert.assertEquals(nbGroups, groupIds.size());
        waitRequestEnds(groupIds.size(), 30);
        Optional<FileReference> commonFileRef = fileReferenceWithOwnersRepository.findByLocationStorageAndMetaInfoChecksum(
            ONLINE_CONF,
            csCommon);
        Assert.assertTrue(commonFileRef.isPresent());
        Assert.assertEquals(nbGroups, commonFileRef.get().getLazzyOwners().size());
        long nbReqErrors = storageReqRepo.countByStorageAndStatus(ONLINE_CONF, FileRequestStatus.ERROR);
        if (nbReqErrors > 0 || !listener.getErrors().isEmpty()) {
            LOGGER.warn("Request errors detected : {}", nbReqErrors);
            LOGGER.warn("Request groups error events received : {}", listener.getErrors().size());
        }

        Assert.assertTrue("All storage request groups should be done", listener.getSuccess().size() >= nbGroups);
        // Check all requested groups has been done
        Assert.assertTrue("All storage request groups should be done",
                          groupIds.stream()
                                  .allMatch(groupId -> listener.getSuccess()
                                                               .values()
                                                               .stream()
                                                               .anyMatch(r -> Objects.equals(r.getGroupId(),
                                                                                             groupId))));

    }

    @Test
    public void storeFile() throws InterruptedException, MalformedURLException {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        String cs1 = UUID.randomUUID().toString();
        String cs2 = UUID.randomUUID().toString();
        String cs3 = UUID.randomUUID().toString();
        String cs4 = UUID.randomUUID().toString();
        Set<FileStorageRequestDto> files = Sets.newHashSet();
        files.add(FileStorageRequestDto.build("file.test",
                                              cs1,
                                              "UUID",
                                              "application/octet-stream",
                                              "owner",
                                              SESSION_OWNER,
                                              SESSION,
                                              new URL("file", null, fileToStore.toFile().getAbsolutePath()).toString(),
                                              ONLINE_CONF,
                                              null));
        files.add(FileStorageRequestDto.build("file2.test",
                                              cs2,
                                              "UUID",
                                              "application/octet-stream",
                                              "owner",
                                              SESSION_OWNER,
                                              SESSION,
                                              new URL("file", null, fileToStore.toFile().getAbsolutePath()).toString(),
                                              ONLINE_CONF,
                                              null));
        files.add(FileStorageRequestDto.build("restoError.file3.test",
                                              cs3,
                                              "UUID",
                                              "application/octet-stream",
                                              "owner",
                                              SESSION_OWNER,
                                              SESSION,
                                              new URL("file", null, fileToStore.toFile().getAbsolutePath()).toString(),
                                              NEARLINE_CONF,
                                              null));
        files.add(FileStorageRequestDto.build("file4.test",
                                              cs4,
                                              "UUID",
                                              "application/octet-stream",
                                              "owner",
                                              SESSION_OWNER,
                                              SESSION,
                                              new URL("file", null, fileToStore.toFile().getAbsolutePath()).toString(),
                                              NEARLINE_CONF,
                                              null));

        files.add(FileStorageRequestDto.build(AvailabilityUpdateCustomTestAction.FILE_TO_UPDATE_NAME,
                                              AvailabilityUpdateCustomTestAction.FILE_TO_UPDATE_CHECKSUM,
                                              "UUID",
                                              "application/octet-stream",
                                              "owner",
                                              SESSION_OWNER,
                                              SESSION,
                                              new URL("file", null, fileToStore.toFile().getAbsolutePath()).toString(),
                                              NEARLINE_CONF,
                                              null));

        listener.reset();
        Collection<RequestInfo> infos = client.store(files);
        Assert.assertEquals(1, infos.size());
        RequestInfo info = infos.stream().findFirst().get();

        waitRequestEnds(1, 60);
        Assert.assertTrue("Request should be granted", listener.getGranted().contains(info));

        Assert.assertTrue("Request should be successful", listener.getSuccess().containsKey(info));

        Assert.assertEquals("Group request should contains 4 success request",
                            5,
                            listener.getSuccess().get(info).size());
        Assert.assertFalse("Request should not be error", listener.getErrors().containsKey(info));

        storedFileChecksums.add(cs1);
        restorableFileChecksums.add(cs1);
        storedFileChecksums.add(cs2);
        restorableFileChecksums.add(cs2);
        storedFileChecksums.add(cs3);
        unrestorableFileChecksums.add(cs3);
        storedFileChecksums.add(cs4);
        restorableFileChecksums.add(cs4);

        listener.reset();
    }

    @Test
    public void storeError_unknownStorage() throws MalformedURLException, InterruptedException {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        RequestInfo info = client.store(FileStorageRequestDto.build("file.test",
                                                                    UUID.randomUUID().toString(),
                                                                    "UUID",
                                                                    "application/octet-stream",
                                                                    "owner",
                                                                    SESSION_OWNER,
                                                                    SESSION,
                                                                    new URL("file",
                                                                            null,
                                                                            fileToStore.toFile()
                                                                                       .getAbsolutePath()).toString(),
                                                                    "somewhere",
                                                                    null));

        waitRequestEnds(1, 60);
        Assert.assertTrue("Request should be successful", listener.getGranted().contains(info));
        Assert.assertFalse("Request should not be successful", listener.getSuccess().containsKey(info));
        Assert.assertTrue("Request should be error", listener.getErrors().containsKey(info));
        listener.reset();
    }

    @Test
    public void storeError_storagePluginError() throws MalformedURLException, InterruptedException {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        listener.reset();
        RequestInfo info = client.store(FileStorageRequestDto.build("error.file.test",
                                                                    UUID.randomUUID().toString(),
                                                                    "UUID",
                                                                    "application/octet-stream",
                                                                    "owner",
                                                                    SESSION_OWNER,
                                                                    SESSION,
                                                                    new URL("file",
                                                                            null,
                                                                            fileToStore.toFile()
                                                                                       .getAbsolutePath()).toString(),
                                                                    ONLINE_CONF,
                                                                    null));

        waitRequestEnds(1, 60);
        Assert.assertTrue("Request should be successful", listener.getGranted().contains(info));
        Assert.assertFalse("Request should not be successful", listener.getSuccess().containsKey(info));
        Assert.assertTrue("Request should be error", listener.getErrors().containsKey(info));
        listener.reset();
    }

    @Test
    public void storeError_storeSuccessAndError() throws MalformedURLException, InterruptedException {
        // Test a request with one file success and one file error
        Set<FileStorageRequestDto> files = Sets.newHashSet();
        files.add(FileStorageRequestDto.build("error.file.test",
                                              UUID.randomUUID().toString(),
                                              "UUID",
                                              "application/octet-stream",
                                              "owner",
                                              SESSION_OWNER,
                                              SESSION,
                                              new URL("file", null, fileToStore.toFile().getAbsolutePath()).toString(),
                                              ONLINE_CONF,
                                              null));
        files.add(FileStorageRequestDto.build("ok.file.test",
                                              UUID.randomUUID().toString(),
                                              "UUID",
                                              "application/octet-stream",
                                              "owner",
                                              SESSION_OWNER,
                                              SESSION,
                                              new URL("file", null, fileToStore.toFile().getAbsolutePath()).toString(),
                                              ONLINE_CONF,
                                              null));
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        listener.reset();
        Collection<RequestInfo> infos = client.store(files);
        Assert.assertEquals(1, infos.size());
        RequestInfo info = infos.stream().findFirst().get();
        waitRequestEnds(1, 60);
        Assert.assertTrue("Request should be granted", listener.getGranted().contains(info));
        Assert.assertTrue("Request should be successful", listener.getSuccess().containsKey(info));
        Assert.assertEquals("Request should contains 1 success", 1, listener.getSuccess().get(info).size());
        Assert.assertTrue("Request should be error", listener.getErrors().containsKey(info));
        Assert.assertEquals("Request should not be successful", 1, listener.getErrors().get(info).size());
        listener.reset();
    }

    @Test
    public void storeRetry() throws MalformedURLException, InterruptedException {
        Set<FileStorageRequestDto> files = Sets.newHashSet();
        files.add(FileStorageRequestDto.build("error.file.test",
                                              UUID.randomUUID().toString(),
                                              "UUID",
                                              "application/octet-stream",
                                              "owner",
                                              SESSION_OWNER,
                                              SESSION,
                                              new URL("file", null, fileToStore.toFile().getAbsolutePath()).toString(),
                                              ONLINE_CONF,
                                              null));
        files.add(FileStorageRequestDto.build("ok.file.test",
                                              UUID.randomUUID().toString(),
                                              "UUID",
                                              "application/octet-stream",
                                              "owner",
                                              SESSION_OWNER,
                                              SESSION,
                                              new URL("file", null, fileToStore.toFile().getAbsolutePath()).toString(),
                                              ONLINE_CONF,
                                              null));
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        listener.reset();
        Collection<RequestInfo> infos = client.store(files);
        Assert.assertEquals(1, infos.size());
        RequestInfo info = infos.stream().findFirst().get();
        waitRequestEnds(1, 60);
        Assert.assertTrue("Request should be successful", listener.getGranted().contains(info));
        Assert.assertTrue("Request should contains successful storage", listener.getSuccess().containsKey(info));
        Assert.assertEquals("Request should contains 1 successful storage", 1, listener.getSuccess().get(info).size());
        Assert.assertTrue("Request should contains error storage", listener.getErrors().containsKey(info));
        Assert.assertEquals("Request should contains 1 error storage", 1, listener.getErrors().get(info).size());

        listener.reset();

        client.storeRetry(info);

        waitRequestEnds(1, 60);
        Assert.assertFalse("Request should not be successful", listener.getSuccess().containsKey(info));
        Assert.assertTrue("Request should be error", listener.getErrors().containsKey(info));
        Assert.assertEquals("Request should contains 1 error storage", 1, listener.getErrors().get(info).size());

        listener.reset();

    }

    @Test
    public void referenceWithMultipleGroups() throws InterruptedException {
        Set<FileReferenceRequestDto> files = Sets.newHashSet();
        for (int i = 0; i < (FilesReferenceEvent.MAX_REQUEST_PER_GROUP + 1); i++) {
            files.add(FileReferenceRequestDto.build("file1.test",
                                                    UUID.randomUUID().toString(),
                                                    "UUID",
                                                    "application/octet-stream",
                                                    10L,
                                                    "owner",
                                                    "somewhere",
                                                    "file://here/file1.test",
                                                    "source1",
                                                    "session1"));
        }
        listener.reset();
        Collection<RequestInfo> infos = client.reference(files);
        Assert.assertEquals("There should be two requests groups", 2, infos.size());
        waitRequestEnds(2, 60);
        for (RequestInfo info : infos) {
            Assert.assertTrue("Request should be granted", listener.getGranted().contains(info));
        }
        listener.reset();
    }

    private void waitRequestEnds(int nbrequests, int maxDurationSec) throws InterruptedException {
        int loopDuration = 2_000;
        int nbLoop = ((maxDurationSec * 10000) / loopDuration);
        int loop = 0;
        while ((listener.getNbRequestEnds() < nbrequests) && (loop < nbLoop)) {
            loop++;
            Thread.sleep(loopDuration);
        }
        if (listener.getNbRequestEnds() < nbrequests) {
            String message = String.format("Number of requests requested for end not reached %d/%d",
                                           listener.getNbRequestEnds(),
                                           nbrequests);
            Assert.fail(message);
        }
    }

    @Test
    public void referenceFile() throws InterruptedException {
        String owner = "refe-test";
        String sessionOwner = "source1";
        String session = "session1";
        String storage = "somewhere";
        String baseURl = "file://here/it/is/";
        String cs1 = UUID.randomUUID().toString();
        String cs2 = UUID.randomUUID().toString();
        String cs3 = UUID.randomUUID().toString();
        Set<FileReferenceRequestDto> files = Sets.newHashSet();
        files.add(FileReferenceRequestDto.build("file1.test",
                                                cs1,
                                                "UUID",
                                                "application/octet-stream",
                                                10L,
                                                owner,
                                                storage,
                                                baseURl + "file1.test",
                                                sessionOwner,
                                                session));
        files.add(FileReferenceRequestDto.build("file2.test",
                                                cs2,
                                                "UUID",
                                                "application/octet-stream",
                                                10L,
                                                owner,
                                                storage,
                                                baseURl + "file2.test",
                                                sessionOwner,
                                                session));
        files.add(FileReferenceRequestDto.build("file3.test",
                                                cs3,
                                                "UUID",
                                                "application/octet-stream",
                                                10L,
                                                owner,
                                                storage,
                                                baseURl + "file3.test",
                                                sessionOwner,
                                                session));

        listener.reset();
        Collection<RequestInfo> infos = client.reference(files);
        Assert.assertEquals(1, infos.size());
        RequestInfo info = infos.stream().findFirst().get();
        waitRequestEnds(1, 60);
        Assert.assertTrue("Request should be granted", listener.getGranted().contains(info));
        Assert.assertTrue("Request should be successful", listener.getSuccess().containsKey(info));
        Assert.assertFalse("Request should not be error", listener.getErrors().containsKey(info));

        referenceFileChecksums.add(cs1);
        referenceFileChecksums.add(cs2);
        referenceFileChecksums.add(cs3);
        listener.reset();
    }

    @Test
    public void deleteFile() throws MalformedURLException, InterruptedException {

        // Store file
        String checksum = UUID.randomUUID().toString();
        String owner = "delete-test";
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        RequestInfo info = client.store(FileStorageRequestDto.build("ok.file.test",
                                                                    checksum,
                                                                    "UUID",
                                                                    "application/octet-stream",
                                                                    owner,
                                                                    SESSION_OWNER,
                                                                    SESSION,
                                                                    new URL("file",
                                                                            null,
                                                                            fileToStore.toFile()
                                                                                       .getAbsolutePath()).toString(),
                                                                    ONLINE_CONF,
                                                                    null));
        waitRequestEnds(1, 60);

        Assert.assertTrue("Request should be granted", listener.getGranted().contains(info));
        Assert.assertTrue("Request should be successful", listener.getSuccess().containsKey(info));
        Assert.assertFalse("Request should not be error", listener.getErrors().containsKey(info));

        listener.reset();

        // Delete it
        RequestInfo deleteInfo = client.delete(FileDeletionRequestDto.build(checksum,
                                                                            ONLINE_CONF,
                                                                            owner,
                                                                            SESSION_OWNER,
                                                                            SESSION,
                                                                            false));

        waitRequestEnds(1, 60);
        Assert.assertTrue("Request should be granted", listener.getGranted().contains(deleteInfo));
        Assert.assertTrue("Request should be successful", listener.getSuccess().containsKey(deleteInfo));
        Assert.assertFalse("Request should not be error", listener.getErrors().containsKey(deleteInfo));

    }

    @Test
    public void deleteWithMultipleGroups() throws InterruptedException {
        Set<FileDeletionRequestDto> files = Sets.newHashSet();
        for (int i = 0; i < (FilesDeletionEvent.MAX_REQUEST_PER_GROUP + 1); i++) {
            files.add(FileDeletionRequestDto.build(UUID.randomUUID().toString(),
                                                   ONLINE_CONF,
                                                   "owner",
                                                   SESSION_OWNER,
                                                   SESSION,
                                                   false));
        }
        Collection<RequestInfo> infos = client.delete(files);
        Assert.assertEquals("There should be two requests groups", 2, infos.size());
        waitRequestEnds(2, 60);
        for (RequestInfo info : infos) {
            Assert.assertTrue("Request should be granted", listener.getGranted().contains(info));
        }
    }

    @Test
    public void availability() throws MalformedURLException, InterruptedException {

        this.storeFile();
        listener.reset();

        runtimeTenantResolver.forceTenant(getDefaultTenant());
        Collection<RequestInfo> infos = client.makeAvailable(restorableFileChecksums, 24);
        Assert.assertEquals(1, infos.size());
        RequestInfo info = infos.stream().findFirst().get();

        waitRequestEnds(1, 60);
        Assert.assertTrue("Request should be granted", listener.getGranted().contains(info));
        Assert.assertTrue("Request should be successful", listener.getSuccess().containsKey(info));
        Assert.assertFalse("Request should not be error", listener.getErrors().containsKey(info));

    }

    @Test
    public void availabilityWithUpdateOnAvailable() throws MalformedURLException, InterruptedException {

        this.storeFile();
        listener.reset();

        // File to retrieve should exists with default checksum
        Assert.assertTrue("File to retrieve should exists with default checksum",
                          fileRefService.search(NEARLINE_CONF,
                                                AvailabilityUpdateCustomTestAction.FILE_TO_UPDATE_CHECKSUM)
                                        .isPresent());

        runtimeTenantResolver.forceTenant(getDefaultTenant());
        Collection<RequestInfo> infos = client.makeAvailable(Sets.newHashSet(AvailabilityUpdateCustomTestAction.FILE_TO_UPDATE_CHECKSUM),
                                                             24);
        Assert.assertEquals(1, infos.size());
        RequestInfo info = infos.stream().findFirst().get();

        waitRequestEnds(1, 60);

        Assert.assertFalse("File to retrieve should not exists anymore with default checksum",
                           fileRefService.search(NEARLINE_CONF,
                                                 AvailabilityUpdateCustomTestAction.FILE_TO_UPDATE_CHECKSUM)
                                         .isPresent());
        Assert.assertTrue("File to retrieve should exists with updated checksum",
                          fileRefService.search(NEARLINE_CONF,
                                                AvailabilityUpdateCustomTestAction.getUpdatedChecksum(
                                                    AvailabilityUpdateCustomTestAction.FILE_TO_UPDATE_CHECKSUM))
                                        .isPresent());

        // Check that fileRef checksum is updated
        fileRefService.search(NEARLINE_CONF, AvailabilityUpdateCustomTestAction.FILE_TO_UPDATE_CHECKSUM);
        Assert.assertTrue("Request should be granted", listener.getGranted().contains(info));
        Assert.assertTrue("Request should be successful", listener.getSuccess().containsKey(info));
        Assert.assertFalse("Request should not be error", listener.getErrors().containsKey(info));

    }

    @Test
    public void availabilityWithMultipleRequests() throws InterruptedException {
        Set<String> files = Sets.newHashSet();
        for (int i = 0; i < (FilesRestorationRequestEvent.MAX_REQUEST_PER_GROUP + 1); i++) {
            files.add(UUID.randomUUID().toString());
        }

        Collection<RequestInfo> infos = client.makeAvailable(files, 24);
        Assert.assertEquals("There should be two requests groups", 2, infos.size());
        waitRequestEnds(2, 60);
        for (RequestInfo info : infos) {
            Assert.assertTrue("Request should be granted", listener.getGranted().contains(info));
        }
    }

    @Test
    public void availability_offlineFiles() throws MalformedURLException, InterruptedException {

        this.referenceFile();

        listener.reset();

        runtimeTenantResolver.forceTenant(getDefaultTenant());
        Set<String> checksums = Sets.newHashSet();
        checksums.addAll(referenceFileChecksums);
        Collection<RequestInfo> infos = client.makeAvailable(checksums, 24);
        Assert.assertEquals(1, infos.size());
        RequestInfo info = infos.stream().findFirst().get();

        waitRequestEnds(1, 60);
        Assert.assertTrue("Request should be granted", listener.getGranted().contains(info));
        Assert.assertFalse("Request should not be successful", listener.getSuccess().containsKey(info));
        Assert.assertTrue("Request should be error", listener.getErrors().containsKey(info));
        Assert.assertEquals("Number of error invalid",
                            referenceFileChecksums.size(),
                            listener.getErrors().get(info).size());
        for (String checksum : checksums) {
            Assert.assertTrue("Missing error checksum",
                              listener.getErrors()
                                      .get(info)
                                      .stream()
                                      .anyMatch(e -> e.getRequestChecksum().equals(checksum)));
        }

    }

    @Test
    public void availability_offlineFilesAndRestoError() throws MalformedURLException, InterruptedException {

        this.storeFile();

        listener.reset();

        this.referenceFile();

        listener.reset();

        runtimeTenantResolver.forceTenant(getDefaultTenant());
        Set<String> checksums = Sets.newHashSet();
        checksums.addAll(referenceFileChecksums);
        checksums.addAll(storedFileChecksums);
        int nbSuccessExpected = restorableFileChecksums.size();
        int nbErrorExpected = unrestorableFileChecksums.size() + referenceFileChecksums.size();
        Collection<RequestInfo> infos = client.makeAvailable(checksums, 24);
        Assert.assertEquals(1, infos.size());
        RequestInfo info = infos.stream().findFirst().get();

        waitRequestEnds(1, 60);
        Assert.assertTrue("Request should be granted", listener.getGranted().contains(info));
        Assert.assertTrue("Request should contains successful requests ", listener.getSuccess().containsKey(info));
        Assert.assertEquals("Request should contains successful requests ",
                            nbSuccessExpected,
                            listener.getSuccess().get(info).size());
        Assert.assertTrue("Request should contains error requests", listener.getErrors().containsKey(info));
        Assert.assertEquals("Request should contains error requests",
                            nbErrorExpected,
                            listener.getErrors().get(info).size());

        for (String checksum : referenceFileChecksums) {
            Assert.assertTrue("Missing error checksum",
                              listener.getErrors()
                                      .get(info)
                                      .stream()
                                      .anyMatch(e -> e.getRequestChecksum().equals(checksum)));
        }

        for (String checksum : unrestorableFileChecksums) {
            Assert.assertTrue("Missing error checksum",
                              listener.getErrors()
                                      .get(info)
                                      .stream()
                                      .anyMatch(e -> e.getRequestChecksum().equals(checksum)));
        }
    }

    @Test
    public void copy() throws MalformedURLException, InterruptedException {

        this.storeFile();
        listener.reset();
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        Set<FileCopyRequestDto> requests = restorableFileChecksums.stream()
                                                                  .map(f -> FileCopyRequestDto.build(f,
                                                                                                     NEARLINE_CONF_2,
                                                                                                     SESSION_OWNER,
                                                                                                     SESSION))
                                                                  .collect(Collectors.toSet());
        Collection<RequestInfo> infos = client.copy(requests);
        Assert.assertEquals(1, infos.size());
        RequestInfo info = infos.stream().findFirst().get();
        // 1 Copy group requests should be over
        // 1 Availability requests should be over (created by the copy process)
        // 1 Storage group by file. Each group is created after availability event for each file.
        waitRequestEnds(1 + 1 + restorableFileChecksums.size(), 30);
        Assert.assertTrue("Request should be granted", listener.getGranted().contains(info));
        Assert.assertTrue(String.format("Request should be successful for request id %s", info.getGroupId()),
                          listener.getSuccess().containsKey(info));
        Assert.assertFalse("Request should not be error", listener.getErrors().containsKey(info));
    }

    @Test
    public void copyWithAvailableUpdate() throws MalformedURLException, InterruptedException {
        this.storeFile();
        listener.reset();

        // File to retrieve should exists with default checksum
        Assert.assertTrue("File to retrieve should exists with default checksum",
                          fileRefService.search(NEARLINE_CONF,
                                                AvailabilityUpdateCustomTestAction.FILE_TO_UPDATE_CHECKSUM)
                                        .isPresent());

        runtimeTenantResolver.forceTenant(getDefaultTenant());

        Set<FileCopyRequestDto> requests = Sets.newHashSet(FileCopyRequestDto.build(AvailabilityUpdateCustomTestAction.FILE_TO_UPDATE_CHECKSUM,
                                                                                    ONLINE_CONF,
                                                                                    SESSION_OWNER,
                                                                                    SESSION));
        Collection<RequestInfo> infos = client.copy(requests);
        Assert.assertEquals(1, infos.size());
        RequestInfo info = infos.stream().findFirst().get();
        LOGGER.info("[TEST COPY] Running copy group request {} with {} requests", info.getGroupId(), requests.size());
        // 1 Copy request
        // 1 Availability request
        // 1 Storage request
        waitRequestEnds(3, 40);

        Assert.assertTrue("Request group should be granted", listener.getGranted().contains(info));
    }

    @Test
    public void copy_withError() throws MalformedURLException, InterruptedException {

        this.storeFile();
        listener.reset();
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        Set<FileCopyRequestDto> requests = storedFileChecksums.stream()
                                                              .map(f -> FileCopyRequestDto.build(f,
                                                                                                 NEARLINE_CONF_2,
                                                                                                 SESSION_OWNER,
                                                                                                 SESSION))
                                                              .collect(Collectors.toSet());
        Collection<RequestInfo> infos = client.copy(requests);
        Assert.assertEquals(1, infos.size());
        RequestInfo info = infos.stream().findFirst().get();
        LOGGER.info("[TEST COPY] Running copy group request {} with {} requests", info.getGroupId(), requests.size());
        // 1 Copy request
        // 1 Availability request
        // X Storage request
        waitRequestEnds(1 + 1 + storedFileChecksums.size(), 20);

        Assert.assertTrue("Request group should be granted", listener.getGranted().contains(info));
        Assert.assertTrue(String.format("Request group %s should contains 3 successful request", info.getGroupId()),
                          listener.getSuccess().containsKey(info));
        Assert.assertEquals(String.format("Request group %s should contains 3 successful request", info.getGroupId()),
                            restorableFileChecksums.size(),
                            listener.getSuccess().get(info).size());
        Assert.assertTrue("Request group should be in error", listener.getErrors().containsKey(info));
        Assert.assertEquals(String.format("Request group %s should contains 1 error request", info.getGroupId()),
                            unrestorableFileChecksums.size(),
                            listener.getErrors().get(info).size());
        restorableFileChecksums.forEach(f -> {
            Assert.assertTrue("Missing a sucess file",
                              listener.getSuccess().get(info).stream().anyMatch(e -> e.getRequestChecksum().equals(f)));
        });
        unrestorableFileChecksums.forEach(f -> {
            Assert.assertTrue("Missing an error file",
                              listener.getErrors().get(info).stream().anyMatch(e -> e.getRequestChecksum().equals(f)));
        });
    }

    private StorageLocationConfiguration initDataStoragePluginConfiguration() {
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

    private StorageLocationConfiguration initDataStorageNLPluginConfiguration(String name, String storageDirectory)
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

}
