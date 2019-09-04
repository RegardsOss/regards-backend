/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storagelight.client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.storagelight.domain.database.PrioritizedStorage;
import fr.cnes.regards.modules.storagelight.domain.dto.FileCopyRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.dto.FileDeletionRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.dto.FileReferenceRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.dto.FileStorageRequestDTO;
import fr.cnes.regards.modules.storagelight.service.location.PrioritizedStorageService;
import fr.cnes.regards.modules.storagelight.service.plugin.SimpleNearlineDataStorage;
import fr.cnes.regards.modules.storagelight.service.plugin.SimpleOnlineTestClient;

/**
 * @author sbinda
 *
 */
@ActiveProfiles({ "testAmqp", "storageTest" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_client_tests",
        "regards.storage.cache.path=target/cache", "regards.amqp.enabled=true" })
public class StorageClientTest extends AbstractMultitenantServiceTest {

    @Autowired
    private StorageListener listener;

    @Autowired
    private StorageClient client;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private PrioritizedStorageService prioritizedDataStorageService;

    private Path fileToStore;

    private static final String ONLINE_CONF = "ONLINE_CONF";

    private static final String NEARLINE_CONF = "NEARLINE_CONF";

    private static final String NEARLINE_CONF_2 = "NEARLINE_CONF_2";

    private final Set<String> storedFileChecksums = Sets.newHashSet();

    private final Set<String> restorableFileChecksums = Sets.newHashSet();

    private final Set<String> unrestorableFileChecksums = Sets.newHashSet();

    private final Set<String> referenceFileChecksums = Sets.newHashSet();

    @Before
    public void init() throws IOException, ModuleException {
        simulateApplicationReadyEvent();
        fileToStore = Paths.get("target/file-to-store.test");
        if (!Files.exists(fileToStore)) {
            Files.createFile(fileToStore);
        }
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        if (!prioritizedDataStorageService.search(ONLINE_CONF).isPresent()) {
            initDataStoragePluginConfiguration();
        }
        if (!prioritizedDataStorageService.search(NEARLINE_CONF).isPresent()) {
            initDataStorageNLPluginConfiguration(NEARLINE_CONF, "target/nearline-storage-1");
        }
        if (!prioritizedDataStorageService.search(NEARLINE_CONF_2).isPresent()) {
            initDataStorageNLPluginConfiguration(NEARLINE_CONF_2, "target/nearline-storage-2");
        }

        Assert.assertTrue(prioritizedDataStorageService.search(ONLINE_CONF).isPresent());
        Assert.assertTrue(prioritizedDataStorageService.search(NEARLINE_CONF).isPresent());
        Assert.assertTrue(prioritizedDataStorageService.search(NEARLINE_CONF_2).isPresent());
    }

    @Test
    public void storeFile() throws InterruptedException, MalformedURLException {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        String cs1 = UUID.randomUUID().toString();
        String cs2 = UUID.randomUUID().toString();
        String cs3 = UUID.randomUUID().toString();
        String cs4 = UUID.randomUUID().toString();
        Set<FileStorageRequestDTO> files = Sets.newHashSet();
        files.add(FileStorageRequestDTO.build("file.test", cs1, "UUID", "application/octet-stream", "owner",
                                              new URL("file", null, fileToStore.toFile().getAbsolutePath()),
                                              ONLINE_CONF, null));
        files.add(FileStorageRequestDTO.build("file2.test", cs2, "UUID", "application/octet-stream", "owner",
                                              new URL("file", null, fileToStore.toFile().getAbsolutePath()),
                                              ONLINE_CONF, null));
        files.add(FileStorageRequestDTO.build("restoError.file3.test", cs3, "UUID", "application/octet-stream", "owner",
                                              new URL("file", null, fileToStore.toFile().getAbsolutePath()),
                                              NEARLINE_CONF, null));
        files.add(FileStorageRequestDTO.build("file4.test", cs4, "UUID", "application/octet-stream", "owner",
                                              new URL("file", null, fileToStore.toFile().getAbsolutePath()),
                                              NEARLINE_CONF, null));

        RequestInfo info = client.store(files);

        Thread.sleep(5_000);
        Assert.assertTrue("Request should be granted", listener.getGranted().contains(info));
        Assert.assertTrue("Request should be successful", listener.getSuccess().containsKey(info));
        Assert.assertFalse("Request should not be error", listener.getErrors().containsKey(info));

        storedFileChecksums.add(cs1);
        restorableFileChecksums.add(cs1);
        storedFileChecksums.add(cs2);
        restorableFileChecksums.add(cs2);
        storedFileChecksums.add(cs3);
        unrestorableFileChecksums.add(cs3);
        storedFileChecksums.add(cs4);
        restorableFileChecksums.add(cs4);

    }

