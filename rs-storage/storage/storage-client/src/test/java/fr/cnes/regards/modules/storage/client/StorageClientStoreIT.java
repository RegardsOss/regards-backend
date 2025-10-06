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
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestStatus;
import fr.cnes.regards.modules.fileaccess.dto.request.FileStorageRequestDto;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesStorageRequestEvent;
import fr.cnes.regards.modules.filecatalog.client.RequestInfo;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Test for method StorageClient#store
 *
 * @author sbinda
 * @author onavarro
 */
public class StorageClientStoreIT extends AbstractStorageClientIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageClientStoreIT.class);

    @Test
    public void storeWithMultipleRequests() throws MalformedURLException, InterruptedException {
        Set<FileStorageRequestDto> files = Sets.newHashSet();
        for (int i = 0; i < (FilesStorageRequestEvent.MAX_REQUEST_PER_GROUP + 1); i++) {
            files.add(newFileStorageRequestDto("file.test", ONLINE_CONF));
        }
        Collection<RequestInfo> infos = client.store(files);
        // Wait for storage ends
        waitRequestEnds(2);
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
                                                  MediaType.APPLICATION_OCTET_STREAM_VALUE,
                                                  owner,
                                                  sessionOwner,
                                                  SESSION,
                                                  (new URL("file", null, file.toAbsolutePath().toString())).toString(),
                                                  ONLINE_CONF,
                                                  null));
            files.add(FileStorageRequestDto.build(fileCommon.getFileName().toString(),
                                                  csCommon,
                                                  "MD5",
                                                  MediaType.APPLICATION_OCTET_STREAM_VALUE,
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
    @SneakyThrows
    public void storeFile() {
        super.storeFile();
    }

    @Test
    public void storeError_unknownStorage() {
        // GIVEN a file storage request in an unkown storage
        final FileStorageRequestDto file = newFileStorageRequestDto("file.test", "somewhere");

        // WHEN requesting to store
        final RequestInfo info = client.store(file);
        waitRequestEnds(1);

        // THEN expect the request to be granted but in error
        Assert.assertTrue("Request should be successful", listener.getGranted().contains(info));
        Assert.assertFalse("Request should not be successful", listener.getSuccess().containsKey(info));
        Assert.assertTrue("Request should be error", listener.getErrors().containsKey(info));
    }

    @Test
    public void storeError_storagePluginError() {
        // GIVEN a file storage request with a file in error
        final FileStorageRequestDto file = newFileStorageRequestDto("error.file.test", ONLINE_CONF);

        // WHEN requesting to store
        RequestInfo info = client.store(file);
        waitRequestEnds(1);

        // THEN expect the request to be granted but in error
        Assert.assertTrue("Request should be successful", listener.getGranted().contains(info));
        Assert.assertFalse("Request should not be successful", listener.getSuccess().containsKey(info));
        Assert.assertTrue("Request should be error", listener.getErrors().containsKey(info));
    }

    @Test
    public void storeError_storeSuccessAndError() throws MalformedURLException, InterruptedException {
        // GIVEN 2 file storage request, one with a file in error and the other file is ok
        final Set<FileStorageRequestDto> files = Set.of(newFileStorageRequestDto("error.file.test", ONLINE_CONF),
                                                        newFileStorageRequestDto("ok.file.test", ONLINE_CONF));

        // WHEN requesting to store
        final Collection<RequestInfo> infos = client.store(files);
        Assert.assertEquals(1, infos.size());
        final RequestInfo info = infos.iterator().next();
        waitRequestEnds(1);

        // THEN expect request group to be granted but one request is successful whereas the other is in error
        Assert.assertTrue("Request should be granted", listener.getGranted().contains(info));
        Assert.assertTrue("Request should contains successful storage", listener.getSuccess().containsKey(info));
        Assert.assertEquals("Request should contains 1 success", 1, listener.getSuccess().get(info).size());
        Assert.assertTrue("Request should contains errors", listener.getErrors().containsKey(info));
        Assert.assertEquals("Request should contains 1 error", 1, listener.getErrors().get(info).size());
    }

    @Test
    public void storeRetry() {
        // GIVEN

        // see storeError_storeSuccessAndError

        // 2 file storage request, one with a file in error and the other file is ok
        final Set<FileStorageRequestDto> files = Set.of(newFileStorageRequestDto("error.file.test", ONLINE_CONF),
                                                        newFileStorageRequestDto("ok.file.test", ONLINE_CONF));

        // request to store
        Collection<RequestInfo> infos = client.store(files);
        Assert.assertEquals(1, infos.size());
        RequestInfo info = infos.iterator().next();
        waitRequestEnds(1);

        // assuming 1 success and 1 error
        Assert.assertTrue("Request should be granted", listener.getGranted().contains(info));
        Assert.assertTrue("Request should contains successful storage", listener.getSuccess().containsKey(info));
        Assert.assertEquals("Request should contains 1 success", 1, listener.getSuccess().get(info).size());
        Assert.assertTrue("Request should contains errors", listener.getErrors().containsKey(info));
        Assert.assertEquals("Request should contains 1 error", 1, listener.getErrors().get(info).size());

        listener.reset();

        // WHEN retrying to store
        client.storeRetry(info);
        waitRequestEnds(1);

        // THEN still expect the request in error to be in error again
        Assert.assertFalse("Request should not be successful", listener.getSuccess().containsKey(info));
        Assert.assertTrue("Request should be error", listener.getErrors().containsKey(info));
        Assert.assertEquals("Request should contains 1 error storage", 1, listener.getErrors().get(info).size());
    }

}
