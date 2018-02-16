/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.datastorage.local;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.file.DownloadUtils;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.plugin.DataStorageAccessModeEnum;
import fr.cnes.regards.modules.storage.domain.plugin.IOnlineDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.IProgressManager;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@Plugin(author = "REGARDS Team", description = "Plugin handling the storage on local file system",
        id = "LocalDataStorage", version = "1.0", contact = "regards@c-s.fr", licence = "GPLv3", owner = "CNES",
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
    public Set<LocalWorkingSubset> prepare(Collection<StorageDataFile> dataFiles, DataStorageAccessModeEnum mode) {
        // We choose to use a simple parallel stream to store file on file system, so for now we treat everything at once
        return Sets.newHashSet(new LocalWorkingSubset(Sets.newHashSet(dataFiles)));
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
            //check if file is already at the right place or not. Unless we are instructed not to(for updates for example)
            if (!replaceMode && data.getUrls().stream().map(url -> Paths.get(url.getPath()))
                    .filter(path -> Paths.get(fullPathToFile).equals(path)).count() != 0) {
                Long fileSize = Paths.get(fullPathToFile).toFile().length();
                data.setFileSize(fileSize);
                //if it is, there is nothing to move/copy, we just need to say to the system that the file is stored successfully
                progressManager.storageSucceed(data, new URL("file", "", fullPathToFile), fileSize);
                return;
            }
        } catch (IOException ioe) {
            String failureCause = String.format(
                    "Storage of StorageDataFile(%s) failed due to the following IOException: %s",
                    data.getChecksum(),
                    ioe.toString());
            LOG.error(failureCause, ioe);
            progressManager.storageFailed(data, failureCause);
            return;
        }
        try {
            URL sourceUrl = data.getUrls().stream().filter(url -> url.getProtocol().equals("file")).findAny().get();
            boolean downloadOk = DownloadUtils.downloadAndCheckChecksum(sourceUrl,
                                                                        Paths.get(fullPathToFile),
                                                                        data.getAlgorithm(),
                                                                        data.getChecksum());
            if (!downloadOk) {
                String failureCause = String.format(
                        "Storage of StorageDataFile(%s) failed at the following location: %s. Its checksum once stored do not match with expected",
                        data.getChecksum(),
                        fullPathToFile);
                Files.deleteIfExists(Paths.get(fullPathToFile));
                progressManager.storageFailed(data, failureCause);
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
            LOG.error(
                    "This is a development exception, if you see it in production, go get your dev(s) and spank them!!!!",
                    re);
            throw re;
        } catch (IOException ioe) {
            String failureCause = String.format(
                    "Storage of StorageDataFile(%s) failed due to the following IOException: %s",
                    data.getChecksum(),
                    ioe.toString());
            LOG.error(failureCause, ioe);
            Paths.get(fullPathToFile).toFile().delete();
            progressManager.storageFailed(data, failureCause);
        }
    }

    private String getStorageLocation(StorageDataFile data) throws IOException {
        String checksum = data.getChecksum();
        String storageLocation = baseStorageLocation.getPath() + "/" + checksum.substring(0, 3);
        if (!Paths.get(storageLocation).toFile().exists()) {
            Files.createDirectories(Paths.get(storageLocation));
        }
        // files are stored with the checksum as their name and their extension is based on the url, first '.' after the last '/' of the url
        return storageLocation + "/" + checksum;
    }

    @Override
    public void delete(Set<StorageDataFile> dataFiles, IProgressManager progressManager) {
        for (StorageDataFile data : dataFiles) {
            try {
                Files.deleteIfExists(Paths.get(getStorageLocation(data)));
                progressManager.deletionSucceed(data);
            } catch (IOException ioe) {
                String failureCause = String.format(
                        "Deletion of StorageDataFile(%s) failed due to the following IOException: %s",
                        data.getChecksum(),
                        ioe.getMessage());
                LOG.error(failureCause, ioe);
                progressManager.deletionFailed(data, failureCause);
            }
        }
    }

    @Override
    public InputStream retrieve(StorageDataFile data) throws IOException {
        return Files.newInputStream(Paths.get(getStorageLocation(data)));
    }
}
