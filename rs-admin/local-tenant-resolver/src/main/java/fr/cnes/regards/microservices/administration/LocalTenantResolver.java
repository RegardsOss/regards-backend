/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

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
     * Administration project service
     */
    @Autowired
    private ITenantService tenantService;

    @Override
    public Set<String> getAllTenants() {
        return tenantService.getAllTenants();
    }
}
