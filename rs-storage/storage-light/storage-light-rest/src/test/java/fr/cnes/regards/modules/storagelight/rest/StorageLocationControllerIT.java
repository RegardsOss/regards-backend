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
package fr.cnes.regards.modules.storagelight.rest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.storagelight.domain.event.FileRequestType;
import fr.cnes.regards.modules.storagelight.domain.plugin.StorageType;
import fr.cnes.regards.modules.storagelight.rest.plugin.SimpleOnlineDataStorage;
import fr.cnes.regards.modules.storagelight.service.location.StorageLocationConfigurationService;

/**
 * @author sbinda
 *
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_loc_rest_it",
        "regards.storage.cache.path=target/cache" })
public class StorageLocationControllerIT extends AbstractRegardsTransactionalIT {

    private static final String TARGET_STORAGE = "target";

    private static final String STORAGE_PATH = "target/ONLINE-STORAGE";

    @Autowired
    private StorageLocationConfigurationService prioritizedDataStorageService;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    private void clear() throws IOException {
        prioritizedDataStorageService.search(StorageType.ONLINE).forEach(c -> {
            try {
                prioritizedDataStorageService.delete(c.getId());
            } catch (ModuleException e) {
                Assert.fail(e.getMessage());
            }
        });
        if (Files.exists(Paths.get("target/storage"))) {
            FileUtils.deleteDirectory(Paths.get(STORAGE_PATH).toFile());
        }
    }

    @Before
    public void init()
            throws NoSuchAlgorithmException, FileNotFoundException, IOException, InterruptedException, ModuleException {
        tenantResolver.forceTenant(getDefaultTenant());
        clear();
        initDataStoragePluginConfiguration();
    }

    @Test
    public void retreiveAll() {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        performDefaultGet(StorageLocationController.BASE_PATH, requestBuilderCustomizer, "Expect ok status.");
    }

    @Test
    public void retreiveOne() {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        performDefaultGet(StorageLocationController.BASE_PATH + StorageLocationController.ID_PATH,
                          requestBuilderCustomizer, "Expect ok status.", TARGET_STORAGE);
    }

    @Test
    public void retryErrors() {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        performDefaultGet(StorageLocationController.BASE_PATH + StorageLocationController.ID_PATH
                + StorageLocationController.FILES + StorageLocationController.RETRY, requestBuilderCustomizer,
                          "Expect ok status.", TARGET_STORAGE, FileRequestType.STORAGE.toString());
    }

    @Test
    public void copyFiles() {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        requestBuilderCustomizer.addParameter(StorageLocationController.PATH_COPY_PARAM, "/dir/one");
        requestBuilderCustomizer.addParameter(StorageLocationController.COPY_LOCATION_DEST_PARAM, "somewhere");
        performDefaultGet(StorageLocationController.BASE_PATH + StorageLocationController.ID_PATH
                + StorageLocationController.FILES + StorageLocationController.COPY, requestBuilderCustomizer,
                          "Expect ok status.", TARGET_STORAGE);
    }

    @Test
    public void delete() {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        performDefaultDelete(StorageLocationController.BASE_PATH + StorageLocationController.ID_PATH,
                             requestBuilderCustomizer, "Expect ok status.", TARGET_STORAGE);
    }

    @Test
    public void deleteFiles() {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        performDefaultDelete(StorageLocationController.BASE_PATH + StorageLocationController.ID_PATH
                + StorageLocationController.FILES, requestBuilderCustomizer, "Expect ok status.", TARGET_STORAGE);
    }

    @Test
    public void increasePriority() {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        performDefaultPut(StorageLocationController.BASE_PATH + StorageLocationController.UP_PATH, null,
                          requestBuilderCustomizer, "Expect ok status.", TARGET_STORAGE);
    }

    @Test
    public void decreasePriority() {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        performDefaultPut(StorageLocationController.BASE_PATH + StorageLocationController.DOWN_PATH, null,
                          requestBuilderCustomizer, "Expect ok status.", TARGET_STORAGE);
    }

    private void initDataStoragePluginConfiguration() throws ModuleException {
        try {
            PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(SimpleOnlineDataStorage.class);
            Files.createDirectories(Paths.get(STORAGE_PATH));

            Set<IPluginParam> parameters = IPluginParam
                    .set(IPluginParam.build(SimpleOnlineDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                                            STORAGE_PATH),
                         IPluginParam.build(SimpleOnlineDataStorage.HANDLE_STORAGE_ERROR_FILE_PATTERN, "error.*"),
                         IPluginParam.build(SimpleOnlineDataStorage.HANDLE_DELETE_ERROR_FILE_PATTERN, "delErr.*"));
            PluginConfiguration dataStorageConf = new PluginConfiguration(dataStoMeta, TARGET_STORAGE, parameters, 0);
            prioritizedDataStorageService.create(TARGET_STORAGE, dataStorageConf, 1_000_000L);
        } catch (IOException e) {
            throw new ModuleException(e.getMessage(), e);
        }
    }

}
