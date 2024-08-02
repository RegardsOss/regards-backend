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
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.acquisition.plugins.IFluxScanPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Scan plugin that use stream on a file tree structure.
 * Can filter files according to their last modification date
 *
 * @author Thomas GUILLOU
 **/
public abstract class DiskStreamScanningCommon implements IFluxScanPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiskStreamScanningCommon.class);

    @Autowired
    private INotificationClient notifClient;

    @Override
    public List<Stream<Path>> stream(Path dirPath, Optional<OffsetDateTime> lastModificationDate)
        throws ModuleException {
        List<Stream<Path>> dirStreams = new ArrayList<>();
        if (Files.isDirectory(dirPath)) {
            dirStreams.add(scanDirectory(dirPath, lastModificationDate));
        } else {
            String message = String.format("Configured directory %s for scan does not exists or is not accessible.",
                                           dirPath.toString());
            LOGGER.error(message);
            notifClient.notify(message,
                               "Acquisition chain invalid",
                               NotificationLevel.WARNING,
                               DefaultRole.EXPLOIT,
                               DefaultRole.ADMIN,
                               DefaultRole.PROJECT_ADMIN);
        }
        return dirStreams;
    }

    private Stream<Path> scanDirectory(Path dirPath, Optional<OffsetDateTime> lastModificationDate)
        throws ModuleException {
        try {
            Stream<Path> pathStream = Files.walk(dirPath).filter(this::isRegularFile).filter(this::isPathMatchPattern);
            if (lastModificationDate.isPresent()) {
                pathStream = pathStream.filter(path -> isFileMatchDateFilter(path, lastModificationDate.get()));
            }
            return pathStream;
        } catch (IOException e) {
            throw new ModuleException(e.getMessage(), e);
        }
    }

    private boolean isFileMatchDateFilter(Path entry, OffsetDateTime lastModificationDate) {
        try {
            OffsetDateTime lastModifiedDate = OffsetDateTime.ofInstant(Files.getLastModifiedTime(entry).toInstant(),
                                                                       ZoneOffset.UTC);
            return (lastModifiedDate.isAfter(lastModificationDate) || lastModifiedDate.isEqual(lastModificationDate));
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return false;
    }

    private boolean isRegularFile(Path entry) {
        return Files.isReadable(entry) && Files.isRegularFile(entry);
    }

    protected abstract boolean isPathMatchPattern(Path path);
}
