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
package fr.cnes.regards.modules.acquisition.plugins.scan;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;
import fr.cnes.regards.modules.acquisition.plugins.IScanPlugin;
import fr.cnes.regards.modules.acquisition.plugins.Microscope;

/**
 * @author oroussel
 */
@Plugin(id = "TarGzScanPlugin", version = "1.0.0-SNAPSHOT",
        description = "Scan directory to detect files with '.tar.gz'", author = "REGARDS Team",
        contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI", url = "https://github.com/RegardsOss")
public class TarGzScanPlugin implements IScanPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataScanPlugin.class);

    public static final String FIELD_DIR = "directory";

    @PluginParameter(name = FIELD_DIR, label = "Root product directory to scan")
    private String directory;

    @Override
    public List<Path> scan(Optional<OffsetDateTime> lastModificationDate) {
        List<Path> metadataFiles = new ArrayList<>();
        Path dirPath = Paths.get(directory);
        if (Files.isDirectory(dirPath)) {
            metadataFiles.addAll(scanDirectory(dirPath, lastModificationDate));
        } else {
            LOGGER.error("Invalid directory path : {}", dirPath.toString());
        }
        return metadataFiles;
    }

    private List<Path> scanDirectory(Path dirPath, Optional<OffsetDateTime> lastModificationDate) {
        List<Path> metadataFiles = new ArrayList<>();
        // Search only sub directories and metadata files
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(dirPath, path -> Files.isDirectory(path) || path
                .getFileName().toString().endsWith(Microscope.TAR_GZ_EXT))) {
            for (Path path : paths) {
                // metadata file...
                if (Files.isRegularFile(path)) {
                    if (lastModificationDate.isPresent()) {
                        OffsetDateTime lmd = OffsetDateTime
                                .ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneOffset.UTC);
                        if (lmd.isAfter(lastModificationDate.get())) {
                            metadataFiles.add(path);
                        }
                    } else {
                        metadataFiles.add(path);
                    }
                } else { // directory => recursive
                    metadataFiles.addAll(scanDirectory(path, lastModificationDate));
                }
            }
            // Sort by path
            Collections.sort(metadataFiles);
        } catch (IOException x) {
            throw new PluginUtilsRuntimeException("Scanning failure", x);
        }
        return metadataFiles;
    }
}
