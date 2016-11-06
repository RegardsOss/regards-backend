/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import fr.cnes.regards.cloud.gateway.authentication.plugins.IAuthenticationPlugin;
import fr.cnes.regards.cloud.gateway.authentication.stub.AuthenticationPluginStub;
import fr.cnes.regards.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.modules.plugins.dao.stubs.PluginConfigurationRepositoryStub;

/**
 *
 * Class AuthenticationTestConfiguration
 *
 * Test configuration class
 *
 * @author Sébastien Binda
 *
 */
@ComponentScan(basePackages = { "fr.cnes.regards.cloud.gateway", "fr.cnes.regards.modules" })
@EnableAutoConfiguration
public class AuthenticationTestConfiguration {

    /**
     *
     * Create transactionManager mandatory by DefaultTokenService but we dont need it because JWT token are not stored.
     *
     * @return PlatformTransactionManager
     * @since 1.0-SNAPSHOT
     */
    @Bean
    @ConditionalOnMissingBean(PlatformTransactionManager.class)
    public PlatformTransactionManager transactionManagerStub() {
        return new PlatformTransactionManager() {

            @Override
            public void rollback(final TransactionStatus pStatus) {
                // Nothing to do. The JwtTokenStore does not persists tokens
            }

            @Override
            public TransactionStatus getTransaction(final TransactionDefinition pDefinition) {
                // Nothing to do. The JwtTokenStore does not persists tokens
                return null;
            }

            @Override
            public void commit(final TransactionStatus pStatus) {
                // Nothing to do. The JwtTokenStore does not persists tokens
            }
        };
    }

    @Bean
    IAuthenticationPlugin defaultAuthenticationPlugin() {
        return new AuthenticationPluginStub();
    }

    @Bean
    IPluginConfigurationRepository pluginConfigurationRepo() {
        return new PluginConfigurationRepositoryStub();
    }

}