    @Test
    public void storeError_unknownStorage() throws MalformedURLException, InterruptedException {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        RequestInfo info = client.store(FileStorageRequestDTO
                .build("file.test", UUID.randomUUID().toString(), "UUID", "application/octet-stream", "owner",
                       new URL("file", null, fileToStore.toFile().getAbsolutePath()), "somewhere", null));

        Thread.sleep(5_000);
        Assert.assertTrue("Request should be successful", listener.getGranted().contains(info));
        Assert.assertFalse("Request should not be successful", listener.getSuccess().containsKey(info));
        Assert.assertTrue("Request should be error", listener.getErrors().containsKey(info));
    }

    @Test
    public void storeError_storagePluginError() throws MalformedURLException, InterruptedException {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        RequestInfo info = client.store(FileStorageRequestDTO
                .build("error.file.test", UUID.randomUUID().toString(), "UUID", "application/octet-stream", "owner",
                       new URL("file", null, fileToStore.toFile().getAbsolutePath()), ONLINE_CONF, null));

        Thread.sleep(5_000);
        Assert.assertTrue("Request should be successful", listener.getGranted().contains(info));
        Assert.assertFalse("Request should not be successful", listener.getSuccess().containsKey(info));
        Assert.assertTrue("Request should be error", listener.getErrors().containsKey(info));
    }

    @Test
    public void storeError_storeSuccessAndError() throws MalformedURLException, InterruptedException {
        // Test a request with one file success and one file error
        Set<FileStorageRequestDTO> files = Sets.newHashSet();
        files.add(FileStorageRequestDTO
                .build("error.file.test", UUID.randomUUID().toString(), "UUID", "application/octet-stream", "owner",
                       new URL("file", null, fileToStore.toFile().getAbsolutePath()), ONLINE_CONF, null));
        files.add(FileStorageRequestDTO
                .build("ok.file.test", UUID.randomUUID().toString(), "UUID", "application/octet-stream", "owner",
                       new URL("file", null, fileToStore.toFile().getAbsolutePath()), ONLINE_CONF, null));
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        RequestInfo info = client.store(files);
        Thread.sleep(5_000);
        Assert.assertTrue("Request should be successful", listener.getGranted().contains(info));
        Assert.assertFalse("Request should not be successful", listener.getSuccess().containsKey(info));
        Assert.assertTrue("Request should be error", listener.getErrors().containsKey(info));
    }

    @Test
    public void storeRetry() throws MalformedURLException, InterruptedException {
        Set<FileStorageRequestDTO> files = Sets.newHashSet();
        files.add(FileStorageRequestDTO
                .build("error.file.test", UUID.randomUUID().toString(), "UUID", "application/octet-stream", "owner",
                       new URL("file", null, fileToStore.toFile().getAbsolutePath()), ONLINE_CONF, null));
        files.add(FileStorageRequestDTO
                .build("ok.file.test", UUID.randomUUID().toString(), "UUID", "application/octet-stream", "owner",
                       new URL("file", null, fileToStore.toFile().getAbsolutePath()), ONLINE_CONF, null));
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        RequestInfo info = client.store(files);
        Thread.sleep(5_000);
        Assert.assertTrue("Request should be successful", listener.getGranted().contains(info));
        Assert.assertFalse("Request should not be successful", listener.getSuccess().containsKey(info));
        Assert.assertTrue("Request should be error", listener.getErrors().containsKey(info));

        listener.reset();

        client.storeRetry(info);

        Thread.sleep(5_000);
        Assert.assertFalse("Request should not be successful", listener.getSuccess().containsKey(info));
        Assert.assertTrue("Request should be error", listener.getErrors().containsKey(info));

    }

    @Test
    public void referenceFile() throws InterruptedException {
        String owner = "refe-test";
        String storage = "somewhere";
        String baseURl = "file://here/it/is/";
        String cs1 = UUID.randomUUID().toString();
        String cs2 = UUID.randomUUID().toString();
        String cs3 = UUID.randomUUID().toString();
        Set<FileReferenceRequestDTO> files = Sets.newHashSet();
        files.add(FileReferenceRequestDTO.build("file1.test", cs1, "UUID", "application/octet-stream", 10L, owner,
                                                storage, baseURl + "file1.test"));
        files.add(FileReferenceRequestDTO.build("file2.test", cs2, "UUID", "application/octet-stream", 10L, owner,
                                                storage, baseURl + "file2.test"));
        files.add(FileReferenceRequestDTO.build("file3.test", cs3, "UUID", "application/octet-stream", 10L, owner,
                                                storage, baseURl + "file3.test"));

        RequestInfo info = client.reference(files);
        Thread.sleep(5_000);
        Assert.assertTrue("Request should be granted", listener.getGranted().contains(info));
        Assert.assertTrue("Request should be successful", listener.getSuccess().containsKey(info));
        Assert.assertFalse("Request should not be error", listener.getErrors().containsKey(info));

        referenceFileChecksums.add(cs1);
        referenceFileChecksums.add(cs2);
        referenceFileChecksums.add(cs3);
    }

