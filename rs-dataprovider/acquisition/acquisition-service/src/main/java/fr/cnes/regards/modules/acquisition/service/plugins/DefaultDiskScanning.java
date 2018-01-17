/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;
import fr.cnes.regards.modules.acquisition.plugins.IScanPlugin;

/**
 * Scan directories and return detected files according to last modification date filter and glob pattern.
 *
 * @author Marc Sordi
 *
 */
@Plugin(id = "DefaultDiskScanning", version = "1.0.0-SNAPSHOT", description = "Scan directories to detect files",
        author = "REGARDS Team", contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class DefaultDiskScanning implements IScanPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDiskScanning.class);

    public static final String FIELD_DIRS = "directories";

    public static final String FIELD_GLOB = "glob";

    @PluginParameter(name = FIELD_DIRS, label = "List of directories to scan")
    private List<String> directories;

    @PluginParameter(name = FIELD_GLOB, label = "Glob pattern", defaultValue = "*", optional = true)
    private String glob;

    @Override
    public List<Path> scan(Optional<OffsetDateTime> lastModificationDate) {

        List<Path> scannedFiles = new ArrayList<>();

        for (String dir : directories) {
            Path dirPath = Paths.get(dir);
            if (Files.isDirectory(dirPath)) {
                scannedFiles.addAll(scanDirectory(dirPath, lastModificationDate));
            } else {
                LOGGER.error("Invalid directory path : {}", dirPath.toString());
            }
        }
        return scannedFiles;
    }

    private List<Path> scanDirectory(Path dirPath, Optional<OffsetDateTime> lastModificationDate) {
        List<Path> scannedFiles = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, glob)) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)) {
                    if (lastModificationDate.isPresent()) {
                        OffsetDateTime lmd = OffsetDateTime.ofInstant(Files.getLastModifiedTime(entry).toInstant(),
                                                                      ZoneOffset.UTC);
                        if (lmd.isAfter(lastModificationDate.get())) {
                            scannedFiles.add(entry);
                        }
                    } else {
                        scannedFiles.add(entry);
                    }
                }
            }
        } catch (IOException x) {
            throw new PluginUtilsRuntimeException("Scanning failure", x);
        }

        return scannedFiles;
    }

}
