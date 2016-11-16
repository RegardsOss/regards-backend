/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.microservice.configurer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.microservice.filter.MaintenanceFilter;
import fr.cnes.regards.framework.security.configurer.ICustomWebSecurityConfiguration;
import fr.cnes.regards.framework.security.filter.CorsFilter;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;

/**
 *
 * Custom configuration to handle request while in maintenance
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
@Component
public class MaintenanceConfiguration implements ICustomWebSecurityConfiguration {

    @Autowired
    private JWTService jwtService;

    @Override
    public void configure(HttpSecurity pHttp) throws Exception {
        pHttp.addFilterAfter(new MaintenanceFilter(jwtService), CorsFilter.class);
    }

}
