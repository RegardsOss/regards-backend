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
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.compress.utils.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.storagelight.domain.DownloadableFile;
import fr.cnes.regards.modules.storagelight.domain.database.CacheFile;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.PrioritizedStorage;
import fr.cnes.regards.modules.storagelight.domain.dto.FileReferenceDTO;
import fr.cnes.regards.modules.storagelight.domain.plugin.IOnlineStorageLocation;
import fr.cnes.regards.modules.storagelight.service.cache.CacheService;
import fr.cnes.regards.modules.storagelight.service.file.request.FileCacheRequestService;
import fr.cnes.regards.modules.storagelight.service.location.PrioritizedStorageService;

/**
 * @author sbinda
 *
 */
@Service
@MultitenantTransactional
public class FileDownloadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileDownloadService.class);

    @Autowired
    private PrioritizedStorageService prioritizedStorageService;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private FileReferenceService fileRefService;

    @Autowired
    private CacheService cachedFileService;

    @Autowired
    private FileCacheRequestService fileCacheReqService;

    /**
     * Download a file thanks to its checksum. If the file is stored in multiple storage location,
     * this method decide which one to retrieve by : <ul>
     *  <li>Only Files on an {@link IOnlineStorageLocation} location can be download</li>
     *  <li>Use the {@link PrioritizedStorage} configuration with the highest priority</li>
     * </ul>
     *
     * @param checksum Checksum of the file to download
     * @throws FileNotFoundException
     */
    @Transactional(noRollbackFor = { EntityNotFoundException.class })
    public DownloadableFile downloadFile(String checksum) throws ModuleException {
        // 1. Retrieve all the FileReference matching the given checksum
        Set<FileReference> fileRefs = fileRefService.search(checksum);
        if (fileRefs.isEmpty()) {
            throw new EntityNotFoundException(checksum, FileReferenceDTO.class);
        }
        Map<String, FileReference> storages = fileRefs.stream()
                .collect(Collectors.toMap(f -> f.getLocation().getStorage(), f -> f));
        // 2. get the storage location with the higher priority
        Optional<PrioritizedStorage> storageLocation = prioritizedStorageService
                .searchActiveHigherPriority(storages.keySet());
        if (storageLocation.isPresent()) {
            PluginConfiguration conf = storageLocation.get().getStorageConfiguration();
            FileReference fileToDownload = storages.get(conf.getLabel());
            return new DownloadableFile(downloadFileReference(fileToDownload),
                    fileToDownload.getMetaInfo().getFileSize(), fileToDownload.getMetaInfo().getFileName(),
                    fileToDownload.getMetaInfo().getMimeType());

        } else {
            throw new ModuleException(String
                    .format("No storage location configured for the given file reference (checksum %s). The file can not be download from %s.",
                            checksum, Arrays.toString(storages.keySet().toArray())));
        }
    }

    /**
     * Try to download the given {@link FileReference}.
     * @param fileToDownload
     * @return {@link InputStream} of the file
     * @throws ModuleException
     */
    @Transactional(noRollbackFor = { EntityNotFoundException.class })
    public InputStream downloadFileReference(FileReference fileToDownload) throws ModuleException {
        Optional<PrioritizedStorage> conf = prioritizedStorageService.search(fileToDownload.getLocation().getStorage());
        if (conf.isPresent()) {
            switch (conf.get().getStorageType()) {
                case NEARLINE:
                    return download(fileToDownload);
                case ONLINE:
                    return downloadOnline(fileToDownload, conf.get());
                default:
                    break;
            }
        }
        throw new ModuleException(
                String.format("Unable to download file %s (checksum : %s) as its storage location %s is not available",
                              fileToDownload.getMetaInfo().getFileName(), fileToDownload.getMetaInfo().getChecksum(),
                              fileToDownload.getLocation().toString()));
    }

    /**
     * Download a file from an ONLINE storage location.
     * @param fileToDownload
     * @param storagePluginConf
     * @return
     * @throws ModuleException
     */
    @Transactional(readOnly = true)
    private InputStream downloadOnline(FileReference fileToDownload, PrioritizedStorage storagePluginConf)
            throws ModuleException {
        try {
            IOnlineStorageLocation plugin = pluginService
                    .getPlugin(storagePluginConf.getStorageConfiguration().getBusinessId());
            return plugin.retrieve(fileToDownload);
        } catch (NotAvailablePluginConfigurationException e) {
            throw new ModuleException(String
                    .format("Unable to download file %s (checksum : %s) as its storage location %s is not active.",
                            fileToDownload.getMetaInfo().getFileName(), fileToDownload.getMetaInfo().getChecksum(),
                            fileToDownload.getLocation().toString()),
                    e);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new ModuleException(e.getMessage(), e);
        }
    }

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
