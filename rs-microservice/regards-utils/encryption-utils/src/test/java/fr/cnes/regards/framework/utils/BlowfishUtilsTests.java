package fr.cnes.regards.framework.utils;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class BlowfishUtilsTests {

    @Test
    public void testEncryptDecrypt()
            throws BadPaddingException, InvalidKeyException, IllegalBlockSizeException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidAlgorithmParameterException {
        Random random = new Random();
        for(int i=0;i<1000;i++) {
            String toEncrypt = generateRandomString(random, i);
            String encrypted = BlowfishUtils.encrypt(toEncrypt);
            String decrypted = BlowfishUtils.decrypt(encrypted);
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
