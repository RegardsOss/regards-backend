/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.ITenantConnectionResolver;
import fr.cnes.regards.modules.project.client.IProjectsClient;

/**
 *
 * Class MicroserviceTenantResolverAutoConfigure
 *
 * Autoconfiguration class for Microservices multitenant resolver
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Configuration
public class MicroserviceTenantConnectionResolverAutoConfigure {

    /**
     * Current Microservice name
     */
    @Value("${spring.application.name}")
    private String microserviceName;

    /**
     *
     * multintenantResolver
     *
     * @param pAdminProjectClient
     *            Administration Rest Client
     * @return IMultitenantResolver
     * @since 1.0-SNAPSHOT
     */
    @Bean
    @ConditionalOnMissingBean(ITenantConnectionResolver.class)
    @ConditionalOnProperty(name = "regards.eureka.client.enabled", havingValue = "true", matchIfMissing = false)
    ITenantConnectionResolver multintenantResolver(final IProjectsClient pAdminProjectClient) {
        return new MicroserviceTenantConnectionResolver(pAdminProjectClient, microserviceName);
    }

}
