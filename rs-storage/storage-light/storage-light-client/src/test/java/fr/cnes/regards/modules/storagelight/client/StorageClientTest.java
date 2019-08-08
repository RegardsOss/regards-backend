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
import java.util.Set;
import java.util.UUID;

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
import fr.cnes.regards.modules.storagelight.domain.dto.FileReferenceRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.dto.FileStorageRequestDTO;
import fr.cnes.regards.modules.storagelight.service.storage.PrioritizedStorageService;

/**
 * @author sbinda
 *
 */
@ActiveProfiles("testAmqp")
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_client_tests",
        "regards.storage.cache.path=target/cache", "regards.amqp.enabled=true" })
public class StorageClientTest extends AbstractMultitenantServiceTest {

    @Autowired
    private StorageListener listener;

    @Autowired
    private StorageClient client;

    @Autowired
    IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private PrioritizedStorageService prioritizedDataStorageService;

    private Path fileToStore;

    private final String ONLINE_CONF = "ONLINE_CONF";

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

        Assert.assertTrue(prioritizedDataStorageService.search(ONLINE_CONF).isPresent());
    }

    @Test
    public void storeFile() throws InterruptedException, MalformedURLException {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        RequestInfo info = client.reference(FileReferenceRequestDTO
                .build("file.test", UUID.randomUUID().toString(), "UUID", "application/octet-stream", 10L, "owner",
                       "somewhere", "somewhere://in/here/file.test"));
        Thread.sleep(2_000);
        Assert.assertTrue("Request should be successful", listener.getGranted().contains(info));
        Assert.assertTrue("Request should be successful", listener.getSuccess().contains(info));
        Assert.assertFalse("Request should not be error", listener.getErrors().containsKey(info));

        info = client.store(FileStorageRequestDTO
                .build("file.test", UUID.randomUUID().toString(), "UUID", "application/octet-stream", "owner",
                       new URL("file", null, fileToStore.toFile().getAbsolutePath()), ONLINE_CONF, null));

        Thread.sleep(5_000);
        Assert.assertTrue("Request should be successful", listener.getGranted().contains(info));
        Assert.assertTrue("Request should be successful", listener.getSuccess().contains(info));
        Assert.assertFalse("Request should not be error", listener.getErrors().containsKey(info));
    }

    @Test
    public void storeError_unknownStorage() throws MalformedURLException, InterruptedException {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        RequestInfo info = client.store(FileStorageRequestDTO
                .build("file.test", UUID.randomUUID().toString(), "UUID", "application/octet-stream", "owner",
                       new URL("file", null, fileToStore.toFile().getAbsolutePath()), "somewhere", null));

        Thread.sleep(5_000);
        Assert.assertTrue("Request should be successful", listener.getGranted().contains(info));
        Assert.assertFalse("Request should not be successful", listener.getSuccess().contains(info));
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
        Assert.assertFalse("Request should not be successful", listener.getSuccess().contains(info));
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
        Thread.sleep(10_000);
        Assert.assertTrue("Request should be successful", listener.getGranted().contains(info));
        Assert.assertFalse("Request should not be successful", listener.getSuccess().contains(info));
        Assert.assertTrue("Request should be error", listener.getErrors().containsKey(info));
    }

    private PrioritizedStorage initDataStoragePluginConfiguration() {
        try {
            PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(SimpleOnlineDataStorage.class);
            Files.createDirectories(Paths.get("target/online-storage/"));

            Set<IPluginParam> parameters = IPluginParam
                    .set(IPluginParam.build(SimpleOnlineDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                                            "target/online-storage/"),
                         IPluginParam.build(SimpleOnlineDataStorage.HANDLE_STORAGE_ERROR_FILE_PATTERN, "error.*"),
                         IPluginParam.build(SimpleOnlineDataStorage.HANDLE_DELETE_ERROR_FILE_PATTERN, "delErr.*"));
            PluginConfiguration dataStorageConf = new PluginConfiguration(dataStoMeta, ONLINE_CONF, parameters, 0);
            dataStorageConf.setIsActive(true);
            return prioritizedDataStorageService.create(dataStorageConf);
        } catch (IOException | ModuleException e) {
            Assert.fail(e.getMessage());
            return null;
        }
    }

}
