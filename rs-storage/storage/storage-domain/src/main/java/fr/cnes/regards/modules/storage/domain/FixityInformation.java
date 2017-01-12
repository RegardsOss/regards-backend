package fr.cnes.regards.modules.storage.domain;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class FixityInformation extends Information {

    private String algorithm;

    private String checksum;

    private int fileSize;

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
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String pChecksum) {
        checksum = pChecksum;
    }

    public int getFileSize() {
        return fileSize;
    }

    public void setFileSize(int pFileSize) {
        fileSize = pFileSize;
    }

    public FixityInformation generate() throws NoSuchAlgorithmException {
        this.algorithm = "SHA1";
        this.addMetadata(new KeyValuePair("algorithm", this.algorithm));
        this.fileSize = (new Random()).nextInt(10000000);
        this.addMetadata(new KeyValuePair("fileSize", this.fileSize));
        this.checksum = sha1("blahblah");
        this.addMetadata(new KeyValuePair("checksum", this.checksum));
        return this;
    }

}
