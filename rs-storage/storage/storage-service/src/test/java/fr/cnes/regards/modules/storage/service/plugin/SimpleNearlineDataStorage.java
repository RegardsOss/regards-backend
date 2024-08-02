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
package fr.cnes.regards.modules.storage.service.plugin;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.framework.utils.file.DownloadUtils;
import fr.cnes.regards.modules.fileaccess.dto.FileReferenceWithoutOwnersDto;
import fr.cnes.regards.modules.fileaccess.dto.request.FileStorageRequestAggregationDto;
import fr.cnes.regards.modules.fileaccess.plugin.domain.*;
import fr.cnes.regards.modules.fileaccess.plugin.dto.FileCacheRequestDto;
import fr.cnes.regards.modules.fileaccess.plugin.dto.FileDeletionRequestDto;
import fr.cnes.regards.modules.storage.dao.IFileReferenceRepository;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequestAggregation;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author Binda sébastien
 */
@Plugin(author = "REGARDS Team",
        description = "Plugin handling the storage on local file system",
        id = "SimpleNearlineTest",
        version = "1.0",
        contact = "regards@c-s.fr",
        license = "GPLv3",
        owner = "CNES",
        url = "https://regardsoss.github.io/")
public class SimpleNearlineDataStorage implements INearlineStorageLocation {

    private String id = UUID.randomUUID().toString();

    /**
     * Plugin parameter name of the storage base location as a string
     */
    public static final String BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME = "Storage_URL";

    public static final String EXT_CACHE_PLUGIN_PARAM_NAME = "external_cache";

    public static final String HANDLE_STORAGE_ERROR_FILE_PATTERN = "error_file_pattern";

    public static final String HANDLE_STORAGE_PENDING_FILE_PATTERN = "pending_file_pattern";

    public static final String HANDLE_DELETE_ERROR_FILE_PATTERN = "delete_error_file_pattern";

