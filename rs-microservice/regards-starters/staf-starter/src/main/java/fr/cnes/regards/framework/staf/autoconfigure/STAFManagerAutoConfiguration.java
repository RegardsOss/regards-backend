package fr.cnes.regards.framework.staf.autoconfigure;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.staf.STAFSessionManager;
import fr.cnes.regards.framework.staf.domain.STAFConfiguration;
import fr.cnes.regards.framework.staf.exception.STAFException;
import fr.cnes.regards.framework.staf.protocol.STAFUrlFactory;

@Configuration
@EnableConfigurationProperties(STAFConfiguration.class)
public class STAFManagerAutoConfiguration {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(STAFManagerAutoConfiguration.class);

    @Autowired
    private STAFConfiguration configuration;

    @PostConstruct
    public static void init() {
        LOGGER.info("URL staf protocol initialized.");
        STAFUrlFactory.initSTAFURLProtocol();
    }

    @Bean
    @ConditionalOnMissingBean
    public STAFSessionManager initSTAFManager() throws STAFException {
        return STAFSessionManager.getInstance(configuration);
    }

}
