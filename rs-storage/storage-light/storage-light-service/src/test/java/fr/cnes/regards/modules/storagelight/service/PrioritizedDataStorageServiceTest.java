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
package fr.cnes.regards.modules.storagelight.service;

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
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.storagelight.domain.database.PrioritizedDataStorage;
import fr.cnes.regards.modules.storagelight.domain.plugin.DataStorageType;
import fr.cnes.regards.modules.storagelight.service.plugin.SimpleOnlineDataStorage;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_test",
        "regards.storage.cache.path=target/cache", "regards.storage.cache.minimum.time.to.live.hours=12" })
@ActiveProfiles({ "disableStorageTasks", "noschdule" })
public class PrioritizedDataStorageServiceTest extends AbstractMultitenantServiceTest {

    private static final String PDS_LABEL = "PrioritizedDataStorageServiceIT";

    private final String targetPath = "target/PrioritizedDataStorageServiceIT";

    @Autowired
    private PrioritizedDataStorageService prioritizedDataStorageService;

    @Autowired
    private IPluginService pluginService;

    @Before
    public void init() {
        prioritizedDataStorageService.findAllByType(DataStorageType.ONLINE).forEach(c -> {
            try {
                prioritizedDataStorageService.delete(c.getId());
            } catch (ModuleException e) {
                Assert.fail(e.getMessage());
            }
        });
    }

    @Test
    public void testDelete() throws ModuleException, IOException, URISyntaxException {
        PrioritizedDataStorage pds = createPrioritizedDataStorage(PDS_LABEL);
        prioritizedDataStorageService.delete(pds.getId());
        // lets check that the plugin configuration has been deleted too
        Optional<PluginConfiguration> optConf = pluginService.findPluginConfigurationByLabel(PDS_LABEL);
        Assert.assertFalse("Prioritized data storage deletion did not deleted corresponding plugin configuration",
                           optConf.isPresent());
    }

    @Test
    public void testUpdate() throws ModuleException, IOException, URISyntaxException {
        String label = "updateConf label";
        PrioritizedDataStorage pds = createPrioritizedDataStorage(label);
        PluginConfiguration updatedConf = getPluginConf(label);
        updatedConf.setId(pds.getDataStorageConfiguration().getId());
        PrioritizedDataStorage upds = new PrioritizedDataStorage(updatedConf, 0L, DataStorageType.ONLINE);
        upds.setId(pds.getId());
        prioritizedDataStorageService.update(upds.getId(), upds);
    }

    @Test(expected = EntityOperationForbiddenException.class)
    public void testUpdateForbidden() throws ModuleException, IOException, URISyntaxException {
        String label = "updateConf label";

        URL newbaseStorageLocation = new URL("file", "",
                Paths.get(targetPath, "/update/conf").toFile().getAbsolutePath());

        PrioritizedDataStorage pds = createPrioritizedDataStorage(label);
        PluginConfiguration updatedConf = getPluginConf(label);
        updatedConf.getParameter(SimpleOnlineDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME)
                .setValue(newbaseStorageLocation.toString());
        updatedConf.setId(pds.getDataStorageConfiguration().getId());
        PrioritizedDataStorage upds = new PrioritizedDataStorage(updatedConf, 0L, DataStorageType.ONLINE);
        upds.setId(pds.getId());
        prioritizedDataStorageService.update(upds.getId(), upds);
    }

    private PluginConfiguration getPluginConf(String label) throws IOException, URISyntaxException {
        URL baseStorageLocation = new URL("file", "", Paths.get(targetPath).toFile().getAbsolutePath());

        PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(SimpleOnlineDataStorage.class);
        Files.createDirectories(Paths.get(baseStorageLocation.toURI()));
        Set<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(SimpleOnlineDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                              baseStorageLocation.toString())
                .addParameter(SimpleOnlineDataStorage.HANDLE_STORAGE_ERROR_FILE_PATTERN, "error.*").getParameters();
        return new PluginConfiguration(dataStoMeta, label, parameters, 0);
    }

    private PrioritizedDataStorage createPrioritizedDataStorage(String label)
            throws IOException, URISyntaxException, ModuleException {

        PluginConfiguration dataStorageConf = getPluginConf(label);
        return prioritizedDataStorageService.create(dataStorageConf);
    }
}
