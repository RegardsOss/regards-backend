package fr.cnes.regards.framework.encryption.configuration;

import javax.validation.constraints.NotNull;
import java.nio.file.Path;

import org.hibernate.validator.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties class for ciphers in regards
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@ConfigurationProperties(prefix = "regards.cipher")
public class CipherProperties {

    /**
     * Cipher key location
     */
    @NotNull
    private Path keyLocation;

    /**
     * Initialization vector
     */
    @NotBlank
    private String iv;

    public CipherProperties() {
    }

    public CipherProperties(Path keyLocation, String iv) {
        this.keyLocation = keyLocation;
        this.iv = iv;
    }

    public Path getKeyLocation() {
        return keyLocation;
    }

    public void setKeyLocation(Path keyLocation) {
        this.keyLocation = keyLocation;
    }

    public String getIv() {
        return iv;
    }

    public void setIv(String iv) {
        this.iv = iv;
    }
}