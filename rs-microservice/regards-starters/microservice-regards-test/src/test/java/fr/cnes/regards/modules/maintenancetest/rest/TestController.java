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
package fr.cnes.regards.modules.maintenancetest.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.microservice.maintenance.MaintenanceException;

/**
 *
 * RestController test implementation.
 *
 * @author Christophe Mertz
 * @since 1.0-SNAPSHOT
 */
@RestController
public class TestController {

    public static final String MAINTENANCE_TEST_URL = "/maintenance/test";

    public static final String MAINTENANCE_TEST_503_URL = "/maintenance/test/error";

    @RequestMapping(method = RequestMethod.GET, path = MAINTENANCE_TEST_URL)
    public ResponseEntity<Void> testMethod() {
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, path = MAINTENANCE_TEST_503_URL)
    public ResponseEntity<Void> testMethodUnavailable() {
        throw new MaintenanceException("it is a runtime exception");
    }

    @RequestMapping(method = RequestMethod.POST, path = MAINTENANCE_TEST_URL)
    public ResponseEntity<String> otherTestMethod() {
        return new ResponseEntity<>("that's created", HttpStatus.CREATED);
    }

}
