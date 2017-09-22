/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.staf.standalone;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.staf.STAFSessionManager;
import fr.cnes.regards.framework.staf.domain.STAFConfiguration;
import fr.cnes.regards.framework.staf.exception.STAFException;
import fr.cnes.regards.framework.staf.protocol.STAFURLFactory;

/**
 * STAF Util standalone spring application configuration.
 * @author Sébastien Binda
 */
@Configuration
public class STAFConfig {

    @Autowired
    private STAFConfiguration configuration;

    @PostConstruct
    public void init() {
        STAFURLFactory.initSTAFURLProtocol();
    }

    @Bean
    public STAFSessionManager getSessionManager() throws STAFException {
        return STAFSessionManager.getInstance(configuration);
    }

}
