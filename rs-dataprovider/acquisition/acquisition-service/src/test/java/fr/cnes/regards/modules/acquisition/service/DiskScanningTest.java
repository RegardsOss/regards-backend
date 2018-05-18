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
package fr.cnes.regards.modules.acquisition.service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.acquisition.plugins.IScanPlugin;
import fr.cnes.regards.modules.acquisition.service.plugins.GlobDiskScanning;
import fr.cnes.regards.modules.acquisition.service.plugins.RegexDiskScanning;

/**
 * Test scanning plugin
 *
 * @author Marc Sordi
 *
 */
public class DiskScanningTest {

    private final Path searchDir = Paths.get("src", "test", "resources", "data", "plugins", "scan");

    @Test
    public void testDirectoryScanningWithoutGlobber() throws ModuleException {

        // Plugin parameters
        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(GlobDiskScanning.FIELD_DIRS, Arrays.asList(searchDir.toString())).getParameters();

        // Plugin and plugin interface packages
        List<String> prefixes = Arrays.asList(IScanPlugin.class.getPackage().getName(),
                                              GlobDiskScanning.class.getPackage().getName());

        // Instantiate plugin
        IScanPlugin plugin = PluginUtils.getPlugin(parameters, GlobDiskScanning.class, prefixes, new HashMap<>());
        Assert.assertNotNull(plugin);

        // Run plugin
        List<Path> scannedFiles = plugin.scan(Optional.empty());
        Assert.assertNotNull(scannedFiles);
        Assert.assertTrue(scannedFiles.size() == 4);
    }

    @Test
    public void testDirectoryScanningWithGlobber() throws ModuleException {

        // Plugin parameters
        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(GlobDiskScanning.FIELD_DIRS, Arrays.asList(searchDir.toString()))
                .addParameter(GlobDiskScanning.FIELD_GLOB, "*_0[12].md").getParameters();

        // Plugin and plugin interface packages
        List<String> prefixes = Arrays.asList(IScanPlugin.class.getPackage().getName(),
                                              GlobDiskScanning.class.getPackage().getName());

        // Instantiate plugin
        IScanPlugin plugin = PluginUtils.getPlugin(parameters, GlobDiskScanning.class, prefixes, new HashMap<>());
        Assert.assertNotNull(plugin);

        // Run plugin
        List<Path> scannedFiles = plugin.scan(Optional.empty());
        Assert.assertNotNull(scannedFiles);
        Assert.assertTrue(scannedFiles.size() == 2);
    }

    @Test
    public void testDirectoryScanningWithoutRegex() throws ModuleException {

        // Plugin parameters
        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(RegexDiskScanning.FIELD_DIRS, Arrays.asList(searchDir.toString())).getParameters();

        // Plugin and plugin interface packages
        List<String> prefixes = Arrays.asList(IScanPlugin.class.getPackage().getName(),
                                              RegexDiskScanning.class.getPackage().getName());

        // Instantiate plugin
        IScanPlugin plugin = PluginUtils.getPlugin(parameters, RegexDiskScanning.class, prefixes, new HashMap<>());
        Assert.assertNotNull(plugin);

        // Run plugin
        List<Path> scannedFiles = plugin.scan(Optional.empty());
        Assert.assertNotNull(scannedFiles);
        Assert.assertTrue(scannedFiles.size() == 4);
    }

    @Test
    public void testDirectoryScanningWithRegex() throws ModuleException {

        // Plugin parameters
        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(RegexDiskScanning.FIELD_DIRS, Arrays.asList(searchDir.toString()))
                .addParameter(RegexDiskScanning.FIELD_REGEX, ".*_0[12]\\.md").getParameters();

        // Plugin and plugin interface packages
        List<String> prefixes = Arrays.asList(IScanPlugin.class.getPackage().getName(),
                                              RegexDiskScanning.class.getPackage().getName());

        // Instantiate plugin
        IScanPlugin plugin = PluginUtils.getPlugin(parameters, RegexDiskScanning.class, prefixes, new HashMap<>());
        Assert.assertNotNull(plugin);

        // Run plugin
        List<Path> scannedFiles = plugin.scan(Optional.empty());
        Assert.assertNotNull(scannedFiles);
        Assert.assertTrue(scannedFiles.size() == 2);
    }
}
