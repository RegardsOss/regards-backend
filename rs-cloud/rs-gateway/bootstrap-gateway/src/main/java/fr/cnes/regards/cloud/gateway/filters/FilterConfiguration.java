/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.cloud.gateway.filters;

import fr.cnes.regards.cloud.gateway.authentication.ExternalAuthenticationVerifier;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Global filters to be applied on all routes ordered by execution.
 * In case "post-logic" filters are also implemented, they will be executed in descending order of the highest precedence.
 *
 * @author Iliana Ghazali
 * @see <a href="https://cloud.spring.io/spring-cloud-gateway/reference/html/#global-filters">spring gateway doc</a>
 **/
@Configuration
public class FilterConfiguration {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public GlobalFilter inputOutputPreparationFilter() {
        return new InputOutputPreparationFilter();
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 2)
    public GlobalFilter externalTokenVerificationFilter(JWTService jwtService,
                                                        ExternalAuthenticationVerifier externalAuthenticationVerifier) {
        return new ExternalTokenVerificationFilter(jwtService, externalAuthenticationVerifier);
    }
}
