/* Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.config;

import javax.annotation.PostConstruct;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;

import fr.cnes.regards.modules.processing.rest.PBatchReactiveController;
import fr.cnes.regards.modules.processing.rest.PMonitoringReactiveController;
import fr.cnes.regards.modules.processing.rest.PProcessReactiveController;

/**
 * TODO : Class description
 *
 * @author Guillaume Andrieu
 *
 */
@Configuration
@ConditionalOnProperty(name = "spring.main.web-application-type", havingValue = "reactive")
@EnableWebFlux
@ComponentScan(basePackageClasses = { PBatchReactiveController.class, PProcessReactiveController.class,
        PMonitoringReactiveController.class })
public class ProcessingRestConfiguration {

    @PostConstruct
    public void init() {

    }

}
