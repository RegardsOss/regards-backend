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
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.compress.utils.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.acquisition.plugins.IFluxScanPlugin;

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

    @Autowired
    private INotificationClient notifClient;

    @Override
    public List<Stream<Path>> stream(Optional<OffsetDateTime> lastModificationDate) throws ModuleException {
        List<Stream<Path>> dirStreams = Lists.newArrayList();
        for (String dir : directories) {
            Path dirPath = Paths.get(dir);
            if (Files.isDirectory(dirPath)) {
                dirStreams.add(scanDirectory(dirPath, lastModificationDate));
            } else {
                String message = String.format("Configured directory %s for scan does not exists or is not accessible.",
                                               dirPath.toString());
                LOGGER.error(message);
                notifClient.notify(message, "Acquisition chain invalid", NotificationLevel.WARNING, DefaultRole.EXPLOIT,
                                   DefaultRole.ADMIN, DefaultRole.PROJECT_ADMIN);
            }
        }
        return dirStreams;
    }

    private Stream<Path> scanDirectory(Path dirPath, Optional<OffsetDateTime> lastModificationDate)
            throws ModuleException {
        try {
            FileSystem fs = dirPath.getFileSystem();
            final PathMatcher matcher = fs.getPathMatcher("glob:" + glob);
            Predicate<Path> filter = entry -> {
                boolean match = Files.isReadable(entry) && Files.isRegularFile(entry)
                        && matcher.matches(entry.getFileName());
                if (match && lastModificationDate.isPresent()) {
                    OffsetDateTime lmd;
                    try {
                        lmd = OffsetDateTime.ofInstant(Files.getLastModifiedTime(entry).toInstant(), ZoneOffset.UTC);
                        return lmd.isAfter(lastModificationDate.get()) || lmd.isEqual(lastModificationDate.get());
                    } catch (IOException e) {
                        LOGGER.error(e.getMessage(), e);
                        match = false;
                    }
                }
                return match;
            };
            return Files.walk(dirPath).filter(filter);

        } catch (IOException e) {
            throw new ModuleException(e.getMessage(), e);
        }
    }
}
