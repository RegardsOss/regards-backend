/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.cloud.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Class ConfigApplication
 *
 * Boostrap for Spring Configuration Server
 * @author Sébastien Binda
 */
// CHECKSTYLE:OFF
@SpringBootApplication
// CHECKSTYLE:ON
@EnableConfigServer
public class Application { // NOSONAR

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    public static void main(final String[] pArgs) {
        try {
            SpringApplication.run(Application.class, pArgs); // NOSONAR
        } catch (Exception e) {
            LOGGER.error("Going to exit", e);
            System.exit(1);
        }
    }
}