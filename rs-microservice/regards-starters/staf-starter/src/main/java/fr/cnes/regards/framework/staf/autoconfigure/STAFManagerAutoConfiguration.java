package fr.cnes.regards.framework.staf.autoconfigure;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.staf.STAFConfiguration;
import fr.cnes.regards.framework.staf.STAFException;
import fr.cnes.regards.framework.staf.STAFManager;

@Configuration
@EnableConfigurationProperties(STAFConfiguration.class)
public class STAFManagerAutoConfiguration {

    @Autowired
    private STAFConfiguration configuration;

    @Bean
    private STAFManager initSTAFManager() throws STAFException {
        return STAFManager.getInstance(configuration);
    }

}
