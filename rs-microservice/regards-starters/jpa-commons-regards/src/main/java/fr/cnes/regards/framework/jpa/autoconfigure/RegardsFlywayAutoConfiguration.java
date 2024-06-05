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
package fr.cnes.regards.framework.jpa.autoconfigure;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.ClassicConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Configuration to prevent Flyway autoconfiguration
 * It also allows to override classic ({@link ClassicConfiguration}) Flyway configuration
 *
 * @author Olivier Rousselot
 */
@AutoConfiguration(before = FlywayAutoConfiguration.class)
public class RegardsFlywayAutoConfiguration {

    @Value("${spring.jpa.properties.hibernate.default_schema:#{null}}")
    private String defaultSchema;

    /**
     * Whether to allow migrations to be run out of order.
     */
    @Value("${spring.flyway.out-of-order:false}")
    private boolean outOfOrder;

    /**
     * Prevent flyway autoconfiguration (Flyway is created by this configuration not by the FlywayAutoConfiguration)
     */
    @Bean
    public Flyway flyway() {
        return Flyway.configure().defaultSchema(defaultSchema).outOfOrder(outOfOrder).load();
    }
}
