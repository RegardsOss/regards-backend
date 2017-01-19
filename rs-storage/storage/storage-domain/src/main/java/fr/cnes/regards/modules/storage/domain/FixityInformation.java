/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class FixityInformation extends Information {

    private transient String algorithm;

    private transient String checksum;

    private transient int fileSize;

    public FixityInformation() {
        super();
    }

    private String sha1(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA1");
        byte[] result = digest.digest(input.getBytes());
        StringBuffer sb = new StringBuffer();
        for (byte element : result) {
            sb.append(Integer.toString((element & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String pAlgorithm) {
        algorithm = pAlgorithm;
        addMetadata("algorithm", algorithm);
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String pChecksum) {
        checksum = pChecksum;
        addMetadata("checksum", checksum);
    }

    public int getFileSize() {
        return fileSize;
    }

    public void setFileSize(int pFileSize) {
        fileSize = pFileSize;
        addMetadata("fileSize", fileSize);
    }

    public FixityInformation generate() throws NoSuchAlgorithmException {
        algorithm = "SHA1";
        addMetadata("algorithm", algorithm);
        fileSize = (new Random()).nextInt(10000000);
        addMetadata("fileSize", fileSize);
        checksum = sha1("blahblah");
        addMetadata("checksum", checksum);
        return this;
    }

}
