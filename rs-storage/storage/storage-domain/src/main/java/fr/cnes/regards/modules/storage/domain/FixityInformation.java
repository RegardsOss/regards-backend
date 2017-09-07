/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import javax.validation.constraints.NotNull;

public class FixityInformation implements Serializable {

    @NotNull
    private String algorithm;

    @NotNull
    private String checksum;

    private Long fileSize;

    public FixityInformation() {
        super();
    }

    public FixityInformation(String algorithm, String checksum) {
        this.algorithm = algorithm;
        this.checksum = checksum;
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

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long pFileSize) {
        fileSize = pFileSize;
    }

    public FixityInformation generate() throws NoSuchAlgorithmException {
        algorithm = "SHA1";
        fileSize = new Long((new Random()).nextInt(10000000));
        checksum = sha1("blahblah");
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((algorithm == null) ? 0 : algorithm.hashCode());
        result = (prime * result) + ((checksum == null) ? 0 : checksum.hashCode());
        result = (prime * result) + ((fileSize == null) ? 0 : fileSize.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        FixityInformation other = (FixityInformation) obj;
        if (algorithm == null) {
            if (other.algorithm != null) {
                return false;
            }
        } else
            if (!algorithm.equals(other.algorithm)) {
                return false;
            }
        if (checksum == null) {
            if (other.checksum != null) {
                return false;
            }
        } else
            if (!checksum.equals(other.checksum)) {
                return false;
            }
        if (fileSize == null) {
            if (other.fileSize != null) {
                return false;
            }
        } else
            if (!fileSize.equals(other.fileSize)) {
                return false;
            }
        return true;
    }

}
