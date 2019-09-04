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
package fr.cnes.regards.modules.storagelight.service.file;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.compress.utils.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.storagelight.domain.database.CacheFile;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.service.cache.CacheService;
import fr.cnes.regards.modules.storagelight.service.file.request.FileCacheRequestService;

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
        Optional<CacheFile> ocf = cachedFileService.getCacheFile(fileToDownload.getMetaInfo().getChecksum());
        if (ocf.isPresent()) {
            // File is in cache and can be download
            try {
                // File is present in cache return stream
                return new FileInputStream(ocf.get().getLocation().getPath());
            } catch (FileNotFoundException e) {
                // Only log error and then ask for new availability of the file
                LOGGER.error(e.getMessage(), e);
            }
        }
        // ask for file availability and return a not available yet response
        fileCacheReqService.makeAvailable(Sets.newHashSet(fileToDownload), OffsetDateTime.now().plusHours(1),
                                          UUID.randomUUID().toString());
        throw new EntityNotFoundException(String.format("File %s is not available yet. Please try later.",
                                                        fileToDownload.getMetaInfo().getFileName()));
    }
}
