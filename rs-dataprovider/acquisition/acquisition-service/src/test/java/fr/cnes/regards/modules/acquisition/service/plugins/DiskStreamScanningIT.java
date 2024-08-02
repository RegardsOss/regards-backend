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
package fr.cnes.regards.modules.acquisition.service.plugins;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.exception.PluginInitException;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author Thomas GUILLOU
 **/
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=regex_scan_test" })
public class DiskStreamScanningIT {

    private Path scanPath;

    @Before
    public void before() throws URISyntaxException {
        scanPath = Paths.get(DiskStreamScanningIT.class.getClassLoader()
                                                       .getResource("data/plugins/stream-scan")
                                                       .toURI());
    }

    @Test
    public void testRegexPlugin() throws ModuleException, PluginInitException {
        // invalid regex
        Assertions.assertThrows(PluginInitException.class, () -> instanciateRegexPlugin("*"));

        // correct regex
        instanciateRegexPlugin(".*");

        Assertions.assertEquals(4, scanFilesFromRegex(".*").size());
        Assertions.assertEquals(2, scanFilesFromRegex(".*file1.*").size());
        Assertions.assertEquals(2, scanFilesFromRegex(".*folder.*file[0-9].*").size());
    }

    @Test
    public void testGlobPlugin() throws ModuleException, PluginInitException {
        // invalid glob pattern
        Assertions.assertThrows(PluginInitException.class, () -> instanciateGlobPlugin("[]"));

        // correct glob pattern
        instanciateGlobPlugin("*");

        Assertions.assertEquals(4, scanFilesFromGlob("*").size());
        Assertions.assertEquals(2, scanFilesFromGlob("*file1*").size());
        Assertions.assertEquals(1, scanFilesFromGlob("*folder*file1*").size());
    }

    private List<Path> scanFilesFromRegex(String regex) throws PluginInitException, ModuleException {
        RegexStreamDiskScanningPlugin plugin = instanciateRegexPlugin(regex);
        List<Stream<Path>> streams = plugin.stream(scanPath, Optional.empty());
        return streams.stream().flatMap(st -> st).toList();
    }

    private List<Path> scanFilesFromGlob(String glob) throws PluginInitException, ModuleException {
        GlobDiskStreamScanningPlugin plugin = instanciateGlobPlugin(glob);
        List<Stream<Path>> streams = plugin.stream(scanPath, Optional.empty());
        return streams.stream().flatMap(st -> st).toList();
    }

    private GlobDiskStreamScanningPlugin instanciateGlobPlugin(String glob) throws PluginInitException {
        GlobDiskStreamScanningPlugin globDiskStreamScanningPlugin = new GlobDiskStreamScanningPlugin();
        globDiskStreamScanningPlugin.glob = glob;
        globDiskStreamScanningPlugin.init();
        return globDiskStreamScanningPlugin;
    }

    private RegexStreamDiskScanningPlugin instanciateRegexPlugin(String regex) throws PluginInitException {
        RegexStreamDiskScanningPlugin regexStreamDiskScanningPlugin = new RegexStreamDiskScanningPlugin();
        regexStreamDiskScanningPlugin.regex = regex;
        regexStreamDiskScanningPlugin.init();
        return regexStreamDiskScanningPlugin;
    }
}
