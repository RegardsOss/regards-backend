/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.local;

import java.io.IOException;
import java.io.InputStream;
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
import com.google.gson.Gson;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.utils.file.DownloadUtils;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.plugin.DataStorageAccessModeEnum;
import fr.cnes.regards.modules.storage.plugin.DataStorageInfo;
import fr.cnes.regards.modules.storage.plugin.IOnlineDataStorage;
import fr.cnes.regards.modules.storage.plugin.IProgressManager;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@Plugin(author = "REGARDS Team", description = "Plugin handling the storage on local file system",
        id = "LocalDataStorage", version = "1.0", contact = "regards@c-s.fr", licence = "GPLv3", owner = "CNES",
        url = "https://regardsoss.github.io/")
public class LocalDataStorage implements IOnlineDataStorage<LocalWorkingSubset> {

    private static final Logger LOG = LoggerFactory.getLogger(LocalDataStorage.class);

    public static final String BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME = "Storage_URL";

    @Autowired
    private Gson gson;

    @PluginParameter(name = BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME)
    private String baseStorageLocationAsString;

    private URL baseStorageLocation;

    @PluginInit
    public void init() {
        baseStorageLocation = gson.fromJson(baseStorageLocationAsString, URL.class);
    }

    @Override
    public Set<LocalWorkingSubset> prepare(Collection<DataFile> dataFiles, DataStorageAccessModeEnum mode) {
        // We choose to use a simple parallel stream to store file on file system, so for now we treat everything at once
        return Sets.newHashSet(new LocalWorkingSubset(Sets.newHashSet(dataFiles)));
    }

    @Override
    public void store(LocalWorkingSubset workingSubset, Boolean replaceMode, IProgressManager progressManager) {
        workingSubset.getDataFiles().parallelStream().forEach(data -> doStore(progressManager, data, replaceMode));
    }

    private void doStore(IProgressManager progressManager, DataFile data, Boolean replaceMode) {
        String fullPathToFile;
        try {
            fullPathToFile = getStorageLocation(data);
            //check if file is already at the right place or not. Unless we are instructed not to(for updates for example)
            if (!replaceMode && Paths.get(fullPathToFile).equals(Paths.get(data.getUrl().getPath()))) {
                Long fileSize = Paths.get(fullPathToFile).toFile().length();
                data.setFileSize(fileSize);
                //if it is, there is nothing to move/copy, we just need to say to the system that the file is stored successfully
                progressManager.storageSucceed(data, data.getUrl(), fileSize);
                return;
            }
        } catch (IOException ioe) {
            String failureCause = String.format("Storage of DataFile(%s) failed due to the following IOException: %s",
                                                data.getChecksum(), ioe.getMessage());
            LOG.error(failureCause, ioe);
            progressManager.storageFailed(data, failureCause);
            return;
        }
        try {
            boolean downloadOk = DownloadUtils.downloadAndCheckChecksum(data.getUrl(), Paths.get(fullPathToFile),
                                                                        data.getAlgorithm(), data.getChecksum());
            if (!downloadOk) {
                String failureCause = String.format(
                                                    "Storage of DataFile(%s) failed at the following location: %s. Its checksum once stored do not match with expected",
                                                    data.getChecksum(), fullPathToFile);
                Files.deleteIfExists(Paths.get(fullPathToFile));
                progressManager.storageFailed(data, failureCause);
            } else {
                Long fileSize = Paths.get(fullPathToFile).toFile().length();
                data.setFileSize(fileSize);
                progressManager.storageSucceed(data, new URL("file", "", fullPathToFile), fileSize);
            }
        } catch (NoSuchAlgorithmException e) {
            RuntimeException re = new RuntimeException(e);
            LOG.error("This is a development exception, if you see it in production, go get your dev(s) and spank them!!!!",
                      re);
            throw re;
        } catch (IOException ioe) {
            String failureCause = String.format("Storage of DataFile(%s) failed due to the following IOException: %s",
                                                data.getChecksum(), ioe.getMessage());
            LOG.error(failureCause, ioe);
            Paths.get(fullPathToFile).toFile().delete();
            progressManager.storageFailed(data, failureCause);
        }
    }

    private String getStorageLocation(DataFile data) throws IOException {
        String checksum = data.getChecksum();
        String storageLocation = baseStorageLocation.getPath() + "/" + checksum.substring(0, 3);
        if (!Files.exists(Paths.get(storageLocation))) {
            Files.createDirectories(Paths.get(storageLocation));
        }
        // files are stored with the checksum as their name and their extension is based on the url, first '.' after the last '/' of the url
        String fullPathToFile = storageLocation + "/" + checksum + getExtension(data.getUrl());
        return fullPathToFile;
    }

    /**
     * does not take into account hidden files
     *
     * @param originUrl
     * @return the extension with '.' prefixing it or "" in case of hidden files and files without extension
     */
    private String getExtension(URL originUrl) {
        String[] pathParts = originUrl.getPath().split("/");
        String fileName = pathParts[pathParts.length - 1];
        int i;
        if ((i = fileName.indexOf('.')) > 0) {
            return fileName.substring(i);
        }
        return "";
    }

    @Override
    public void delete(Set<DataFile> dataFiles, IProgressManager progressManager) {
        for (DataFile data : dataFiles) {
            try {
                Files.deleteIfExists(Paths.get(getStorageLocation(data)));
                progressManager.deletionSucceed(data);
            } catch (IOException ioe) {
                String failureCause = String.format(
                                                    "Storage of DataFile(%s) failed due to the following IOException: %s",
                                                    data.getChecksum(), ioe.getMessage());
                LOG.error(failureCause, ioe);
                progressManager.deletionFailed(data, failureCause);
            }
        }
    }

    @Override
    public Set<DataStorageInfo> getMonitoringInfos() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InputStream retrieve(DataFile data) throws IOException {
        return Files.newInputStream(Paths.get(getStorageLocation(data)));
    }
}
