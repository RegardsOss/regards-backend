/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.security.configurer;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

/**
 * Interface ICustomWebSecurityConfiguration
 *
 * Interface to define specific WebSecurity configurer
 * @author Sébastien Binda
 */
@FunctionalInterface
public interface ICustomWebSecurityConfiguration {

    /**
     * Configure HttpSecurity
     * @param pHttp HttpSecurity
     * @throws CustomWebSecurityConfigurationException configuration exception
     */
    void configure(final HttpSecurity pHttp) throws CustomWebSecurityConfigurationException;

}