    @Test
    public void deleteFile() throws MalformedURLException, InterruptedException {

        // Store file
        String checksum = UUID.randomUUID().toString();
        String owner = "delete-test";
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        RequestInfo info = client.store(FileStorageRequestDTO
                .build("ok.file.test", checksum, "UUID", "application/octet-stream", owner,
                       new URL("file", null, fileToStore.toFile().getAbsolutePath()), ONLINE_CONF, null));
        Thread.sleep(5_000);

        Assert.assertTrue("Request should be granted", listener.getGranted().contains(info));
        Assert.assertTrue("Request should be successful", listener.getSuccess().containsKey(info));
        Assert.assertFalse("Request should not be error", listener.getErrors().containsKey(info));

        listener.reset();

        // Delete it
        RequestInfo deleteInfo = client.delete(FileDeletionRequestDTO.build(checksum, ONLINE_CONF, owner, false));

        Thread.sleep(5_000);
        Assert.assertTrue("Request should be granted", listener.getGranted().contains(deleteInfo));
        Assert.assertTrue("Request should be successful", listener.getSuccess().containsKey(deleteInfo));
        Assert.assertFalse("Request should not be error", listener.getErrors().containsKey(deleteInfo));

    }

    @Test
    public void availability() throws MalformedURLException, InterruptedException {

        this.storeFile();

        runtimeTenantResolver.forceTenant(getDefaultTenant());
        RequestInfo info = client.makeAvailable(restorableFileChecksums, OffsetDateTime.now().plusDays(1));

        Thread.sleep(5_000);
        Assert.assertTrue("Request should be granted", listener.getGranted().contains(info));
        Assert.assertTrue("Request should be successful", listener.getSuccess().containsKey(info));
        Assert.assertFalse("Request should not be error", listener.getErrors().containsKey(info));

    }

    @Test
    public void availability_restorationError() throws MalformedURLException, InterruptedException {

        this.storeFile();

        listener.reset();

        this.referenceFile();

        listener.reset();

        runtimeTenantResolver.forceTenant(getDefaultTenant());
        Set<String> checksums = Sets.newHashSet();
        checksums.addAll(storedFileChecksums);
        checksums.addAll(referenceFileChecksums);
        RequestInfo info = client.makeAvailable(storedFileChecksums, OffsetDateTime.now().plusDays(1));

        Thread.sleep(5_000);
        Assert.assertTrue("Request should be granted", listener.getGranted().contains(info));
        Assert.assertFalse("Request should not be successful", listener.getSuccess().containsKey(info));
        Assert.assertTrue("Request should be error", listener.getErrors().containsKey(info));

    }

