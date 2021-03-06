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
package fr.cnes.regards.modules.backendforfrontend.rest;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.toponyms.client.IToponymsClient;
import fr.cnes.regards.modules.toponyms.domain.ToponymDTO;
import fr.cnes.regards.modules.toponyms.domain.ToponymGeoJson;
import fr.cnes.regards.modules.toponyms.domain.ToponymGeoJsonDTO;
import fr.cnes.regards.modules.toponyms.domain.ToponymsRestConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.List;

/**
 * Controller to search for Toponymms throught instance module toponyms of access-instance
 *
 * @author S??bastien Binda
 *
 */
@RestController
@RequestMapping(path = ToponymsRestConfiguration.ROOT_MAPPING)
public class ToponymsController {

    @Autowired
    private IToponymsClient client;

    /** Authentication */
    @Autowired
    private IAuthenticationResolver authenticationResolver;

    /** Tenant resolver */
    @Autowired
    private IRuntimeTenantResolver tenantResolver;


    /**
     * Endpoint to retrieve one toponym by his identifier
     *
     * @param businessId Unique identifier of toponym to search for
     * @param simplified True for simplified geometry (minimize size)
     * @return {@link ToponymDTO}
     * @throws EntityNotFoundException
     */
    @RequestMapping(value = ToponymsRestConfiguration.TOPONYM_ID, method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve one toponym by his identifier", role = DefaultRole.PUBLIC)
    public ResponseEntity<EntityModel<ToponymDTO>> get(@PathVariable("businessId") String businessId,
            @RequestParam(required = false) Boolean simplified) throws HttpClientErrorException, HttpServerErrorException  {
        FeignSecurityManager.asInstance();
        try {
            if (simplified != null) {
                return client.get(businessId, simplified);
            } else {
                return client.get(businessId);
            }
        } finally {
            FeignSecurityManager.reset();
        }
    }

    /**
     * Endpoint to search for toponyms. Geometries are not retrivied and list content is limited to 100 entities.
     *
     * @param partialLabel
     * @param locale
     * @return {@link ToponymDTO}s
     * @throws EntityNotFoundException
     */
    @RequestMapping(value = ToponymsRestConfiguration.SEARCH, method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(
            description = "Endpoint to search for toponyms. Geometries are not retrivied and list content is limited to 100 entities.",
            role = DefaultRole.PUBLIC)
    public ResponseEntity<List<EntityModel<ToponymDTO>>> search(@RequestParam(required = false) String partialLabel,
            @RequestParam(required = false) String locale) throws HttpClientErrorException, HttpServerErrorException {
        FeignSecurityManager.asInstance();
        try {
            return client.search(partialLabel, locale);
        } finally {
            FeignSecurityManager.reset();
        }
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to add a not visible toponym.", role = DefaultRole.REGISTERED_USER)
    public ResponseEntity<EntityModel<ToponymDTO>> createNotVisibleToponym(
            @RequestBody ToponymGeoJsonDTO toponymGeoJsonDTO)
            throws HttpClientErrorException, HttpServerErrorException {
        FeignSecurityManager.asInstance();
        try {
            String feature = toponymGeoJsonDTO.getToponym();
            return client.createNotVisibleToponym(new ToponymGeoJson(feature, this.authenticationResolver.getUser(),
                                                                     this.tenantResolver.getTenant()));
        } finally {
            FeignSecurityManager.reset();
        }
    }
}
