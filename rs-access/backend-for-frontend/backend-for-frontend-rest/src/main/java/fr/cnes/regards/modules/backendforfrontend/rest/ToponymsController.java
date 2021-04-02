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
import fr.cnes.regards.modules.toponyms.domain.ToponymsRestConfiguration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

/**
 * Controller to search for Toponymms throught instance module toponyms of access-instance
 *
 * @author Sébastien Binda
 *
 */
@RestController
@RequestMapping(path = ToponymsRestConfiguration.ROOT_MAPPING)
public class ToponymsController {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AccessSearchController.class);

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
            @RequestParam(required = false) Boolean simplified) throws EntityNotFoundException {
        FeignSecurityManager.asInstance();
        try {
            if (simplified != null) {
                return client.get(businessId, simplified);
            } else {
                return client.get(businessId);
            }
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            LOGGER.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.EXPECTATION_FAILED);
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
            @RequestParam(required = false) String locale) throws EntityNotFoundException {
        FeignSecurityManager.asInstance();
        try {
            return client.search(partialLabel, locale);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            LOGGER.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.EXPECTATION_FAILED);
        } finally {
            FeignSecurityManager.reset();
        }
    }

    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to add a not visible toponym.", role = DefaultRole.REGISTERED_USER)
    public ResponseEntity<EntityModel<ToponymDTO>> createNotVisibleToponym(@RequestBody String featureString) {
        FeignSecurityManager.asInstance();
        try {
            return client.createNotVisibleToponym(new ToponymGeoJson(featureString, this.authenticationResolver.getUser(), this.tenantResolver.getTenant()));
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            LOGGER.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.EXPECTATION_FAILED);
        } finally {
            FeignSecurityManager.reset();
        }
    }
}
