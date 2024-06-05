package fr.cnes.regards.framework.encryption.configuration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Properties class for ciphers in regards
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Validated
@ConfigurationProperties(prefix = "regards.cipher")
public class CipherProperties {

    /**
     * Cipher key location.<br/>
     * A String is used instead of Path because strange behaviour has alrady appended at microservice startup with Path:
     * Resource class is used checking that Path exists somewhere into file system or loaded jar and there seems a bug
     * exist into Jetty (to be confirmed but using a String is no big deal so...)
     */
    @NotNull
    private String keyLocation;

    /**
     * Initialization vector
     */
    @NotBlank
    private String iv;

    public CipherProperties() {
    }

    public CipherProperties(String keyLocation, String iv) {
        this.keyLocation = keyLocation;
        this.iv = iv;
    }

    public String getKeyLocation() {
        return keyLocation;
    }

    public void setKeyLocation(String keyLocation) {
        this.keyLocation = keyLocation;
    }

    public String getIv() {
        return iv;
    }

    public void setIv(String iv) {
        this.iv = iv;
    }
}