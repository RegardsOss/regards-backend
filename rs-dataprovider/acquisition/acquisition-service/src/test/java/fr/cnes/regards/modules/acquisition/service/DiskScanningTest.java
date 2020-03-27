/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.utils.plugins.PluginParameterTransformer;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
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

    @Before
    public void setup() {
        PluginUtils.setup();
    }

    @Test
    public void testDirectoryScanningWithoutGlobber()
            throws ModuleException, IOException, NotAvailablePluginConfigurationException {

        // Plugin parameters
        Set<IPluginParam> parameters = IPluginParam
                .set(IPluginParam.build(GlobDiskScanning.FIELD_DIRS,
                                        PluginParameterTransformer.toJson(Arrays.asList(searchDir.toString()))));

        PluginConfiguration pluginConf = PluginConfiguration.build(GlobDiskScanning.class, null, parameters);
        // Instantiate plugin
        IScanPlugin plugin = PluginUtils.getPlugin(pluginConf, new HashMap<String, Object>());
        Assert.assertNotNull(plugin);

        // Run plugin
        List<Path> scannedFiles = plugin.scan(Optional.empty());
        Assert.assertNotNull(scannedFiles);
        Assert.assertTrue(scannedFiles.size() == 4);

        Collections.sort(scannedFiles, (file1, file2) -> {
            try {
                return Files.getLastModifiedTime(file1).compareTo(Files.getLastModifiedTime(file2));
            } catch (IOException e) {
                return 0;
            }
        });

        // Scan from first LMD
        OffsetDateTime lmd = OffsetDateTime.ofInstant(Files.getLastModifiedTime(scannedFiles.get(0)).toInstant(),
                                                      ZoneOffset.UTC);
        scannedFiles = plugin.scan(Optional.of(lmd));
        // File in same second are selected
        Assert.assertTrue(scannedFiles.size() == 4);
    }

    @Test
    public void testDirectoryScanningWithGlobber() throws ModuleException, NotAvailablePluginConfigurationException {

        // Plugin parameters
        Set<IPluginParam> parameters = IPluginParam.set(
                                                        IPluginParam.build(GlobDiskScanning.FIELD_DIRS,
                                                                           PluginParameterTransformer.toJson(Arrays
                                                                                   .asList(searchDir.toString()))),
                                                        IPluginParam.build(GlobDiskScanning.FIELD_GLOB, "*_0[12].md"));

        PluginConfiguration pluginConf = PluginConfiguration.build(GlobDiskScanning.class, null, parameters);
        // Instantiate plugin
        IScanPlugin plugin = PluginUtils.getPlugin(pluginConf, new HashMap<String, Object>());
        Assert.assertNotNull(plugin);

        // Run plugin
        List<Path> scannedFiles = plugin.scan(Optional.empty());
        Assert.assertNotNull(scannedFiles);
        Assert.assertTrue(scannedFiles.size() == 2);
    }

    @Test
    public void testDirectoryScanningWithoutRegex() throws ModuleException, NotAvailablePluginConfigurationException {

        // Plugin parameters
        Set<IPluginParam> parameters = IPluginParam
                .set(IPluginParam.build(RegexDiskScanning.FIELD_DIRS,
                                        PluginParameterTransformer.toJson(Arrays.asList(searchDir.toString()))));

        PluginConfiguration pluginConf = PluginConfiguration.build(RegexDiskScanning.class, null, parameters);
        // Instantiate plugin
        IScanPlugin plugin = PluginUtils.getPlugin(pluginConf, new HashMap<String, Object>());
        Assert.assertNotNull(plugin);

        // Run plugin
        List<Path> scannedFiles = plugin.scan(Optional.empty());
        Assert.assertNotNull(scannedFiles);
        Assert.assertTrue(scannedFiles.size() == 4);
    }

    @Test
    public void testDirectoryScanningWithRegex() throws ModuleException, NotAvailablePluginConfigurationException {

        // Plugin parameters
        Set<IPluginParam> parameters = IPluginParam.set(
                                                        IPluginParam.build(RegexDiskScanning.FIELD_DIRS,
                                                                           PluginParameterTransformer.toJson(Arrays
                                                                                   .asList(searchDir.toString()))),
                                                        IPluginParam.build(RegexDiskScanning.FIELD_REGEX,
                                                                           ".*_0[12]\\.md"));

        PluginConfiguration pluginConf = PluginConfiguration.build(RegexDiskScanning.class, null, parameters);
        // Instantiate plugin
        IScanPlugin plugin = PluginUtils.getPlugin(pluginConf, new HashMap<String, Object>());
        Assert.assertNotNull(plugin);

        // Run plugin
        List<Path> scannedFiles = plugin.scan(Optional.empty());
        Assert.assertNotNull(scannedFiles);
        Assert.assertTrue(scannedFiles.size() == 2);
    }
}
