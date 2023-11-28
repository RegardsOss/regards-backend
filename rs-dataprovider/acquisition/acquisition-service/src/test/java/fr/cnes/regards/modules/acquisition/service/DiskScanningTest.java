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
package fr.cnes.regards.modules.acquisition.service;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.IPluginParam;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.acquisition.plugins.IScanPlugin;
import fr.cnes.regards.modules.acquisition.service.plugins.GlobDiskScanning;
import fr.cnes.regards.modules.acquisition.service.plugins.RegexDiskScanning;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test scanning plugin
 *
 * @author Marc Sordi
 */
public class DiskScanningTest {

    private final Path searchDir = Paths.get("src", "test", "resources", "data", "plugins", "scan");

    @Before
    public void setup() {
        PluginUtils.setup();
    }

    @Test
    public void testDirectoryScanningWithoutGlobber() throws NotAvailablePluginConfigurationException {

        PluginConfiguration pluginConf = PluginConfiguration.build(GlobDiskScanning.class, null, null);
        // Instantiate plugin
        IScanPlugin plugin = PluginUtils.getPlugin(pluginConf, new ConcurrentHashMap<>());
        Assert.assertNotNull(plugin);

        // Run plugin
        List<Path> scannedFiles = plugin.scan(searchDir, (Optional.empty()));
        Assert.assertNotNull(scannedFiles);
        Assert.assertEquals(4, scannedFiles.size());
    }

    @Test
    public void testDirectoryScanningWithGlobber() throws NotAvailablePluginConfigurationException {

        // Plugin parameters
        Set<IPluginParam> parameters = IPluginParam.set(IPluginParam.build(GlobDiskScanning.FIELD_GLOB, "*_0[12].md"));

        PluginConfiguration pluginConf = PluginConfiguration.build(GlobDiskScanning.class, null, parameters);
        // Instantiate plugin
        IScanPlugin plugin = PluginUtils.getPlugin(pluginConf, new ConcurrentHashMap<>());
        Assert.assertNotNull(plugin);

        // Run plugin
        List<Path> scannedFiles = plugin.scan(searchDir, Optional.empty());
        Assert.assertNotNull(scannedFiles);
        Assert.assertEquals(2, scannedFiles.size());
    }

    @Test
    public void testDirectoryScanningWithoutRegex() throws NotAvailablePluginConfigurationException {

        PluginConfiguration pluginConf = PluginConfiguration.build(RegexDiskScanning.class, null, null);
        // Instantiate plugin
        IScanPlugin plugin = PluginUtils.getPlugin(pluginConf, new ConcurrentHashMap<>());
        Assert.assertNotNull(plugin);

        // Run plugin
        List<Path> scannedFiles = plugin.scan(searchDir, Optional.empty());
        Assert.assertNotNull(scannedFiles);
        Assert.assertEquals(4, scannedFiles.size());
    }

    @Test
    public void testDirectoryScanningWithRegex() throws NotAvailablePluginConfigurationException {

        // Plugin parameters
        Set<IPluginParam> parameters = IPluginParam.set(IPluginParam.build(RegexDiskScanning.FIELD_REGEX,
                                                                           ".*_0[12]\\.md"));

        PluginConfiguration pluginConf = PluginConfiguration.build(RegexDiskScanning.class, null, parameters);
        // Instantiate plugin
        IScanPlugin plugin = PluginUtils.getPlugin(pluginConf, new ConcurrentHashMap<>());
        Assert.assertNotNull(plugin);

        // Run plugin
        List<Path> scannedFiles = plugin.scan(searchDir, Optional.empty());
        Assert.assertNotNull(scannedFiles);
        Assert.assertEquals(2, scannedFiles.size());
    }
}
