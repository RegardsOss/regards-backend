/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.microservice.configurer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;

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
@Component
public class MaintenanceConfiguration implements ICustomWebSecurityConfiguration {

    /**
     * Thread tenant resolver
     */
    @Autowired
    private IRuntimeTenantResolver pResolver;

    @Value("${regards.microservices.enable.maintenance.mode:true}")
    private static final boolean ENABLE_MAINTENANCE_MODE = true;

    @Override
    public void configure(final HttpSecurity pHttp) {
        if (ENABLE_MAINTENANCE_MODE) {
            pHttp.addFilterAfter(new MaintenanceFilter(pResolver), CorsFilter.class);
        }
    }

}
