/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.swagger.autoconfigure;

import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Check that spring boot server is already configured
 *
 * @author msordi
 *
 */
public class ServerProperties extends AllNestedConditions {

    public ServerProperties() {
        super(ConfigurationPhase.REGISTER_BEAN);
    }

    /**
     * @param pConfigurationPhase
     */
    public ServerProperties(final ConfigurationPhase pConfigurationPhase) {
        super(pConfigurationPhase);
    }

    @ConditionalOnProperty("server.adress")
    static class OnServerAdress {

    }

    @ConditionalOnProperty("server.port")
    static class OnServerPort {

    }

}
