package fr.cnes.regards.framework.utils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.org.apache.bcel.internal.classfile.ConstantString;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class BlowfishUtils {

    private static final Logger LOG = LoggerFactory.getLogger(BlowfishUtils.class);

    private static final String key = "746f746f"; //FIXME: toto in hexa

    private static final String BLOWFISH_INSTANCE = "Blowfish/CBC/PKCS5Padding";

    private static final String IV = "12345678";

    public static String encrypt(String toEncrypt)
            throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException,
            InvalidAlgorithmParameterException {
        try {
            Cipher blowfish = Cipher.getInstance(BLOWFISH_INSTANCE);
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "Blowfish");
            blowfish.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(IV.getBytes()));
            return DatatypeConverter.printBase64Binary(blowfish.doFinal(toEncrypt.getBytes()));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            //those two exception should never occur
            LOG.error("There was an issue with encryption using Blowfish", e);
            throw new RsRuntimeException(e);
        }
    }

    public static String decrypt(String toDecrypt)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException,
            IllegalBlockSizeException, InvalidAlgorithmParameterException {
        Cipher blowfish = Cipher.getInstance(BLOWFISH_INSTANCE);
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "Blowfish");
        blowfish.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(IV.getBytes()));
        return new String(blowfish.doFinal(DatatypeConverter.parseBase64Binary(toDecrypt)));
    }

}