    public static final String HANDLE_RESTORATION_ERROR_FILE_PATTERN = "resto_error_pattern";

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleNearlineDataStorage.class);

    private final static String BASE_URL = "target/storage-nearline";

    private final String doNotHandlePattern = "doNotHandle.*";

    /**
     * {@link IRuntimeTenantResolver} instance
     */
    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IFileReferenceRepository fileRefRepo;

    /**
     * Base storage location url
     */
    @PluginParameter(name = BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                     description = "Base storage location url to use",
                     label = "Base storage location url")
    private String baseStorageLocationAsString;

    @PluginParameter(name = HANDLE_STORAGE_ERROR_FILE_PATTERN,
                     description = "Error file pattern",
                     label = "Error file pattern")
    private String errorFilePattern;

    @PluginParameter(name = HANDLE_STORAGE_PENDING_FILE_PATTERN,
                     description = "Pending file pattern",
                     label = "Error file pattern")
    private String pendingFilePattern;

    @PluginParameter(name = HANDLE_DELETE_ERROR_FILE_PATTERN,
                     description = "Delete Error file pattern",
                     label = "Delete Error file pattern")
    private String deleteErrorFilePattern;

    @PluginParameter(name = HANDLE_RESTORATION_ERROR_FILE_PATTERN,
                     description = "Restoration Error file pattern",
                     label = "Delete Error file pattern")
    private String restoErrorFilePattern;

    @PluginParameter(name = EXT_CACHE_PLUGIN_PARAM_NAME,
                     description = "External cache",
                     label = "External cache",
                     defaultValue = "false")
    private boolean externalCache = false;

    private List<String> checksumsRestored = new ArrayList<>();

    private List<String> checksumsAvailables = new ArrayList<>();

    /**
     * Plugin init method
     */
    @PluginInit
    public void init() throws IOException {
        // Clear directory
        FileUtils.deleteDirectory(Paths.get(BASE_URL).toFile());
    }

    @Override
    public PreparationResponse<FileStorageWorkingSubset, FileStorageRequestAggregationDto> prepareForStorage(Collection<FileStorageRequestAggregationDto> FileReferenceRequest) {
        Collection<FileStorageWorkingSubset> workingSubSets = Lists.newArrayList();
        workingSubSets.add(new FileStorageWorkingSubset(Sets.newHashSet(FileReferenceRequest)));
        return PreparationResponse.build(workingSubSets, Maps.newHashMap());
    }

    @Override
    public void store(FileStorageWorkingSubset workingSubset, IStorageProgressManager progressManager) {
        // because we use a parallel stream, we need to get the tenant now and force it before each doStore call
        String tenant = runtimeTenantResolver.getTenant();
        workingSubset.getFileReferenceRequests().stream().forEach(data -> {
            runtimeTenantResolver.forceTenant(tenant);
            doStore(progressManager, FileStorageRequestAggregation.fromDto(data));
        });
    }

    private void doStore(IStorageProgressManager progressManager, FileStorageRequestAggregation fileRefRequest) {

        Assert.assertNotNull("File reference request cannot be null", fileRefRequest);
        Assert.assertNotNull("File reference request meta info cannot be null", fileRefRequest.getMetaInfo());
        Assert.assertNotNull("File reference request file name cannot be null",
                             fileRefRequest.getMetaInfo().getFileName());
        Assert.assertNotNull("File reference request checksum cannot be null",
                             fileRefRequest.getMetaInfo().getChecksum());
        Assert.assertNotNull("File reference request destination location cannot be null", fileRefRequest.getStorage());
        Assert.assertNotNull("File reference request origin location cannot be null", fileRefRequest.getOriginUrl());
        String fileName = fileRefRequest.getMetaInfo().getFileName();
        if (Pattern.matches(doNotHandlePattern, fileName)) {
            // Do nothing to test not handled files
            LOGGER.info("File {} ignored for storage", fileName);
        } else if (Pattern.matches(errorFilePattern, fileName)) {
            LOGGER.error("Simulated error for file storage {}.", fileName);
            progressManager.storageFailed(fileRefRequest.toDto(), "Specific error generated for tests");
        } else {
            String directory;
            if (fileRefRequest.getStorageSubDirectory() == null) {
                directory = fileRefRequest.getMetaInfo().getChecksum().substring(0, 5);
            } else {
                directory = fileRefRequest.getStorageSubDirectory();
            }
            String storedUrl = String.format("%s%s",
                                             BASE_URL,
                                             Paths.get("/", directory, fileRefRequest.getMetaInfo().getChecksum()));
            try {
                if (!Files.exists(Paths.get(storedUrl).getParent())) {
                    Files.createDirectories(Paths.get(storedUrl).getParent());
                }
                if (!Files.exists(Paths.get(storedUrl))) {
                    Files.createFile(Paths.get(storedUrl));
                    try (FileOutputStream out = new FileOutputStream(Paths.get(storedUrl).toFile())) {
                        byte[] bytes = new byte[1024];
                        new SecureRandom().nextBytes(bytes);
                        out.write(bytes);
                        out.flush();
                    }
                    LOGGER.info("Create file with size {}", Paths.get(storedUrl).toFile().length());
                }
                if (Pattern.matches(pendingFilePattern, fileName)) {
                    LOGGER.info("Simulated success with pending for file storage {}.", fileName);
                    progressManager.storageSucceedWithPendingActionRemaining(fileRefRequest.toDto(),
                                                                             new URL("file", null, storedUrl),
                                                                             1024L,
                                                                             true);
                } else {
                    LOGGER.info("Simulated success for file storage {}.", fileName);
                    progressManager.storageSucceed(fileRefRequest.toDto(), new URL("file", null, storedUrl), 1024L);
                }
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
                progressManager.storageFailed(fileRefRequest.toDto(), e.getMessage());
            }
        }
    }

    @Override
    public PreparationResponse<FileDeletionWorkingSubset, FileDeletionRequestDto> prepareForDeletion(Collection<FileDeletionRequestDto> fileDeletionRequests) {
        Collection<FileDeletionWorkingSubset> workingSubSets = Lists.newArrayList();
        workingSubSets.add(new FileDeletionWorkingSubset(Sets.newHashSet(fileDeletionRequests)));
        return PreparationResponse.build(workingSubSets, Maps.newHashMap());
    }

    @Override
    public void delete(FileDeletionWorkingSubset workingSubset, IDeletionProgressManager progressManager) {
        workingSubset.getFileDeletionRequests().forEach(request -> {
            String fileName = request.getFileReference().getMetaInfo().getFileName();
            if (Pattern.matches(deleteErrorFilePattern, fileName)) {
                progressManager.deletionFailed(request, "Specific error generated for tests");
            } else {
                progressManager.deletionSucceedWithPendingAction(request);
            }
        });
    }

    @Override
    public PreparationResponse<FileRestorationWorkingSubset, FileCacheRequestDto> prepareForRestoration(Collection<FileCacheRequestDto> requests) {
        Collection<FileRestorationWorkingSubset> workingSubSets = Lists.newArrayList();
        workingSubSets.add(new FileRestorationWorkingSubset(Sets.newHashSet(requests)));
        return PreparationResponse.build(workingSubSets, Maps.newHashMap());
    }

    @Override
    public void retrieve(FileRestorationWorkingSubset workingSubset, IRestorationProgressManager progressManager) {
        workingSubset.getFileRestorationRequests().forEach(f -> {
            if (Pattern.matches(restoErrorFilePattern, f.getFileReference().getMetaInfo().getFileName())) {
                progressManager.restoreFailed(f, "Specific error generated for tests");
            } else {
                // Create file
                try {
                    if (!Files.exists(Paths.get(f.getRestorationDirectory()))) {
                        Files.createDirectories(Paths.get(f.getRestorationDirectory()));
                    }
                    Path filePath = Paths.get(f.getRestorationDirectory(), f.getChecksum());
                    if (!Files.exists(filePath)) {
                        Files.createFile(filePath);
                        try (FileOutputStream out = new FileOutputStream(filePath.toFile())) {
                            byte[] bytes = new byte[1024];
                            new SecureRandom().nextBytes(bytes);
                            out.write(bytes);
                            out.flush();
                        }
                        LOGGER.info("Retrieve file with size {}", filePath.toFile().length());
                    }
                    if (isInternalCache()) {
                        progressManager.restoreSucceededInternalCache(f, filePath);
                    } else {
                        progressManager.restoreSucceededExternalCache(f,
                                                                      filePath.toAbsolutePath().toFile().toURL(),
                                                                      1024L,
                                                                      OffsetDateTime.now().plusDays(10));
                    }
                    checksumsRestored.add(f.getChecksum());
                } catch (IOException e) {
                    LOGGER.error(e.getMessage(), e);
                    progressManager.restoreFailed(f, e.getMessage());
                }
            }
        });
    }

    @Override
    public void runPeriodicAction(IPeriodicActionProgressManager progressManager) {
        LOG.info("Running task !!!!");
        fileRefRepo.findAll().stream().filter(f -> f.getLocation().isPendingActionRemaining()).forEach(f -> {
            progressManager.storagePendingActionSucceed(f.getLocation().getUrl());
        });
    }

    @Override
    public boolean allowPhysicalDeletion() {
        return true;
    }

    @Override
    public boolean isValidUrl(String urlToValidate, Set<String> errors) {
        return true;
    }

    @Override
    public boolean hasPeriodicAction() {
        return true;
    }

    @Override
    public InputStream download(FileReferenceWithoutOwnersDto fileReference)
        throws NearlineFileNotAvailableException, NearlineDownloadException {
        LOGGER.info("Download for id {}", id);
        if (fileReference.getLocation().getUrl() == null) {
            throw new NearlineFileNotAvailableException(String.format(
                "Unable to download file %s as url is not defined.",
                fileReference.getMetaInfo().getFileName()));
        }

        if (!checksumsAvailables.contains(fileReference.getChecksum())
            && !checksumsRestored.contains(fileReference.getChecksum())) {
            throw new NearlineFileNotAvailableException(String.format("File %s is not available for download (not "
                                                                      + "restored yet)", fileReference.getChecksum()));
        }

        URL urlToDownload;
        try {
            // Calculate url to download, if file is simulated as already exists creates it from local path
            // If not, the restored URL is already a valid URL
            if (checksumsAvailables.contains(fileReference.getChecksum())) {
                urlToDownload = new URL("file:///" + Paths.get(fileReference.getLocation().getUrl()).toAbsolutePath());
            } else {
                urlToDownload = new URL(fileReference.getLocation().getUrl());
            }
        } catch (MalformedURLException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RsRuntimeException("Invalid url");
        }
        try (InputStream fileInputStream = DownloadUtils.getInputStream(urlToDownload, new ArrayList<>())) {
            return fileInputStream;
        } catch (FileNotFoundException e) {
            LOGGER.warn(e.getMessage(), e);
            throw new NearlineFileNotAvailableException(String.format("File %s is not available.",
                                                                      fileReference.getMetaInfo().getFileName()));

        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new NearlineDownloadException(String.format("File %s cannot be download : %s",
                                                              fileReference.getMetaInfo().getFileName(),
                                                              e.getMessage()));
        }
    }

    @Override
    public boolean isInternalCache() {
        return !externalCache;
    }

    public void simulateAvailableFileForDownload(String checksum) {
        LOGGER.info("Add for id {}", id);
        checksumsAvailables.add(checksum);
    }
}
