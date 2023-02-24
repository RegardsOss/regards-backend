/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.modules.storage.dao.IFileReferenceRepository;
import fr.cnes.regards.modules.storage.domain.database.request.FileCacheRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileDeletionRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequest;
import fr.cnes.regards.modules.storage.domain.plugin.*;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Set;
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

    /**
     * Plugin parameter name of the storage base location as a string
     */
    public static final String BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME = "Storage_URL";

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

    /**
     * Plugin init method
     */
    @PluginInit
    public void init() throws IOException {
        // Clear directory
        FileUtils.deleteDirectory(Paths.get(BASE_URL).toFile());
    }

    @Override
    public PreparationResponse<FileStorageWorkingSubset, FileStorageRequest> prepareForStorage(Collection<FileStorageRequest> FileReferenceRequest) {
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
            doStore(progressManager, data);
        });
    }

    private void doStore(IStorageProgressManager progressManager, FileStorageRequest fileRefRequest) {

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
            return;
        } else if (Pattern.matches(errorFilePattern, fileName)) {
            LOGGER.error("Simulated error for file storage {}.", fileName);
            progressManager.storageFailed(fileRefRequest, "Specific error generated for tests");
        } else {
            String directory;
            if (fileRefRequest.getStorageSubDirectory() == null) {
                directory = fileRefRequest.getMetaInfo().getChecksum().substring(0, 5);
            } else {
                directory = fileRefRequest.getStorageSubDirectory();
            }
            String storedUrl = String.format("%s%s",
                                             BASE_URL,
                                             Paths.get("/", directory, fileRefRequest.getMetaInfo().getChecksum())
                                                  .toString());
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
                    progressManager.storageSucceedWithPendingActionRemaining(fileRefRequest,
                                                                             new URL("file", null, storedUrl),
                                                                             1024L,
                                                                             true);
                } else {
                    LOGGER.info("Simulated success for file storage {}.", fileName);
                    progressManager.storageSucceed(fileRefRequest, new URL("file", null, storedUrl), 1024L);
                }
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
                progressManager.storageFailed(fileRefRequest, e.getMessage());
            }
        }
    }

    @Override
    public PreparationResponse<FileDeletionWorkingSubset, FileDeletionRequest> prepareForDeletion(Collection<FileDeletionRequest> fileDeletionRequests) {
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
                progressManager.deletionSucceed(request);
            }
        });
    }

    @Override
    public PreparationResponse<FileRestorationWorkingSubset, FileCacheRequest> prepareForRestoration(Collection<FileCacheRequest> requests) {
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
                    progressManager.restoreSucceed(f, filePath);
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

}
