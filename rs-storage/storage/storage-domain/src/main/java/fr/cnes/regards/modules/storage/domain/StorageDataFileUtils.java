package fr.cnes.regards.modules.storage.domain;

import java.net.URL;
import java.util.Optional;

import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class StorageDataFileUtils {

    public static final String FILE_PROTOCOL = "file";

    /**
     * Allows to extract one of the accessible urls from {@link StorageDataFile} urls
     * @param file {@link StorageDataFile}
     * @return one of the accessible urls, for now with protocol file, null otherwise
     */
    public static URL getAccessibleUrl(StorageDataFile file) {
        Optional<URL> urlOpt = file.getUrls().stream().filter(url -> FILE_PROTOCOL.equals(url.getProtocol())).findAny();
        if (urlOpt.isPresent()) {
            return urlOpt.get();
        } else {
            return null;
        }
    }
}
