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
package fr.cnes.regards.modules.storage.plugin.datastorage.local;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.utils.file.DownloadUtils;
import fr.cnes.regards.modules.storage.domain.StorageDataFileUtils;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.plugin.DataStorageAccessModeEnum;
import fr.cnes.regards.modules.storage.domain.plugin.IOnlineDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.IProgressManager;
import fr.cnes.regards.modules.storage.domain.plugin.PluginConfUpdatable;
import fr.cnes.regards.modules.storage.domain.plugin.WorkingSubsetWrapper;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@Plugin(author = "REGARDS Team", description = "Plugin handling the storage on local file system", id = "Local",
        version = "1.0", contact = "regards@c-s.fr", licence = "GPLv3", owner = "CNES",
        url = "https://regardsoss.github.io/")
public class LocalDataStorage implements IOnlineDataStorage<LocalWorkingSubset> {

    /**
     * Plugin parameter name of the storage base location as a string
     */
    public static final String BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME = "Storage_URL";

    /**
     * Plugin parameter name of the can delete attribute
     */
    public static final String LOCAL_STORAGE_DELETE_OPTION = "Local_Delete_Option";

    /**
     * Plugin parameter name of the total space allowed
     */
    public static final String LOCAL_STORAGE_TOTAL_SPACE = "Local_Total_Space";

    /**
     * Plugin parameter subdirectory to store AIP files
     */
    public static final String AIP_STORAGE_SUBDIRECTORY = "aipStorageSubdirectory";

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(LocalDataStorage.class);

    /**
     * {@link IRuntimeTenantResolver} instance
     */
    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Base storage location url
     */
    @PluginParameter(name = BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME, description = "Base storage location url to use",
            label = "Base storage location url")
    private String baseStorageLocationAsString;

    /**
     * Sub directoriy where to store AIP files if the storage directory is provided.
     */
    @PluginParameter(name = AIP_STORAGE_SUBDIRECTORY,
            description = "Subdirectory used to store AIP files if the store directory is provided.",
            defaultValue = "AIPS", label = "Subdirectory to store AIP files")
    private String aipSubDirectory;

    /**
     * can this data storage delete files or not?
     */
    @PluginParameter(name = LOCAL_STORAGE_DELETE_OPTION, defaultValue = "true",
            description = "Can this data storage delete files or not?", label = "Deletion option")
    private Boolean canDelete;

    /**
     * Total space, in byte, this data storage is allowed to use
     */
    @PluginParameter(name = LOCAL_STORAGE_TOTAL_SPACE,
            description = "Total space, in byte, this data storage is allowed to use", label = "Total allocated space")
    private Long totalSpace;

    /**
     * storage base location as url
     */
    private URL baseStorageLocation;

    /**
     * Plugin init method
     */
    @PluginInit
    public void init() throws MalformedURLException {
        baseStorageLocation = new URL(baseStorageLocationAsString);
    }

    @Override
    public WorkingSubsetWrapper<LocalWorkingSubset> prepare(Collection<StorageDataFile> dataFiles,
            DataStorageAccessModeEnum mode) {
        // We choose to use a simple parallel stream to store file on file system, so for now we treat everything at once
        WorkingSubsetWrapper<LocalWorkingSubset> wrapper = new WorkingSubsetWrapper<>();
        wrapper.getWorkingSubSets().add(new LocalWorkingSubset(Sets.newHashSet(dataFiles)));
        return wrapper;
    }

    @Override
    public boolean canDelete() {
        return canDelete;
    }

    @Override
    public void store(LocalWorkingSubset workingSubset, Boolean replaceMode, IProgressManager progressManager) {
        // because we use a parallel stream, we need to get the tenant now and force it before each doStore call
        String tenant = runtimeTenantResolver.getTenant();
        workingSubset.getDataFiles().stream().forEach(data -> {
            runtimeTenantResolver.forceTenant(tenant);
            doStore(progressManager, data, replaceMode);
        });
    }

    @Override
    public Long getTotalSpace() {
        return totalSpace;
    }

