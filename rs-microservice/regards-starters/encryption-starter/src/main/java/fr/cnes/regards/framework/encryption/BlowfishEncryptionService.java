package fr.cnes.regards.framework.encryption;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.nio.file.Files;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.encryption.configuration.CipherProperties;
import fr.cnes.regards.framework.encryption.exception.EncryptionException;
import fr.cnes.regards.framework.utils.RsRuntimeException;

/**
 * Implementation of {@link IEncryptionService} which uses Blowfish as algorithm
 * @author Sylvain VISSIERE-GUERINET
 */
public class BlowfishEncryptionService implements IEncryptionService {

    private static final Logger LOG = LoggerFactory.getLogger(BlowfishEncryptionService.class);

    private static final String BLOWFISH_NAME = "Blowfish";

    private static final String BLOWFISH_INSTANCE = BLOWFISH_NAME + "/CBC/PKCS5Padding";

    private SecretKeySpec secretKey;

    private IvParameterSpec ivParamSpec;

    @Override
    public String encrypt(String toEncrypt) throws EncryptionException {
        if ((secretKey == null) || (ivParamSpec == null)) {
            throw new IllegalStateException(
                    "You cannot encrypt data before the key and initialization vector has been set.");
        }
        try {
            Cipher blowfish = Cipher.getInstance(BLOWFISH_INSTANCE);

            blowfish.init(Cipher.ENCRYPT_MODE, secretKey, ivParamSpec);
            return DatatypeConverter.printBase64Binary(blowfish.doFinal(toEncrypt.getBytes()));
        } catch (InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            //those two exception should never occur
            LOG.error("There was an issue with encryption using Blowfish", e);
            throw new RsRuntimeException(e);
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            throw new EncryptionException(
                    String.format("\"%s\" could not be encrypted using %s", toEncrypt, BLOWFISH_INSTANCE), e);
        }
    }

    @Override
    public String decrypt(String toDecrypt) throws EncryptionException {
        if ((secretKey == null) || (ivParamSpec == null)) {
            throw new IllegalStateException(
                    "You cannot decrypt data before the key and initialization vector has been set.");
        }
        try {
            Cipher blowfish = Cipher.getInstance(BLOWFISH_INSTANCE);
            blowfish.init(Cipher.DECRYPT_MODE, secretKey, ivParamSpec);
            return new String(blowfish.doFinal(DatatypeConverter.parseBase64Binary(toDecrypt)));
        } catch (InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            //those two exception should never occur
            LOG.error("There was an issue with encryption using Blowfish", e);
            throw new RsRuntimeException(e);
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            throw new EncryptionException(
                    String.format("\"%s\" could not be decrypted using %s", toDecrypt, BLOWFISH_INSTANCE), e);
        }
    }

    /**
     * Initialize BlowfishEncryptionService by setting its secret key and initialization vector.
     * @throws InvalidAlgorithmParameterException in case the initialization vector is not valid
     * @throws InvalidKeyException                in case the key is not valid
     * @throws IOException                        because of Files.readAllLines
     */
    public void init(CipherProperties properties)
            throws InvalidAlgorithmParameterException, InvalidKeyException, IOException {
        String key = Files.readAllLines(properties.getKeyLocation()).get(0);
        secretKey = new SecretKeySpec(key.getBytes(), BLOWFISH_NAME);
        ivParamSpec = new IvParameterSpec(properties.getIv().getBytes());
        //to check that the secret key is valid , lets init a cipher with "12345678" as IV
        try {
            Cipher blowfish = Cipher.getInstance(BLOWFISH_INSTANCE);
            blowfish.init(Cipher.DECRYPT_MODE, secretKey, ivParamSpec);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            //those exception are normally sent only on fixed values. Our test key is valid, the algorithm and padding is valid too
            throw new RsRuntimeException(e);
        }
    }

}
