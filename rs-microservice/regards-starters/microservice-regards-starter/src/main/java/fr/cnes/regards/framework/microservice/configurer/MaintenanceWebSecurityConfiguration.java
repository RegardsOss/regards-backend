/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.microservice.configurer;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import fr.cnes.regards.framework.microservice.maintenance.MaintenanceFilter;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.configurer.ICustomWebSecurityConfiguration;
import fr.cnes.regards.framework.security.filter.CorsFilter;

/**
 *
 * Custom configuration to handle request while in maintenance
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Marc Sordi
 *
 */
public class MaintenanceWebSecurityConfiguration implements ICustomWebSecurityConfiguration {

    /**
     * Thread tenant resolver
     */
    private final IRuntimeTenantResolver resolver;

    public MaintenanceWebSecurityConfiguration(IRuntimeTenantResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void configure(final HttpSecurity pHttp) {
        pHttp.addFilterAfter(new MaintenanceFilter(resolver), CorsFilter.class);
    }

}
