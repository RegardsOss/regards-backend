/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.service.file;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.fileaccess.dto.FileReferenceDto;
import fr.cnes.regards.modules.fileaccess.dto.StorageType;
import fr.cnes.regards.modules.fileaccess.plugin.domain.INearlineStorageLocation;
import fr.cnes.regards.modules.fileaccess.plugin.domain.IOnlineStorageLocation;
import fr.cnes.regards.modules.fileaccess.plugin.domain.NearlineDownloadException;
import fr.cnes.regards.modules.fileaccess.plugin.domain.NearlineFileNotAvailableException;
import fr.cnes.regards.modules.storage.domain.DownloadableFile;
import fr.cnes.regards.modules.storage.domain.database.*;
import fr.cnes.regards.modules.storage.service.cache.CacheService;
import fr.cnes.regards.modules.storage.service.file.request.FileCacheRequestService;
import fr.cnes.regards.modules.storage.service.location.StorageLocationConfigurationService;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.apache.commons.compress.utils.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeType;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Predicates.instanceOf;
import static io.vavr.API.$;
import static io.vavr.API.Case;

/**
 * Service to handle files download.<br>
 *
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class FileDownloadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileDownloadService.class);

    public static final String FILES_PATH = "/files";

    public static final String DOWNLOAD_TOKEN_PATH = "/{checksum}/download/token";

    public static final String TOKEN_PARAM = "t";

    @Autowired
    private StorageLocationConfigurationService storageLocationConfService;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private FileReferenceService fileRefService;

    @Autowired
    private CacheService cachedFileService;

    @Autowired
    private FileCacheRequestService fileCacheReqService;

    @Autowired
    private FileReferenceService fileReferenceService;

    /**
     * Download a file thanks to its checksum. If the file is stored in multiple storage location,
     * this method decide which one to retrieve by : <ul>
     * <li>Only Files on an {@link IOnlineStorageLocation} location can be download</li>
     * <li>Use the {@link StorageLocationConfiguration} configuration with the highest priority</li>
     * </ul>
     * WARNING : the return of the downloadFile is a Callable ! That means the real download will be done
     * only when downloadFile(checksum).call() is done. Some repository access is not done here, but inside the Callable
     * That means the method which call this callable must
     * - be not transactional
     * - or be transactional with option noRollbackFor = { NearlineFileNotAvailableException.class }
     *
     * @param checksum Checksum of the file to download
     */
    @Transactional(noRollbackFor = { EntityNotFoundException.class })
    public Callable<DownloadableFile> downloadFile(String checksum) throws ModuleException {
        //noinspection unchecked
        return Try.success(checksum)
                  .flatMap(this::downloadCacheFile)
                  .recoverWith(NoSuchElementException.class,
                               Try.of(() -> fileRefService.search(checksum))
                                  .filter(s -> !s.isEmpty())
                                  .mapFailure(Case($(instanceOf(NoSuchElementException.class)),
                                                   ex -> new EntityNotFoundException(checksum, FileReferenceDto.class)),
                                              Case($(), (Function<Throwable, ModuleException>) ModuleException::new))
                                  .map(fileRefs -> fileRefs.stream()
                                                           .collect(Collectors.toMap(f -> f.getLocation().getStorage(),
                                                                                     f -> f)))
                                  .flatMap(storages -> searchOnline(storages).orElse(() -> searchNearline(storages))
                                                                             .toTry(() -> new ModuleException(String.format(
                                                                                 "No storage location configured for the given file reference (checksum %s). The file can not be download from %s.",
                                                                                 checksum,
                                                                                 Arrays.toString(storages.keySet()
                                                                                                         .toArray()))))))
                  // let exceptions bubble up so that Spring Tx manager isn't all fucked up...
                  .get();
    }

    protected Option<Callable<DownloadableFile>> searchOnline(Map<String, FileReference> storages) {
        return Option.ofOptional(storageLocationConfService.searchActiveHigherPriority(storages.keySet(),
                                                                                       StorageType.ONLINE))
                     .map(storageLocation -> {
                         PluginConfiguration conf = storageLocation.getPluginConfiguration();
                         FileReference fileToDownload = storages.get(conf.getLabel());
                         return () -> {
                             Long fileSize = fileToDownload.getMetaInfo().getFileSize();
                             String fileName = fileToDownload.getMetaInfo().getFileName();
                             MimeType mimeType = fileToDownload.getMetaInfo().getMimeType();
                             InputStream is = downloadOnline(fileToDownload, storageLocation);
                             return isRawData(fileToDownload) ?
                                 new QuotaLimitedDownloadableFile(is, fileSize, fileName, mimeType) :
                                 new StandardDownloadableFile(is, fileSize, fileName, mimeType);
                         };
                     });
    }

    protected Option<Callable<DownloadableFile>> searchNearline(Map<String, FileReference> storages) {
        return Option.ofOptional(storageLocationConfService.searchActiveHigherPriority(storages.keySet(),
                                                                                       StorageType.NEARLINE))
                     .map(storageLocation -> {
                         PluginConfiguration conf = storageLocation.getPluginConfiguration();
                         FileReference fileToDownload = storages.get(conf.getLabel());
                         return () -> {
                             Long fileSize = fileToDownload.getMetaInfo().getFileSize();
                             String fileName = fileToDownload.getMetaInfo().getFileName();
                             MimeType mimeType = fileToDownload.getMetaInfo().getMimeType();
                             InputStream is = download(fileToDownload);
                             return isRawData(fileToDownload) ?
                                 new QuotaLimitedDownloadableFile(is, fileSize, fileName, mimeType) :
                                 new StandardDownloadableFile(is, fileSize, fileName, mimeType);
                         };
                     });
    }

    /**
     * Download a file from the cache system if exists.
     * <p>
     * Eagerly accesses DB while lazily returning a DownloadableFile
     * so that InputStream is not opened until it's needed.
     *
     * @param checksum of the file to download
     */
    private Try<Callable<DownloadableFile>> downloadCacheFile(String checksum) {
        return Option.ofOptional(cachedFileService.findByChecksum(checksum)).toTry().map(cachedFileToDownload -> () -> {
            if (cachedFileToDownload.isInternalCache()) {
                return downloadFromInternalCacheFile(cachedFileToDownload);
            } else {
                return downloadFromExternalCacheFile(cachedFileToDownload, checksum);
            }
        });
    }

    private DownloadableFile downloadFromExternalCacheFile(CacheFile cachedFileToDownload, String checksum)
        throws ModuleException {
        if (cachedFileToDownload.getExpirationDate().isBefore(OffsetDateTime.now())) {
            cachedFileService.delete(cachedFileToDownload);
            throw new NearlineFileNotAvailableException(String.format("Nearline file %s with checksum %s has expired",
                                                                      cachedFileToDownload.getFileName(),
                                                                      cachedFileToDownload.getChecksum()));
        }

        // file is cached in an external cache, we download it with corresponding plugin
        try {
            INearlineStorageLocation plugin = pluginService.getPlugin(cachedFileToDownload.getExternalCachePlugin());
            // we got checksum and plugin associated of the cached file.
            // we don't need to retrieve exact FileReference in database.
            // instead, we recreate a fileReference with needed information.
            FileReference fakeFileReference = simulateFileReferenceFromCacheFile(checksum, cachedFileToDownload);
            InputStream download = plugin.download(fakeFileReference.toDtoWithoutOwners());
            return new StandardDownloadableFile(download,
                                                cachedFileToDownload.getFileSize(),
                                                cachedFileToDownload.getFileName(),
                                                cachedFileToDownload.getMimeType());
        } catch (NearlineFileNotAvailableException e) {
            LOGGER.error("Nearline file {} with checksum {} is not available",
                         cachedFileToDownload.getFileName(),
                         cachedFileToDownload.getChecksum(),
                         e);
            cachedFileService.delete(cachedFileToDownload);
            throw e;
        } catch (NearlineDownloadException e) {
            LOGGER.error("An error occurred while downloading external cached file {}", checksum, e);
            throw e;
        } catch (NotAvailablePluginConfigurationException e) {
            String message = String.format("Plugin %s is not available, cannot retrieve external cache file %s",
                                           cachedFileToDownload.getExternalCachePlugin(),
                                           cachedFileToDownload.getFileName());
            LOGGER.error(message);
            throw new ModuleException(message, e);
        }
    }

    private DownloadableFile downloadFromInternalCacheFile(CacheFile cachedFileToDownload)
        throws EntityNotFoundException {
        try {
            Long fileSize = cachedFileToDownload.getFileSize();
            String fileName = cachedFileToDownload.getFileName();
            MimeType mimeType = cachedFileToDownload.getMimeType();
            FileInputStream is = new FileInputStream(cachedFileToDownload.getLocation().getPath());
            return isRawData(cachedFileToDownload) ?
                new QuotaLimitedDownloadableFile(is, fileSize, fileName, mimeType) :
                new StandardDownloadableFile(is, fileSize, fileName, mimeType);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new EntityNotFoundException(String.format("File %s not found in cache system",
                                                            cachedFileToDownload.getLocation().getPath()));
        }
    }

    /**
     * Create and complete a FileReference with maximum information stored in a CacheFile.
     */
    private static FileReference simulateFileReferenceFromCacheFile(String checksum, CacheFile cachedFileToDownload) {
        FileReference fileReference = new FileReference();
        FileReferenceMetaInfo metaInfo = new FileReferenceMetaInfo(checksum,
                                                                   "MD5",
                                                                   cachedFileToDownload.getFileName(),
                                                                   cachedFileToDownload.getFileSize(),
                                                                   cachedFileToDownload.getMimeType());
        FileLocation location = new FileLocation();
        metaInfo.setType(cachedFileToDownload.getType());
        fileReference.setMetaInfo(metaInfo);
        fileReference.setNearlineConfirmed(false);
        fileReference.setLocation(location);
        return fileReference;
    }

    private boolean isRawData(CacheFile cachedFile) {
        return Objects.equals(cachedFile.getType(), DataType.RAWDATA.name());
    }

    private boolean isRawData(FileReference fileRef) {
        return Objects.equals(fileRef.getMetaInfo().getType(), DataType.RAWDATA.name());
    }

    /**
     * Download a file from an ONLINE storage location.
     */
    @Transactional(readOnly = true)
    public InputStream downloadOnline(FileReference fileToDownload, StorageLocationConfiguration storagePluginConf)
        throws ModuleException {
        try {
            IOnlineStorageLocation plugin = pluginService.getPlugin(storagePluginConf.getPluginConfiguration()
                                                                                     .getBusinessId());
            return plugin.retrieve(fileToDownload.toDtoWithoutOwners());
        } catch (NotAvailablePluginConfigurationException e) {
            throw new ModuleException(String.format(
                "Unable to download file %s (checksum : %s) as its storage location %s is not active.",
                fileToDownload.getMetaInfo().getFileName(),
                fileToDownload.getMetaInfo().getChecksum(),
                fileToDownload.getLocation().toString()), e);
        } catch (FileNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
            throw new EntityNotFoundException(e.getMessage());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new ModuleException(e.getMessage(), e);
        }
    }

    /**
     * Try to download a nearline file. If the file is in the cache system, then the file can be download. Else,
     * a availability request is created and a {@link EntityNotFoundException} is thrown.
     *
     * @param fileToDownload {@link FileReference} to download.
     * @return stream of the file from its cache copy.
     * @throws EntityNotFoundException If file is not in cache currently.
     */
    @Transactional(noRollbackFor = EntityNotFoundException.class)
    public InputStream download(FileReference fileToDownload)
        throws EntityNotFoundException, NearlineFileNotAvailableException {
        Optional<CacheFile> ocf = cachedFileService.findByChecksum(fileToDownload.getMetaInfo().getChecksum());
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
        fileCacheReqService.makeAvailable(Sets.newHashSet(fileToDownload), 24, UUID.randomUUID().toString());
        throw new NearlineFileNotAvailableException(String.format("File %s is not available yet. Please try later.",
                                                                  fileToDownload.getMetaInfo().getFileName()));
    }

    public static class StandardDownloadableFile extends DownloadableFile {

        protected StandardDownloadableFile(InputStream fileInputStream,
                                           Long fileSize,
                                           String fileName,
                                           MimeType mediaType) {
            super(fileInputStream, fileSize, fileName, mediaType);
        }

        @Override
        public void close() {
            InputStream is = getFileInputStream();
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    LOGGER.error("Error closing File input stream.", e);
                }
            }
        }
    }

    public static class QuotaLimitedDownloadableFile extends StandardDownloadableFile {

        protected QuotaLimitedDownloadableFile(InputStream fileInputStream,
                                               Long fileSize,
                                               String fileName,
                                               MimeType mediaType) {
            super(fileInputStream, fileSize, fileName, mediaType);
        }
    }
}
