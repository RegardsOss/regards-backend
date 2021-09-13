/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.service.location;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.storage.dao.IFileReferenceRepository;
import fr.cnes.regards.modules.storage.dao.IFileStorageRequestRepository;
import fr.cnes.regards.modules.storage.dao.IGroupRequestInfoRepository;
import fr.cnes.regards.modules.storage.domain.database.StorageLocationConfiguration;
import fr.cnes.regards.modules.storage.domain.plugin.StorageType;
import fr.cnes.regards.modules.storage.service.plugin.SimpleNearlineDataStorage;
import fr.cnes.regards.modules.storage.service.plugin.SimpleOnlineDataStorage;

/**
 * @author Sébastien Binda
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_tests"},
        locations = { "classpath:application-test.properties" })
@ActiveProfiles({ "noscheduler" })
public class StorageLocationConfigurationServiceTest extends AbstractMultitenantServiceTest {

    private static final String PDS_LABEL = "PrioritizedDataStorageServiceIT";

    private final String targetPath = "target/PrioritizedDataStorageServiceIT";

    @Autowired
    private StorageLocationConfigurationService storageLocationConfService;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IFileReferenceRepository fileRefRepo;

    @Autowired
    private IGroupRequestInfoRepository requInfoRepo;

    @Autowired
    private IFileStorageRequestRepository fileRefRequestRepo;

    @Autowired
    private IJobInfoRepository jobInfoRepo;

    @Before
    public void init() {
        requInfoRepo.deleteAll();
        fileRefRepo.deleteAll();
        fileRefRequestRepo.deleteAll();
        jobInfoRepo.deleteAll();
        storageLocationConfService.search(StorageType.ONLINE).forEach(c -> {
            try {
                storageLocationConfService.delete(c.getId());
            } catch (ModuleException e) {
                Assert.fail(e.getMessage());
            }
        });
    }

    @Test
    public void testDelete() throws ModuleException, IOException, URISyntaxException {
        StorageLocationConfiguration pds = createStorageLocationConf(PDS_LABEL);
        storageLocationConfService.delete(pds.getId());
        // lets check that the plugin configuration has been deleted too
        Optional<PluginConfiguration> optConf = pluginService.findPluginConfigurationByLabel(PDS_LABEL);
        Assert.assertFalse("Prioritized data storage deletion did not deleted corresponding plugin configuration",
                           optConf.isPresent());
    }

    @Test
    public void testUpdate() throws ModuleException, IOException, URISyntaxException {
        String name = "updateConf";
        StorageLocationConfiguration pds = createStorageLocationConf(name);
        pds.setAllocatedSizeInKo(0L);
        StorageLocationConfiguration updated = storageLocationConfService.update(name, pds);
        Assert.assertEquals(0L, updated.getAllocatedSizeInKo().longValue());
    }

    @Test(expected = EntityInvalidException.class)
    public void testUpdateWithNewPluginType() throws ModuleException, IOException, URISyntaxException {
        String name = "updateConf";
        StorageLocationConfiguration pds = createStorageLocationConf(name);
        pds.setPluginConfiguration(getNearlineOnlinePluginConf(pds.getName(), pds.getPluginConfiguration().getBusinessId()));
        storageLocationConfService.update(name, pds);
    }

    private PluginConfiguration getNearlineOnlinePluginConf(String label, String businessId) throws IOException, URISyntaxException {
        URL baseStorageLocation = new URL("file", "", Paths.get(targetPath).toFile().getAbsolutePath());

        PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(SimpleNearlineDataStorage.class);
        Files.createDirectories(Paths.get(baseStorageLocation.toURI()));
        Set<IPluginParam> parameters = IPluginParam
                .set(IPluginParam.build(SimpleOnlineDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                                        baseStorageLocation.toString()),
                     IPluginParam.build(SimpleOnlineDataStorage.HANDLE_STORAGE_ERROR_FILE_PATTERN, "error.*"),
                     IPluginParam.build(SimpleOnlineDataStorage.HANDLE_DELETE_ERROR_FILE_PATTERN, "delErr.*"));
        PluginConfiguration pluginConfiguration = new PluginConfiguration(label, parameters, 0, dataStoMeta.getPluginId());
        pluginConfiguration.setBusinessId(businessId);
        return pluginConfiguration;
    }

    private PluginConfiguration getSimpleOnlinePluginConf(String label) throws IOException, URISyntaxException {
        URL baseStorageLocation = new URL("file", "", Paths.get(targetPath).toFile().getAbsolutePath());

        PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(SimpleOnlineDataStorage.class);
        Files.createDirectories(Paths.get(baseStorageLocation.toURI()));
        Set<IPluginParam> parameters = IPluginParam
                .set(IPluginParam.build(SimpleNearlineDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                                        baseStorageLocation.toString()),
                     IPluginParam.build(SimpleNearlineDataStorage.HANDLE_STORAGE_ERROR_FILE_PATTERN, "error.*"),
                     IPluginParam.build(SimpleNearlineDataStorage.HANDLE_RESTORATION_ERROR_FILE_PATTERN, "restErr.*"),
                     IPluginParam.build(SimpleNearlineDataStorage.HANDLE_DELETE_ERROR_FILE_PATTERN, "delErr.*"));
        return new PluginConfiguration(label, parameters, 0, dataStoMeta.getPluginId());
    }

    private StorageLocationConfiguration createStorageLocationConf(String name)
            throws IOException, URISyntaxException, ModuleException {
        PluginConfiguration dataStorageConf = getSimpleOnlinePluginConf(name);
        return storageLocationConfService.create(name, dataStorageConf, 1_000_000L);
    }
}
