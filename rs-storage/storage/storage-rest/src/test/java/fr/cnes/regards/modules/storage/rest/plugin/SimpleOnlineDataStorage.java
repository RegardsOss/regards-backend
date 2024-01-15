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
package fr.cnes.regards.modules.storage.rest.plugin;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.fileaccess.plugin.domain.*;
import fr.cnes.regards.modules.fileaccess.plugin.dto.FileCacheRequestDto;
import fr.cnes.regards.modules.fileaccess.plugin.dto.FileDeletionRequestDto;
import fr.cnes.regards.modules.filecatalog.dto.FileReferenceWithoutOwnersDto;
import fr.cnes.regards.modules.filecatalog.dto.request.FileStorageRequestAggregationDto;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequestAggregation;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Binda sébastien
 */
@Plugin(author = "REGARDS Team",
        description = "Plugin handling the storage on local file system",
        id = "SimpleOnlineTest",
        version = "1.0",
        contact = "regards@c-s.fr",
        license = "GPLv3",
        owner = "CNES",
        url = "https://regardsoss.github.io/")
public class SimpleOnlineDataStorage implements IOnlineStorageLocation {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleOnlineDataStorage.class);

    /**
     * Plugin parameter name of the storage base location as a string
     */
    public static final String BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME = "Storage_URL";

    public static final String HANDLE_STORAGE_ERROR_FILE_PATTERN = "error_file_pattern";

    public static final String HANDLE_DELETE_ERROR_FILE_PATTERN = "delete_error_file_pattern";

    /**
     * {@link IRuntimeTenantResolver} instance
     */
    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

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

    @PluginParameter(name = HANDLE_DELETE_ERROR_FILE_PATTERN,
                     description = "Delete Error file pattern",
                     label = "Delete Error file pattern")
    private String deleteErrorFilePattern;

    private final String doNotHandlePattern = "doNotHandle.*";

    /**
     * Plugin init method
     */
    @PluginInit
    public void init() throws IOException {
        // Clear directory
        FileUtils.deleteDirectory(Paths.get(baseStorageLocationAsString).toFile());
    }

    @Override
    public PreparationResponse<FileStorageWorkingSubset, FileStorageRequestAggregationDto> prepareForStorage(Collection<FileStorageRequestAggregationDto> fileReferenceRequests) {
        Collection<FileStorageWorkingSubset> workingSubSets = Lists.newArrayList();
        workingSubSets.add(new FileStorageWorkingSubset(fileReferenceRequests));
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
            return;
        } else if (Pattern.matches(errorFilePattern, fileName)) {
            progressManager.storageFailed(fileRefRequest.toDto(), "Specific error generated for tests");
        } else {
            String directory;
            if (fileRefRequest.getStorageSubDirectory() == null) {
                directory = fileRefRequest.getMetaInfo().getChecksum().substring(0, 5);
            } else {
                directory = fileRefRequest.getStorageSubDirectory();
            }
            String storedUrl = String.format("%s%s",
                                             baseStorageLocationAsString,
                                             Paths.get("/", directory, fileRefRequest.getMetaInfo().getChecksum())
                                                  .toString());

            try {
                String originFilePath = new URL(fileRefRequest.getOriginUrl()).getPath();
                if (Files.notExists(Paths.get(storedUrl).getParent())) {
                    Files.createDirectories(Paths.get(storedUrl).getParent());
                }
                if (Files.notExists(Paths.get(originFilePath))) {
                    progressManager.storageFailed(fileRefRequest.toDto(),
                                                  String.format(
                                                      "Origine file %s to store does not exists or is not readable.",
                                                      fileRefRequest.getOriginUrl()));
                } else {
                    if (Files.notExists(Paths.get(storedUrl))) {
                        Files.copy(Paths.get(originFilePath), Paths.get(storedUrl));
                    }
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
                progressManager.deletionFailed(request, "Test deletion failure");
            } else {
                progressManager.deletionSucceed(request);
            }
        });
    }

    @Override
    public InputStream retrieve(FileReferenceWithoutOwnersDto fileRef) throws ModuleException {
        try {
            return (new URL(fileRef.getLocation().getUrl())).openStream();
        } catch (IOException e) {
            String errorMessage = String.format("[TEST STORAGE PLUGIN] file %s is not a valid URL to retrieve.",
                                                fileRef.getLocation().getUrl());
            LOGGER.error(errorMessage, e);
            throw new ModuleException(errorMessage);
        }
    }

    @Override
    public PreparationResponse<FileRestorationWorkingSubset, FileCacheRequestDto> prepareForRestoration(Collection<FileCacheRequestDto> requests) {
        Collection<FileRestorationWorkingSubset> workingSubSets = Lists.newArrayList();
        workingSubSets.add(new FileRestorationWorkingSubset(Sets.newHashSet(requests)));
        return PreparationResponse.build(workingSubSets, Maps.newHashMap());
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
