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
package fr.cnes.regards.modules.project.dao;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.support.RegistrationPolicy;

/**
 *
 * Class ProjectDaoTestConfiguration
 *
 * Configuration class for DAO tests.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Configuration
@EnableAutoConfiguration
@PropertySource("classpath:tests.properties")
public class ProjectDaoTestConfiguration {

    @Bean
    public MBeanExporter mBeanExporter() {
        MBeanExporter exporter=new MBeanExporter();
        exporter.setRegistrationPolicy(RegistrationPolicy.IGNORE_EXISTING);
        return exporter;
    }

}
