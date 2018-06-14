package fr.cnes.regards.framework.utils;

import java.io.IOException;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.encryption.BlowfishEncryptionService;
import fr.cnes.regards.framework.encryption.configuration.CipherProperties;
import fr.cnes.regards.framework.encryption.exception.EncryptionException;

/**
 * Main class
 *
 * @author svissier
 */
public class EncryptionApp {

    private static final java.lang.String KEY_LOCATION = "regards.cipher.key-location";

    private static final java.lang.String IV = "regards.cipher.iv";

    private static final Logger LOG = LoggerFactory.getLogger(EncryptionApp.class);

    private static final String VALUE = "value";

    public static void main(String[] args)
            throws InvalidAlgorithmParameterException, InvalidKeyException, IOException, EncryptionException {
        String keyLocation = System.getProperty(KEY_LOCATION, null);
        String iv = System.getProperty(IV, null);
        String value = System.getProperty(VALUE, null);
        if (keyLocation == null) {
            LOG.error("{} parameter is missing", KEY_LOCATION);
            System.exit(1);
        }
        if (iv == null) {
            LOG.error("{} parameter is missing", IV);
            System.exit(1);
        }
        if (value == null) {
            LOG.error("{} parameter is missing", VALUE);
            System.exit(1);
        }
        BlowfishEncryptionService blowfishEncryptionService = new BlowfishEncryptionService();
        blowfishEncryptionService.init(new CipherProperties(Paths.get(keyLocation), iv));
        LOG.info("Encrypted value: {}", blowfishEncryptionService.encrypt(value));
        System.exit(0);
    }
}