    private void doStore(IProgressManager progressManager, StorageDataFile data, Boolean replaceMode) {
        String fullPathToFile;
        try {
            fullPathToFile = getStorageLocation(data);
        } catch (IOException ioe) {
            String failureCause = String.format(
                                                "Storage of StorageDataFile(%s) failed due to the following IOException: %s",
                                                data.getChecksum(), ioe.toString());
            LOG.error(failureCause, ioe);
            progressManager.storageFailed(data, Optional.empty(), failureCause);
            return;
        }
        //check if file is already at the right place or not. Unless we are instructed not to(for updates for example)
        if (!replaceMode && Files.exists(Paths.get(fullPathToFile))) {
            Long fileSize = Paths.get(fullPathToFile).toFile().length();
            data.setFileSize(fileSize);
            //if it is, there is nothing to move/copy, we just need to say to the system that the file is stored successfully
            try {
                progressManager.storageSucceed(data, new URL("file", "", fullPathToFile), fileSize);
            } catch (MalformedURLException e) {
                LOG.error(e.getMessage(), e);
            }
            return;
        }

        try {
            URL sourceUrl = StorageDataFileUtils.getAccessibleUrl(data);
            if (sourceUrl == null) {
                StringJoiner stringedUrls = new StringJoiner(",");
                data.getUrls().forEach(url -> stringedUrls.add(url.toExternalForm()));
                String errorMsg = String.format(
                                                "Error trying to retrieve file(checksum: %s). We could not find any accessible url(Actual urls: %s)",
                                                data.getChecksum(), stringedUrls.toString());
                LOG.error(errorMsg);
                progressManager.storageFailed(data, Optional.of(new URL(fullPathToFile)), errorMsg);
                return;
            }
            boolean downloadOk = DownloadUtils.downloadAndCheckChecksum(sourceUrl, Paths.get(fullPathToFile),
                                                                        data.getAlgorithm(), data.getChecksum());
            if (!downloadOk) {
                String failureCause = String.format(
                                                    "Storage of StorageDataFile(%s) failed at the following location: %s. Its checksum once stored do not match with expected",
                                                    data.getChecksum(), fullPathToFile);
                Files.deleteIfExists(Paths.get(fullPathToFile));
                progressManager.storageFailed(data, Optional.of(new URL("file", "", fullPathToFile)), failureCause);
            } else {
                File file = Paths.get(fullPathToFile).toFile();
                if (file.canWrite()) {
                    file.setReadOnly();
                }
                Long fileSize = file.length();
                data.setFileSize(fileSize);
                progressManager.storageSucceed(data, new URL("file", "", fullPathToFile), fileSize);
            }
        } catch (NoSuchAlgorithmException e) {
            RuntimeException re = new RuntimeException(e);
            LOG.error("This is a development exception, if you see it in production, go get your dev(s) and spank them!!!!",
                      re);
            throw re;
        } catch (IOException ioe) {
            String failureCause = String.format(
                                                "Storage of StorageDataFile(%s) failed due to the following IOException: %s",
                                                data.getChecksum(), ioe.toString());
            LOG.error(failureCause, ioe);
            Paths.get(fullPathToFile).toFile().delete();
            progressManager.storageFailed(data, Optional.empty(), failureCause);
        }
    }

    private String getStorageLocation(StorageDataFile data) throws IOException {
        String checksum = data.getChecksum();
        String storageLocation = baseStorageLocation.getPath() + "/";
        if (data.getStorageDirectory() != null) {
            // Storage directory is provider. use it
            storageLocation = storageLocation + data.getStorageDirectory();
            if (data.getDataType().equals(DataType.AIP)) {
                storageLocation = storageLocation + "/" + this.aipSubDirectory;
            }
        } else {
            // Storage directory is not provided, generate new one with checksum
            storageLocation = storageLocation + checksum.substring(0, 3);
        }
        if (!Paths.get(storageLocation).toFile().exists()) {
            Files.createDirectories(Paths.get(storageLocation));
        }
        // files are stored with the checksum as their name and their extension is based on the url, first '.' after the last '/' of the url
        return storageLocation + "/" + checksum;
    }

    @Override
    public void delete(LocalWorkingSubset workingSubset, IProgressManager progressManager) {
        for (StorageDataFile data : workingSubset.getDataFiles()) {
            try {
                Path location = Paths.get(getStorageLocation(data));
                Files.deleteIfExists(location);
                progressManager.deletionSucceed(data, location.toUri().toURL());
            } catch (IOException ioe) {
                String failureCause = String.format(
                                                    "Deletion of StorageDataFile(%s) failed due to the following IOException: %s",
                                                    data.getChecksum(), ioe.getMessage());
                LOG.error(failureCause, ioe);
                progressManager.deletionFailed(data, Optional.empty(), failureCause);
            }
        }
    }

    @Override
    public InputStream retrieve(StorageDataFile data) throws IOException {
        return Files.newInputStream(Paths.get(getStorageLocation(data)));
    }

    @Override
    public PluginConfUpdatable allowConfigurationUpdate(PluginConfiguration newConfiguration,
            PluginConfiguration currentConfiguration, boolean filesAlreadyStored) {
        // Only the baseStorageDirectory cannot be changed if files are already stored
        String currentLocation = currentConfiguration.getParameterValue(BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME);
        String newLocation = newConfiguration.getParameterValue(BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME);
        if (filesAlreadyStored && !currentLocation.equals(newLocation)) {
            return PluginConfUpdatable.preventUpdate(String
                    .format("Files are already stored in the base location %s. You can't modify this parameter. Maybe you want to create a new configuration for the %s location?",
                            currentLocation, newLocation));
        } else {
            return PluginConfUpdatable.allowUpdate();
        }

    }

    @Override
    public Map<String, Object> getDiagnosticInfo() {
        Map<String, Object> diagInfo = new HashMap<>();
        diagInfo.put("Plugin Id", "Local");
        return diagInfo;
    }
}
