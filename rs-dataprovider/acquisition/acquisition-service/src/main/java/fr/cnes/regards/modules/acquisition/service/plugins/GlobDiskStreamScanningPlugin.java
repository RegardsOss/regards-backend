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
package fr.cnes.regards.modules.acquisition.service.plugins;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.exception.PluginInitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.regex.PatternSyntaxException;

/**
 * Scan directories and return detected files according to last modification date filter and glob pattern by stream.
 *
 * @author Marc Sordi
 */
@Plugin(id = "GlobDiskStreamScanning",
        version = "1.0.0-SNAPSHOT",
        description = "Scan directories to detect files filtering with a glob pattern by stream",
        markdown = "GlobDiskStreamScanning.md",
        author = "REGARDS Team",
        contact = "regards@c-s.fr",
        license = "GPLv3",
        owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class GlobDiskStreamScanningPlugin extends DiskStreamScanningCommon {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobDiskStreamScanningPlugin.class);

    public static final String FIELD_GLOB = "glob";

    @PluginParameter(name = FIELD_GLOB,
                     label = "Glob pattern",
                     markdown = "glob_pattern.md",
                     defaultValue = "*",
                     optional = true)
    public String glob;

    private PathMatcher matcher;

    @PluginInit
    public void init() throws PluginInitException {
        try {
            matcher = FileSystems.getDefault().getPathMatcher("glob:" + glob);
        } catch (PatternSyntaxException e) {
            LOGGER.error("Glob pattern error : " + e.getMessage());
            throw new PluginInitException("Glob pattern error : " + e.getMessage());
        }
    }

    @Override
    protected boolean isPathMatchPattern(Path path) {
        return matcher.matches(path.getFileName());
    }
}
