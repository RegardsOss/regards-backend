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
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.plugin.DataStorageInfo;
import fr.cnes.regards.modules.storage.plugin.DataStorageType;
import fr.cnes.regards.modules.storage.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.plugin.ProgressManager;
import fr.cnes.regards.modules.storage.plugin.exception.StorageCorruptedException;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@Plugin(author = "REGARDS Team", description = "Plugin handling the storage on local file system",
        id = "LocalDataStorage", version = "1.0", contact = "regards@c-s.fr", licence = "GPLv3", owner = "CNES",
        url = "https://regardsoss.github.io/")
public class LocalDataStorage implements IDataStorage<LocalWorkingSubset> {

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
    public DataStorageType getType() {
        return DataStorageType.ONLINE;
    }

    public AIP storeMetadata(AIP aip) throws IOException, StorageCorruptedException {
        // We store the jsonified version of aip, so lets get the aip as a json string
        String aipJsonString = gson.toJson(aip);

        // We are storing the aip, it means that only us know under which form it is stored, so we are the only ones that knows the encoding used and on what the checksum should be calculated.
        try {
            // We are specifying the storage encoding and not the encoding of the attribute checksum for a simple reason:
            // the encoding of the file is important while the encoding used for the attribute is not.
            // Moreover, we better let the jvm handle attribute encoding and convert things as it is used to do.
            String checksum = ChecksumUtils
                    .getHexChecksum(MessageDigest.getInstance("MD5").digest(aipJsonString.getBytes(STORAGE_ENCODING)));

            aip.setChecksum(checksum);
            // Lets compute the filename: baseStorageLocation+3 first char of checksum+checksum
            // This is the IDataStorage implementation for local file system, that means the url should not contain a host part or it is localhost.
            // So the path to store the file is simply the path part or the URI.
            // We assume that baseStorageLocation already exists on the file system.
            // We just need to create if it does not already exist the directory between baseStorageLocation and the file.
            String storageLocation = baseStorageLocation.getPath() + "/" + checksum.substring(0, 3);
            Files.createDirectory(Paths.get(storageLocation));
            String fullPathToFile = storageLocation + "/" + checksum + ".json";
            BufferedWriter writer = Files.newBufferedWriter(Paths.get(fullPathToFile), STORAGE_ENCODING,
                                                            StandardOpenOption.CREATE);
            writer.write(aipJsonString);
            writer.flush();
            writer.close();
            // Now that it is stored, lets checked that it is correctly stored!
            try (InputStream is = Files.newInputStream(Paths.get(fullPathToFile));
                    DigestInputStream dis = new DigestInputStream(is, MessageDigest.getInstance("MD5"))) {
                while (dis.read() != -1) {
                }
                String fileChecksum = ChecksumUtils.getHexChecksum(dis.getMessageDigest().digest());
                if (!fileChecksum.equals(aip.getChecksum())) {
                    StorageCorruptedException e = new StorageCorruptedException(
                            "Storage of AIP metadata(" + aip.getIpId() + ") failed at the following location: "
                                    + fullPathToFile + ". Its checksum once stored do not match with expected");
                    LOG.error(e.getMessage(), e);
                    Files.deleteIfExists(Paths.get(fullPathToFile));
                    throw e;
                }
            }
        } catch (NoSuchAlgorithmException e) {
            RuntimeException re = new RuntimeException(e);
            LOG.error("This is a development exception, if you see it in production, go get your dev(s) and spank them!!!!",
                      re);
            throw re;
        }
        return aip;
    }

    public void checkIntegrity(AIP aip) throws NoSuchAlgorithmException, IOException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        //        md5.digest()
        try (InputStream fileIs = Files.newInputStream(Paths.get(baseStorageLocationAsString));
                DigestInputStream fileDis = new DigestInputStream(fileIs, md5);) {
            while (fileDis.read() != -1) {
            }
            byte[] checksum = fileDis.getMessageDigest().digest();
        }
        InputStream aipIs = new ByteArrayInputStream(gson.toJson(aip).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Set<LocalWorkingSubset> prepare(Multimap<AIP, List<DataFile>> pAips) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void store(LocalWorkingSubset pWorkingSubset, Boolean pReplaceMode, ProgressManager pProgressManager) {
        // TODO Auto-generated method stub

    }

    @Override
    public void retrieve(LocalWorkingSubset pWorkingSubset, ProgressManager pProgressManager) {
        // TODO Auto-generated method stub

    }

    @Override
    public void delete(LocalWorkingSubset pWorkingSubset, ProgressManager pProgressManager) {
        // TODO Auto-generated method stub

    }

    @Override
    public Set<DataStorageInfo> getMonitoringInfos() {
        // TODO Auto-generated method stub
        return null;
    }

}
