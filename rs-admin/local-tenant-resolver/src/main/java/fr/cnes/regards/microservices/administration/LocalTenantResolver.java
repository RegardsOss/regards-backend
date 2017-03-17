/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.project.service.ITenantService;

/**
 *
 * Class LocalTenantResolver
 *
 * Administration microservice local tenant resolver.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public class LocalTenantResolver implements ITenantResolver {

    /**
     * Microservice name
     */
    @Value("${spring.application.name}")
    private String microserviceName;

    /**
     * Administration project service
     */
    @Autowired
    private ITenantService tenantService;

    @Override
    public Set<String> getAllTenants() {
        return tenantService.getAllTenants();
    }

    @Override
    public Set<String> getAllActiveTenants() {
        return tenantService.getAllActiveTenants(microserviceName);
    }
}
