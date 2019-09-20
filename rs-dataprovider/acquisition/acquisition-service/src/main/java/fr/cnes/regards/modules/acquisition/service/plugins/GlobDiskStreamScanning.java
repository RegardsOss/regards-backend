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
package fr.cnes.regards.modules.acquisition.service.plugins;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import fr.cnes.regards.modules.acquisition.plugins.IFluxScanPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;

/**
 * Scan directories and return detected files according to last modification date filter and glob pattern by stream.
 *
 * @author Marc Sordi
 *
 */
@Plugin(id = "GlobDiskStreamScanning", version = "1.0.0-SNAPSHOT",
        description = "Scan directories to detect files filtering with a glob pattern by stream",
        markdown = "GlobDiskStreamScanning.md", author = "REGARDS Team", contact = "regards@c-s.fr", license = "GPLv3",
        owner = "CSSI", url = "https://github.com/RegardsOss")
public class GlobDiskStreamScanning implements IFluxScanPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobDiskStreamScanning.class);

    public static final String FIELD_DIRS = "directories";

    public static final String FIELD_GLOB = "glob";

    @PluginParameter(name = FIELD_DIRS, label = "List of directories to scan")
    private List<String> directories;

    @PluginParameter(name = FIELD_GLOB, label = "Glob pattern", markdown = "glob_pattern.md", defaultValue = "*",
            optional = true)
    private String glob;

    @Override
    public List<DirectoryStream<Path>> stream(Optional<OffsetDateTime> lastModificationDate) throws ModuleException {

        List<DirectoryStream<Path>> scannedFiles = null;

        for (String dir : directories) {
            Path dirPath = Paths.get(dir);
            if (Files.isDirectory(dirPath)) {
                scannedFiles = scanDirectory(dirPath, lastModificationDate);
            } else {
                LOGGER.error("Invalid directory path : {}", dirPath.toString());
            }
        }
        return scannedFiles;
    }

    private static void walk(List<DirectoryStream<Path>> paths, Path path, DirectoryStream.Filter<Path> filter) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    paths.add(Files.newDirectoryStream(entry, filter));
                    walk(paths, entry, filter);
                }
            }
        }
    }

    private List<DirectoryStream<Path>> scanDirectory(Path dirPath, Optional<OffsetDateTime> lastModificationDate)
            throws ModuleException {
        try {
            FileSystem fs = dirPath.getFileSystem();
            final PathMatcher matcher = fs.getPathMatcher("glob:" + glob);
            List<DirectoryStream<Path>> paths = new ArrayList<>();
            DirectoryStream.Filter<Path> filter = entry -> {
                boolean match = Files.isRegularFile(entry) && matcher.matches(entry.getFileName());
                if (match && lastModificationDate.isPresent()) {
                    OffsetDateTime lmd = OffsetDateTime.ofInstant(Files.getLastModifiedTime(entry).toInstant(),
                            ZoneOffset.UTC);
                    return lmd.isAfter(lastModificationDate.get()) || lmd.isEqual(lastModificationDate.get());
                } else {
                    return match;
                }
            };
            walk(paths, dirPath, filter);
            return paths;
        } catch (IOException e) {
            throw new ModuleException(e.getMessage(), e);
        }
    }
}
