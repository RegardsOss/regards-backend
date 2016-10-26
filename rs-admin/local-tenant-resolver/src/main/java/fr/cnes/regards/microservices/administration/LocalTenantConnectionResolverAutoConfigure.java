/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.ITenantConnectionResolver;
import fr.cnes.regards.framework.multitenant.autoconfigure.tenant.ITenantResolver;
import fr.cnes.regards.modules.project.service.ProjectService;

/**
 *
 * Class MicroserviceTenantResolverAutoConfigure
 *
 * Autoconfiguration class for Administration Local multitenant resolver
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Configuration
public class LocalTenantConnectionResolverAutoConfigure {

    /**
     * Current microservice name
     */
    @Value("${spring.application.name}")
    private String microserviceName;

    /**
     * Administration project service
     */
    @Autowired
    private ProjectService projectService;

    /**
     *
     * multintenantResolver
     *
     * @return IMultitenantResolver
     * @since 1.0-SNAPSHOT
     */
    @Bean
    @ConditionalOnMissingBean
    public ITenantConnectionResolver tenantConnectionResolver() {
        return new LocalTenantConnectionResolver(microserviceName, projectService);
    }

    @Bean
    @ConditionalOnMissingBean
    public ITenantResolver tenantResolver() {
        return new LocalTenantResolver();
    }

}
