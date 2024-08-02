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

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.exception.PluginInitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Scan directories recursively and return detected files according to last modification date filter and regex
 * pattern by stream.
 *
 * @author Thomas GUILLOU
 **/
@Plugin(id = "RegexDiskStreamScanning",
        version = "1.0.0-SNAPSHOT",
        description = "Scan directories to detect files filtering with a regex pattern by stream",
        markdown = "RegexDiskStreamScanning.md",
        author = "REGARDS Team",
        contact = "regards@c-s.fr",
        license = "GPLv3",
        owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class RegexStreamDiskScanningPlugin extends DiskStreamScanningCommon {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegexStreamDiskScanningPlugin.class);

    public static final String FIELD_REGEX = "regex";

    @PluginParameter(name = FIELD_REGEX, label = "Regex pattern", defaultValue = ".*", optional = true)
    public String regex;

    private Pattern pattern;

    @PluginInit
    public void init() throws PluginInitException {
        try {
            pattern = Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            LOGGER.error("Regex pattern error : " + e.getMessage());
            throw new PluginInitException("Regex pattern error : " + e.getMessage());
        }
    }

    @Override
    protected boolean isPathMatchPattern(Path path) {
        return pattern.matcher(path.getFileName().toString()).matches();
    }
}
