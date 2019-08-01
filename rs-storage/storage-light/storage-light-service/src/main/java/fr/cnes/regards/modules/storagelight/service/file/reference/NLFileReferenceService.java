/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storagelight.service.file.reference;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.compress.utils.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.storagelight.domain.database.CachedFile;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileCacheRequest;
import fr.cnes.regards.modules.storagelight.service.file.cache.CacheService;
import fr.cnes.regards.modules.storagelight.service.file.reference.flow.FileRefEventPublisher;

/**
 * <b>N</b>ear<b>L</b>lineFileReferenceService. Handle file reference stored on a storage location of type NEARLINE.
 * NEARLINE, means that files stored can not be retrieved synchronously. For example files stored on a  tapes.
 *
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class NLFileReferenceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NLFileReferenceService.class);

    @Autowired
    private FileRefEventPublisher publisher;

    @Autowired
    private CacheService cachedFileService;

    @Autowired
    private FileCacheRequestService fileCacheReqService;

    /**
     * Try to download a nearline file. If the file is in the cache system, then the file can be downloaded. Else,
     * a availability request is created and a {@link EntityNotFoundException} is thrown.
     * @param fileToDownload {@link FileReference} to download.
     * @return stream of the file from its cache copy.
     * @throws EntityNotFoundException If file is not in cache currently.
     */
    @Transactional(noRollbackFor = EntityNotFoundException.class)
    public InputStream download(FileReference fileToDownload) throws EntityNotFoundException {
        Optional<CachedFile> ocf = cachedFileService.getAvailable(fileToDownload);
        if (ocf.isPresent()) {
            // File is in cache and can be downloaded
            try {
                // File is present in cache return stream
                return new FileInputStream(ocf.get().getLocation().getPath());
            } catch (FileNotFoundException e) {
                // Only log error and then ask for new availability of the file
                LOGGER.error(e.getMessage(), e);
            }
        }
        // ask for file availability and return a not available yet response
        makeAvailable(Sets.newHashSet(fileToDownload), OffsetDateTime.now().plusHours(1));
        throw new EntityNotFoundException(String.format("File %s is not available yet. Please try later.",
                                                        fileToDownload.getMetaInfo().getFileName()));
    }

    /**
     * Creates {@link FileCacheRequest} for each {@link FileReference} to be available for download.
     * After copy in cache, files will be available until the given expiration date.
     * @param fileReferences
     * @param expirationDate
     */
    public void makeAvailable(Set<FileReference> fileReferences, OffsetDateTime expirationDate) {
        // Check files already available in cache
        Set<FileReference> availables = cachedFileService.getAvailables(fileReferences);
        Set<FileReference> toRestore = fileReferences.stream().filter(f -> !availables.contains(f))
                .collect(Collectors.toSet());
        // Notify available
        notifyAvailables(availables);
        // Create a restoration request for all to restore
        for (FileReference f : toRestore) {
            fileCacheReqService.create(f);
        }
    }

    /**
     * Notify all files as AVAILABLE.
     * @param availables
     */
    private void notifyAvailables(Set<FileReference> availables) {
        availables.forEach(f -> publisher
                .publishFileRefAvailable(f.getMetaInfo().getChecksum(),
                                         String.format("file %s (checksum %s) is available for download.",
                                                       f.getMetaInfo().getFileName(), f.getMetaInfo().getChecksum())));
    }

}
