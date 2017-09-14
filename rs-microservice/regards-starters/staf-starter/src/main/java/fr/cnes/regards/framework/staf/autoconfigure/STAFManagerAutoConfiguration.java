package fr.cnes.regards.framework.staf.autoconfigure;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.staf.STAFManager;
import fr.cnes.regards.framework.staf.domain.STAFConfiguration;
import fr.cnes.regards.framework.staf.exception.STAFException;

@Configuration
@EnableConfigurationProperties(STAFConfiguration.class)
public class STAFManagerAutoConfiguration {

    @Autowired
    private STAFConfiguration configuration;

    @Bean
    @ConditionalOnMissingBean
    public STAFManager initSTAFManager() throws STAFException {
        return STAFManager.getInstance(configuration);
    }

}
