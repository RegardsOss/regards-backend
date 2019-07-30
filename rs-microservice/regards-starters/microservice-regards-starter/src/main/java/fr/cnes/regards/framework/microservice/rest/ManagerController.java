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
package fr.cnes.regards.framework.microservice.rest;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.microservice.manager.IApplicationManager;

/**
 * @author svissier
 */
@RestController
@RequestMapping("/")
@ConditionalOnWebApplication
public class ManagerController {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ManagerController.class);

    /**
     * Application manager
     */
    @Autowired
    private IApplicationManager applicationManager;

    /**
     * Allows to immediately shutdown the application
     */
    @PostMapping("/shutdown/immediate")
    public ResponseEntity<Void> immediateShutdown() {
        ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.OK);
        try {
            applicationManager.immediateShutdown();
        } catch (final IOException e) {
            LOG.error(e.getMessage(), e);
            response = new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

}
