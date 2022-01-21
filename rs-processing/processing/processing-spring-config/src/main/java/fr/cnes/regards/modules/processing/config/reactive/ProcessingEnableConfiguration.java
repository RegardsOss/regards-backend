/* Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.config.reactive;

import fr.cnes.regards.framework.feign.autoconfigure.FeignWebMvcConfiguration;
import fr.cnes.regards.framework.security.autoconfigure.MethodSecurityAutoConfiguration;
import fr.cnes.regards.framework.security.autoconfigure.WebSecurityAutoConfiguration;
import name.nkonev.r2dbc.migrate.autoconfigure.R2dbcMigrateAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
/**
 * This class is the "@EnableXXX" config for reactive application.
 * @author gandrieu
 */
@Configuration
@ConditionalOnProperty(name = "spring.main.web-application-type", havingValue = "reactive")

@EnableAutoConfiguration(exclude = {
        R2dbcMigrateAutoConfiguration.class,
        WebMvcAutoConfiguration.class,
        FeignWebMvcConfiguration.class,
        WebSecurityAutoConfiguration.class,
        MethodSecurityAutoConfiguration.class
})
@EnableWebFluxSecurity
@EnableJpaRepositories
@EnableFeignClients
public class ProcessingEnableConfiguration {

}
