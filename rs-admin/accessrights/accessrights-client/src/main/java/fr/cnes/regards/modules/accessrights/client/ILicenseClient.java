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
package fr.cnes.regards.modules.accessrights.client;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.accessrights.domain.projects.LicenseDTO;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;

/**
 * API client for license reset
 *
 * @author Marc Sordi
 */
@RestClient(name = "rs-admin", contextId = "rs-admin.license-client")
public interface ILicenseClient {

    /**
     * Controller base path
     */
    String PATH_LICENSE = "/license";

    /**
     * Controller path to reset the license
     */
    String PATH_RESET = "/reset";

    @GetMapping(path = ILicenseClient.PATH_LICENSE, produces = MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<EntityModel<LicenseDTO>> retrieveLicense();

    @PutMapping(path = ILicenseClient.PATH_LICENSE, produces = MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<EntityModel<LicenseDTO>> acceptLicense();

    @PutMapping(path = ILicenseClient.PATH_LICENSE + PATH_RESET, produces = MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> resetLicense();
}
