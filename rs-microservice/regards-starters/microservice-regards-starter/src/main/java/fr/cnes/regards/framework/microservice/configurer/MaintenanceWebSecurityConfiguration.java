/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
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
