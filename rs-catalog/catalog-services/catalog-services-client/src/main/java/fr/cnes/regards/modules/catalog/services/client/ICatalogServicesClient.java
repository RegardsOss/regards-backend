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
package fr.cnes.regards.modules.catalog.services.client;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.catalog.services.domain.dto.PluginConfigurationDto;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Feign client for calling rs-catalog's CatalogServicesController
 *
 * @author Xavier-Alexandre Brochard
 */
@RestClient(name = "rs-catalog", contextId = "rs-catalog.services-client")
public interface ICatalogServicesClient {

    String ROOT_PATH = "/services";

    /**
     * Call rs-catalog's CatalogServicesController#retrieveServices
     *
     * @param datasetId    the id of the Dataset. Can be <code>null</code>.
     * @param serviceScope the applicable mode. Can be <code>null</code>.
     * @return the list of services
     */
    @GetMapping(path = ROOT_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<EntityModel<PluginConfigurationDto>>> retrieveServices(
            @RequestParam(value = "datasetIpIds", required = false) final List<String> datasetIds,
            @RequestParam(value = "applicationModes", required = false) final List<ServiceScope> serviceScopes);

}