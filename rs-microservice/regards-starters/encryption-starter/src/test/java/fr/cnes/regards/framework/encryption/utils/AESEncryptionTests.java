package fr.cnes.regards.framework.encryption.utils;

import java.io.IOException;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.util.Random;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.regards.framework.encryption.AESEncryptionService;
import fr.cnes.regards.framework.encryption.configuration.CipherProperties;
import fr.cnes.regards.framework.encryption.exception.EncryptionException;

/**
 * Some basic tests
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public class AESEncryptionTests {

    private AESEncryptionService aesEncryptionService;

    @Before
    public void init() throws InvalidAlgorithmParameterException, InvalidKeyException, IOException {
        aesEncryptionService = new AESEncryptionService();
        aesEncryptionService.init(new CipherProperties(Paths.get("src", "test", "resources", "testKey"), "1234567812345678"));
    }

    @Test
    public void testEncryptDecrypt() throws EncryptionException {
        Random random = new Random();
        for (int i = 0; i < 1000; i++) {
            String toEncrypt = generateRandomString(random, i);
            String encrypted = aesEncryptionService.encrypt(toEncrypt);
            String decrypted = aesEncryptionService.decrypt(encrypted);
            Assert.assertEquals(toEncrypt, decrypted);
        }
    }

    @Test
    public void testDecryptMultipleTime() throws EncryptionException {
        Random random = new Random();
        String toEncrypt = generateRandomString(random, 15);
        String encrypted = aesEncryptionService.encrypt(toEncrypt);
        for (int i = 0; i < 1000; i++) {
            String decrypted = aesEncryptionService.decrypt(encrypted);
            Assert.assertEquals(toEncrypt, decrypted);
        }
    }

    private String generateRandomString(Random random, int stringLength) {
        String possibleLetters = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWYXZ";
        char[] string = new char[stringLength];
        for (int j = 0; j < stringLength; j++) {
            string[j] = possibleLetters.charAt(random.nextInt(possibleLetters.length()));
        }
        return new String(string);
    }
}
