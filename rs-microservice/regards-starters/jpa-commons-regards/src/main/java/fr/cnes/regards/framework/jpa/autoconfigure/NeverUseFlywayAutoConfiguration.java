/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.jpa.autoconfigure;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * COnfiguration to prevent Flyway auto configuration
 * @author Olivier Rousselot
 */
@Configuration
@AutoConfigureBefore(FlywayAutoConfiguration.class)
public class NeverUseFlywayAutoConfiguration {

    @Value("${spring.jpa.properties.hibernate.default_schema:null}")
    private String defaultSchema;

    /**
     * Prevent flyway auto configuration (Flyway is created by this configuration not by the FlywayAutoConfiguration)
     */
    @Bean
    public Flyway flyway() {
        return Flyway.configure().defaultSchema(defaultSchema).load();
    }
}
