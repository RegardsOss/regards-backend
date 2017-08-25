/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.local;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import fr.cnes.regards.framework.file.utils.ChecksumUtils;
import fr.cnes.regards.framework.file.utils.DownloadUtils;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.plugin.DataStorageInfo;
import fr.cnes.regards.modules.storage.plugin.IOnlineDataStorage;
import fr.cnes.regards.modules.storage.plugin.ProgressManager;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@Plugin(author = "REGARDS Team", description = "Plugin handling the storage on local file system",
        id = "LocalDataStorage", version = "1.0", contact = "regards@c-s.fr", licence = "GPLv3", owner = "CNES",
        url = "https://regardsoss.github.io/")
public class LocalDataStorage implements IOnlineDataStorage<LocalWorkingSubset> {

    private static final Charset STORAGE_ENCODING = StandardCharsets.UTF_8;

    private static final Logger LOG = LoggerFactory.getLogger(LocalDataStorage.class);

    public static final String BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME = "leParameter";

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
    public Set<LocalWorkingSubset> prepare(Multimap<AIP, List<DataFile>> pAips) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void store(LocalWorkingSubset workingSubset, Boolean replaceMode, ProgressManager progressManager)
            throws IOException {
        for (DataFile data : workingSubset.getDataFiles()) {
            String fullPathToFile = getStorageLocation(data);
            BufferedWriter writer = Files
                    .newBufferedWriter(Paths.get(fullPathToFile), STORAGE_ENCODING, StandardOpenOption.CREATE);
            InputStream sourceStream = DownloadUtils.download(data.getOriginUrl());
            int read;
            while ((read = sourceStream.read()) != -1) {
                writer.write(read);
            }
            writer.flush();
            writer.close();
            // Now that it is stored, lets checked that it is correctly stored!
            try (InputStream is = Files.newInputStream(Paths.get(fullPathToFile));
                    DigestInputStream dis = new DigestInputStream(is, MessageDigest.getInstance("MD5"))) {
                while (dis.read() != -1) {
                }
                String fileChecksum = ChecksumUtils.getHexChecksum(dis.getMessageDigest().digest());
                if (!fileChecksum.equals(data.getChecksum())) {
                    String failureCause = String
                            .format("Storage of DataFile(%s) failed at the following location: %s. Its checksum once stored do not match with expected",
                                    data.getChecksum(), fullPathToFile);
                    Files.deleteIfExists(Paths.get(fullPathToFile));
                    // FIXME: progressManager.storageFailed(data.getAip(), data, failureCause);
                }
            } catch (NoSuchAlgorithmException e) {
                RuntimeException re = new RuntimeException(e);
                LOG.error(
                        "This is a development exception, if you see it in production, go get your dev(s) and spank them!!!!",
                        re);
                throw re;
            }
        }
    }

    private String getStorageLocation(DataFile data) throws IOException {
        String checksum=data.getChecksum();
        String storageLocation = baseStorageLocation.getPath() + "/" + checksum.substring(0, 3);
        Files.createDirectory(Paths.get(storageLocation));
        // files are stored with the checksum as their name and their extension is based on their MIMEType
        String fullPathToFile =
                storageLocation + "/" + checksum + "." + data.getMimeType().getSubtype().toLowerCase();
        return fullPathToFile;
    }

    @Override
    public void retrieve(LocalWorkingSubset pWorkingSubset, ProgressManager pProgressManager) {
        // TODO Auto-generated method stub
    }

    @Override
    public void delete(LocalWorkingSubset workingSubset, ProgressManager pProgressManager) throws IOException {
        for(DataFile data : workingSubset.getDataFiles()) {
            Files.deleteIfExists(Paths.get(getStorageLocation(data)));
        }
    }

    @Override
    public Set<DataStorageInfo> getMonitoringInfos() {
        // TODO Auto-generated method stub
        return null;
    }

}
