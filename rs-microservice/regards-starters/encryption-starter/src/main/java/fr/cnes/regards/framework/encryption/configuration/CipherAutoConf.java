package fr.cnes.regards.framework.encryption.configuration;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.encryption.AESEncryptionService;
import fr.cnes.regards.framework.encryption.IEncryptionService;

/**
 * Auto configuration for ciphers. Default encryption service: {@link AESEncryptionService}
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@EnableConfigurationProperties({ CipherProperties.class })
@Configuration
public class CipherAutoConf {

    @Autowired
    private CipherProperties cipherProperties;

    @Bean
    @ConditionalOnMissingBean(IEncryptionService.class)
    public IEncryptionService blowfishEncryptionService()
            throws InvalidAlgorithmParameterException, InvalidKeyException, IOException {
        AESEncryptionService aesEncryptionService = new AESEncryptionService();
        aesEncryptionService.init(cipherProperties);
        return aesEncryptionService;
    }

}
