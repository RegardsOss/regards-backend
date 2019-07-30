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
package fr.cnes.regards.framework.authentication.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;

/**
 * Manage authentication bean
 * @author msordi
 */
@Configuration
public class AuthenticationAutoConfiguration {

    public static final String DEFAULT_USER = "DEFAULT_USER";

    public static final String DEFAULT_ROLE = "DEFAULT_ROLE";

    public static final String DEFAULT_TOKEN = "DEFAULT_TOKEN";

    @ConditionalOnMissingBean
    @Bean
    public IAuthenticationResolver defaultAuthenticationResolver() {
        return new DefaultAuthenticationResolver();
    }

    private static class DefaultAuthenticationResolver implements IAuthenticationResolver {

        @Override
        public String getUser() {
            return DEFAULT_USER;
        }

        @Override
        public String getRole() {
            return DEFAULT_ROLE;
        }

        @Override
        public String getToken() {
            return DEFAULT_TOKEN;
        }
    }
}
