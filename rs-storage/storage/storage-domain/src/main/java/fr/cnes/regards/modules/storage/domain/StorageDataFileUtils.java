package fr.cnes.regards.modules.storage.domain;

import java.net.URL;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.Sets;

import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class StorageDataFileUtils {

    public static final String FILE_PROTOCOL = "file";

    public static final String HTTP_PROTOCOL = "http";

    public static final String HTTPS_PROTOCOL = "https";

    public static final Set<String> PROTOCOLS = Sets.newHashSet(FILE_PROTOCOL, HTTP_PROTOCOL, HTTPS_PROTOCOL);

    /**
     * Allows to extract one of the accessible urls from {@link StorageDataFile} urls
     * @param file {@link StorageDataFile}
     * @return one of the accessible urls, for now with protocol file, null otherwise
     */
    public static URL getAccessibleUrl(StorageDataFile file) {
        Optional<URL> urlOpt = file.getUrls().stream().filter(url -> PROTOCOLS.contains(url.getProtocol())).findAny();
        if (urlOpt.isPresent()) {
            return urlOpt.get();
        } else {
            return null;
        }
    }
}
