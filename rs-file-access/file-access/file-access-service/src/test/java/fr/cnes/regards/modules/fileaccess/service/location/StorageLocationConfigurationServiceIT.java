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
package fr.cnes.regards.modules.fileaccess.service.location;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.dto.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.fileaccess.dao.IStorageLocationConfigurationRepository;
import fr.cnes.regards.modules.fileaccess.domain.StorageLocationConfiguration;
import fr.cnes.regards.modules.fileaccess.service.StorageLocationConfigurationService;
import fr.cnes.regards.modules.fileaccess.service.plugin.SimpleNearlineDataStorage;
import fr.cnes.regards.modules.fileaccess.service.plugin.TestSimpleOnlineDataStorage;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;

/**
 * @author Sébastien Binda
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_location_config_service_it" },
                    locations = { "classpath:application-test.properties" })
@ActiveProfiles({ "noscheduler", "nojobs" })
public class StorageLocationConfigurationServiceIT extends AbstractMultitenantServiceIT {

    private static final String DEFAULT_STORAGE_NAME = "default-storage";

    private static final String BASE_STORAGE_LOCATION = "target/storage-location-config-it";

    @Autowired
    private IPluginConfigurationRepository pluginConfigRepository;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private StorageLocationConfigurationService storageLocationConfigService;

    @Autowired
    private IStorageLocationConfigurationRepository storageLocationConfigRepository;

    @Before
    public void init() {
        pluginConfigRepository.deleteAll();
        storageLocationConfigRepository.deleteAll();
    }

    @Test
    public void testDelete() throws ModuleException, IOException, URISyntaxException {
        // GIVEN
        StorageLocationConfiguration pds = createStorageLocationConf(DEFAULT_STORAGE_NAME);
        // WHEN
        storageLocationConfigService.delete(pds.getName());
        // THEN
        // lets check that the plugin configuration has been deleted too
        Optional<PluginConfiguration> optConf = pluginService.findPluginConfigurationByLabel(DEFAULT_STORAGE_NAME);
        Assertions.assertFalse(optConf.isPresent(),
                               "Prioritized data storage deletion did not deleted corresponding plugin "
                               + "configuration");
    }

    @Test
    public void testUpdate() throws ModuleException, IOException, URISyntaxException {
        // GIVEN
        String name = "updateConf";
        StorageLocationConfiguration storageLocationConfig = createStorageLocationConf(name);
        // WHEN
        storageLocationConfig.setAllocatedSizeInKo(0L);
        StorageLocationConfiguration storageLocationConfigUpdated = storageLocationConfigService.update(name,
                                                                                                        storageLocationConfig);
        // THEN
        Assertions.assertEquals(0L, storageLocationConfigUpdated.getAllocatedSizeInKo().longValue());
    }

    @Test
    public void testUpdateWithNewPluginType() throws ModuleException, IOException, URISyntaxException {
        // GIVEN
        String name = "updateConf";
        StorageLocationConfiguration storageLocationConfig = createStorageLocationConf(name);
        PluginConfiguration updatedPluginConfig = getNearlineOnlinePluginConf(storageLocationConfig.getName(),
                                                                              storageLocationConfig.getPluginConfiguration()
                                                                                                   .getBusinessId());
        storageLocationConfig.setPluginConfiguration(updatedPluginConfig);
        // WHEN / THEN
        // Storage location plugin cannot be updated
        Assertions.assertThrows(EntityInvalidException.class,
                                () -> storageLocationConfigService.update(name, storageLocationConfig));
    }

    private PluginConfiguration getNearlineOnlinePluginConf(String label, String businessId)
        throws IOException, URISyntaxException {
        URL baseStorageLocation = new URL("file", "", Paths.get(BASE_STORAGE_LOCATION).toFile().getAbsolutePath());

        PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(SimpleNearlineDataStorage.class);
        Files.createDirectories(Paths.get(baseStorageLocation.toURI()));
        Set<IPluginParam> parameters = IPluginParam.set(IPluginParam.build(TestSimpleOnlineDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                                                                           baseStorageLocation.toString()),
                                                        IPluginParam.build(TestSimpleOnlineDataStorage.HANDLE_STORAGE_ERROR_FILE_PATTERN,
                                                                           "error.*"),
                                                        IPluginParam.build(TestSimpleOnlineDataStorage.HANDLE_DELETE_ERROR_FILE_PATTERN,
                                                                           "delErr.*"));
        PluginConfiguration pluginConfiguration = new PluginConfiguration(label,
                                                                          parameters,
                                                                          0,
                                                                          dataStoMeta.getPluginId());
        pluginConfiguration.setBusinessId(businessId);
        return pluginConfiguration;
    }

    private PluginConfiguration getSimpleOnlinePluginConf(String label) throws IOException, URISyntaxException {
        URL baseStorageLocation = new URL("file", "", Paths.get(BASE_STORAGE_LOCATION).toFile().getAbsolutePath());

        PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(TestSimpleOnlineDataStorage.class);
        Files.createDirectories(Paths.get(baseStorageLocation.toURI()));
        Set<IPluginParam> parameters = IPluginParam.set(IPluginParam.build(SimpleNearlineDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                                                                           baseStorageLocation.toString()),
                                                        IPluginParam.build(SimpleNearlineDataStorage.HANDLE_STORAGE_ERROR_FILE_PATTERN,
                                                                           "error.*"),
                                                        IPluginParam.build(SimpleNearlineDataStorage.HANDLE_RESTORATION_ERROR_FILE_PATTERN,
                                                                           "restErr.*"),
                                                        IPluginParam.build(SimpleNearlineDataStorage.HANDLE_DELETE_ERROR_FILE_PATTERN,
                                                                           "delErr.*"));
        return new PluginConfiguration(label, parameters, 0, dataStoMeta.getPluginId());
    }

    private StorageLocationConfiguration createStorageLocationConf(String name)
        throws IOException, URISyntaxException, ModuleException {
        PluginConfiguration dataStorageConf = getSimpleOnlinePluginConf(name);
        return storageLocationConfigService.create(name, dataStorageConf, 1_000_000L);
    }
}
