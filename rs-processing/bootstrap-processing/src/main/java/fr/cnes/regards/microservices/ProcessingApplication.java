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
package fr.cnes.regards.microservices;

import fr.cnes.regards.framework.microservice.annotation.MicroserviceInfo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Start microservice processing
 *
 * @author Guillaume Andrieu
 */
@SpringBootApplication(scanBasePackages = { "fr.cnes.regards.modules", "fr.cnes.regards.contrib" })
@MicroserviceInfo(name = "processing", version = "2.0.0-SNAPSHOT")
public class ProcessingApplication {

    /**
     * Microservice bootstrap method
     * @param args microservice bootstrap arguments
     */
    public static void main(final String[] args) {
        SpringApplication app = new SpringApplication(ProcessingApplication.class);
        // app.setWebApplicationType(WebApplicationType.REACTIVE);
        app.run(args);
    }

}
