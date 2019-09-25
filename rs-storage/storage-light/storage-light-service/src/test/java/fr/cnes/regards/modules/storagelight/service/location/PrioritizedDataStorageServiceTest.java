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
package fr.cnes.regards.modules.storagelight.service.location;

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
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.storagelight.dao.IFileReferenceRepository;
import fr.cnes.regards.modules.storagelight.dao.IFileStorageRequestRepository;
import fr.cnes.regards.modules.storagelight.domain.database.StorageLocationConfiguration;
import fr.cnes.regards.modules.storagelight.domain.plugin.StorageType;
import fr.cnes.regards.modules.storagelight.service.plugin.SimpleOnlineDataStorage;

/**
 * @author Sébastien Binda
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_tests",
        "regards.storage.cache.path=target/cache" })
@ActiveProfiles({ "noscheduler" })
public class PrioritizedDataStorageServiceTest extends AbstractMultitenantServiceTest {

    private static final String PDS_LABEL = "PrioritizedDataStorageServiceIT";

    private final String targetPath = "target/PrioritizedDataStorageServiceIT";

    @Autowired
    private StorageLocationConfigurationService prioritizedDataStorageService;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IFileReferenceRepository fileRefRepo;

    @Autowired
    private IFileStorageRequestRepository fileRefRequestRepo;

    @Autowired
    private IJobInfoRepository jobInfoRepo;

    @Before
    public void init() {
        fileRefRepo.deleteAll();
        fileRefRequestRepo.deleteAll();
        jobInfoRepo.deleteAll();
        prioritizedDataStorageService.search(StorageType.ONLINE).forEach(c -> {
            try {
                prioritizedDataStorageService.delete(c.getId());
            } catch (ModuleException e) {
                Assert.fail(e.getMessage());
            }
        });
    }

    @Test
    public void testDelete() throws ModuleException, IOException, URISyntaxException {
        StorageLocationConfiguration pds = createPrioritizedDataStorage(PDS_LABEL);
        prioritizedDataStorageService.delete(pds.getId());
        // lets check that the plugin configuration has been deleted too
        Optional<PluginConfiguration> optConf = pluginService.findPluginConfigurationByLabel(PDS_LABEL);
        Assert.assertFalse("Prioritized data storage deletion did not deleted corresponding plugin configuration",
                           optConf.isPresent());
    }

    @Test
    public void testUpdate() throws ModuleException, IOException, URISyntaxException {
        String label = "updateConf label";
        StorageLocationConfiguration pds = createPrioritizedDataStorage(label);
        PluginConfiguration updatedConf = getPluginConf(label);
        updatedConf.setId(pds.getPluginConfiguration().getId());
        updatedConf.setBusinessId(pds.getPluginConfiguration().getBusinessId());
        StorageLocationConfiguration upds = new StorageLocationConfiguration(updatedConf, 0L, 1_000_000L,
                StorageType.ONLINE);
        upds.setId(pds.getId());
        prioritizedDataStorageService.update(upds.getId(), upds);
    }

    @Test(expected = EntityOperationForbiddenException.class)
    public void testUpdateForbidden() throws ModuleException, IOException, URISyntaxException {
        String label = "updateConf label";

        URL newbaseStorageLocation = new URL("file", "",
                Paths.get(targetPath, "/update/conf").toFile().getAbsolutePath());

        StorageLocationConfiguration pds = createPrioritizedDataStorage(label);

        PluginConfiguration updatedConf = getPluginConf(label);
        updatedConf.setBusinessId(pds.getPluginConfiguration().getBusinessId());
        updatedConf.setId(pds.getPluginConfiguration().getId());
        updatedConf.getParameter(SimpleOnlineDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME)
                .value(newbaseStorageLocation.toString());

        StorageLocationConfiguration upds = new StorageLocationConfiguration(updatedConf, 0L, 1_000_000L,
                StorageType.ONLINE);
        upds.setId(pds.getId());
        prioritizedDataStorageService.update(upds.getId(), upds);
    }

    private PluginConfiguration getPluginConf(String label) throws IOException, URISyntaxException {
        URL baseStorageLocation = new URL("file", "", Paths.get(targetPath).toFile().getAbsolutePath());

        PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(SimpleOnlineDataStorage.class);
        Files.createDirectories(Paths.get(baseStorageLocation.toURI()));
        Set<IPluginParam> parameters = IPluginParam
                .set(IPluginParam.build(SimpleOnlineDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                                        baseStorageLocation.toString()),
                     IPluginParam.build(SimpleOnlineDataStorage.HANDLE_STORAGE_ERROR_FILE_PATTERN, "error.*"),
                     IPluginParam.build(SimpleOnlineDataStorage.HANDLE_DELETE_ERROR_FILE_PATTERN, "delErr.*"));
        return new PluginConfiguration(dataStoMeta, label, parameters, 0);
    }

    private StorageLocationConfiguration createPrioritizedDataStorage(String label)
            throws IOException, URISyntaxException, ModuleException {
        PluginConfiguration dataStorageConf = getPluginConf(label);
        return prioritizedDataStorageService.create(dataStorageConf, 1_000_000L);
    }
}