    @Test
    public void availability_offlineFiles() throws MalformedURLException, InterruptedException {

        this.referenceFile();

        listener.reset();

        runtimeTenantResolver.forceTenant(getDefaultTenant());
        Set<String> checksums = Sets.newHashSet();
        checksums.addAll(referenceFileChecksums);
        RequestInfo info = client.makeAvailable(checksums, OffsetDateTime.now().plusDays(1));

        Thread.sleep(5_000);
        Assert.assertTrue("Request should be granted", listener.getGranted().contains(info));
        Assert.assertFalse("Request should not be successful", listener.getSuccess().containsKey(info));
        Assert.assertTrue("Request should be error", listener.getErrors().containsKey(info));
        Assert.assertEquals("Number of error invalid", referenceFileChecksums.size(),
                            listener.getErrors().get(info).size());
        for (String checksum : checksums) {
            Assert.assertTrue("Missing error checksum",
                              listener.getErrors().get(info).stream().anyMatch(e -> e.getChecksum().equals(checksum)));
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
        RequestInfo info = client.makeAvailable(checksums, OffsetDateTime.now().plusDays(1));

        Thread.sleep(5_000);
        Assert.assertTrue("Request should be granted", listener.getGranted().contains(info));
        Assert.assertFalse("Request should not be successful", listener.getSuccess().containsKey(info));
        Assert.assertTrue("Request should be error", listener.getErrors().containsKey(info));

        Assert.assertEquals("Number of error invalid", referenceFileChecksums.size() + unrestorableFileChecksums.size(),
                            listener.getErrors().get(info).size());
        for (String checksum : referenceFileChecksums) {
            Assert.assertTrue("Missing error checksum",
                              listener.getErrors().get(info).stream().anyMatch(e -> e.getChecksum().equals(checksum)));
        }

        for (String checksum : unrestorableFileChecksums) {
            Assert.assertTrue("Missing error checksum",
                              listener.getErrors().get(info).stream().anyMatch(e -> e.getChecksum().equals(checksum)));
        }
    }

    @Test
    public void copy() throws MalformedURLException, InterruptedException {

        this.storeFile();
        listener.reset();
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        Set<FileCopyRequestDTO> requests = restorableFileChecksums.stream()
                .map(f -> FileCopyRequestDTO.build(f, NEARLINE_CONF_2)).collect(Collectors.toSet());
        RequestInfo info = client.copy(requests);
        Thread.sleep(15_000);

        Assert.assertTrue("Request should be granted", listener.getGranted().contains(info));
        Assert.assertTrue(String.format("Request should be successful for request id %s", info.getGroupId()),
                          listener.getSuccess().containsKey(info));
        Assert.assertFalse("Request should not be error", listener.getErrors().containsKey(info));
    }

    @Test
    public void copy_withError() throws MalformedURLException, InterruptedException {

        this.storeFile();
        listener.reset();
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        Set<FileCopyRequestDTO> requests = storedFileChecksums.stream()
                .map(f -> FileCopyRequestDTO.build(f, NEARLINE_CONF_2)).collect(Collectors.toSet());
        RequestInfo info = client.copy(requests);
        Thread.sleep(15_000);

        Assert.assertTrue("Request should be granted", listener.getGranted().contains(info));
        Assert.assertFalse(String.format("Request should be successful for request id %s", info.getGroupId()),
                           listener.getSuccess().containsKey(info));
        Assert.assertTrue("Request should not be error", listener.getErrors().containsKey(info));
        Assert.assertEquals("Number of error files invalid", unrestorableFileChecksums.size(),
                            listener.getErrors().get(info).size());
        unrestorableFileChecksums.forEach(f -> {
            Assert.assertTrue("Missing an error file",
                              listener.getErrors().get(info).stream().anyMatch(e -> e.getChecksum().equals(f)));
        });
    }

    private PrioritizedStorage initDataStoragePluginConfiguration() {
        try {
            PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(SimpleOnlineTestClient.class);
            Files.createDirectories(Paths.get("target/online-storage/"));

            Set<IPluginParam> parameters = IPluginParam
                    .set(IPluginParam.build(SimpleOnlineTestClient.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                                            "target/online-storage/"),
                         IPluginParam.build(SimpleOnlineTestClient.HANDLE_STORAGE_ERROR_FILE_PATTERN, "error.*"),
                         IPluginParam.build(SimpleOnlineTestClient.HANDLE_DELETE_ERROR_FILE_PATTERN, "delErr.*"));
            PluginConfiguration dataStorageConf = new PluginConfiguration(dataStoMeta, ONLINE_CONF, parameters, 0);
            dataStorageConf.setIsActive(true);
            return prioritizedDataStorageService.create(dataStorageConf);
        } catch (IOException | ModuleException e) {
            Assert.fail(e.getMessage());
            return null;
        }
    }

    private PrioritizedStorage initDataStorageNLPluginConfiguration(String label, String storageDirectory)
            throws ModuleException {
        try {
            PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(SimpleNearlineDataStorage.class);
            Files.createDirectories(Paths.get(storageDirectory));
            Set<IPluginParam> parameters = IPluginParam
                    .set(IPluginParam.build(SimpleNearlineDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                                            storageDirectory),
                         IPluginParam.build(SimpleNearlineDataStorage.HANDLE_STORAGE_ERROR_FILE_PATTERN, "error.*"),
                         IPluginParam.build(SimpleNearlineDataStorage.HANDLE_RESTORATION_ERROR_FILE_PATTERN,
                                            "restoError.*"),
                         IPluginParam.build(SimpleNearlineDataStorage.HANDLE_DELETE_ERROR_FILE_PATTERN, "delErr.*"));
            PluginConfiguration dataStorageConf = new PluginConfiguration(dataStoMeta, label, parameters, 0);
            dataStorageConf.setIsActive(true);
            return prioritizedDataStorageService.create(dataStorageConf);
        } catch (IOException e) {
            throw new ModuleException(e.getMessage(), e);
        }
    }

}
