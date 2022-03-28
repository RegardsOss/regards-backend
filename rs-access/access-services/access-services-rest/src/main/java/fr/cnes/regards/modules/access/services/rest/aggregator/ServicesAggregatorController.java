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
package fr.cnes.regards.modules.access.services.rest.aggregator;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.access.services.domain.aggregator.PluginServiceDto;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginConfiguration;
import fr.cnes.regards.modules.access.services.rest.assembler.PluginServiceDtoResourcesAssembler;
import fr.cnes.regards.modules.access.services.service.ui.IUIPluginConfigurationService;
import fr.cnes.regards.modules.catalog.services.client.ICatalogServicesClient;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.catalog.services.domain.dto.PluginConfigurationDto;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;

/**
 * This controller returns aggregations of UI services and Catalog services.
 * @author Xavier-Alexandre Brochard
 */
@RestController
@RequestMapping(ServicesAggregatorController.ROOT_PATH)
public class ServicesAggregatorController {

    public static final String ROOT_PATH = "/services/aggregated";

    /**
     * The client providing catalog services
     */
    private final ICatalogServicesClient catalogServicesClient;

    /**
     * The service provinding ui services
     */
    private final IUIPluginConfigurationService uiPluginConfigurationService;

    /**
     * The resource assembler for {@link PluginServiceDto}s
     */
    private final PluginServiceDtoResourcesAssembler assembler;

    /**
     * @param catalogServicesClient
     * @param uiPluginConfigurationService
     * @param assembler
     */
    public ServicesAggregatorController(ICatalogServicesClient catalogServicesClient,
            IUIPluginConfigurationService uiPluginConfigurationService, PluginServiceDtoResourcesAssembler assembler) {
        super();
        this.catalogServicesClient = catalogServicesClient;
        this.uiPluginConfigurationService = uiPluginConfigurationService;
        this.assembler = assembler;
    }

    /**
     * Returns all services applied to all datasets plus those of the given dataset
     * @param datasetIpIds the ip ids of the {@link Dataset}s
     * @param applicationModes the set of {@link ServiceScope}
     * @return the list of services configured for the given dataset and the given scope
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "Returns services applied to all datasets plus those of the given dataset",
            role = DefaultRole.PUBLIC)
    public ResponseEntity<List<EntityModel<PluginServiceDto>>> retrieveServices(
            @RequestParam(value = "datasetIpIds", required = false) final List<String> datasetIpIds,
            @RequestParam(value = "applicationModes", required = false) final List<ServiceScope> applicationModes) {
        // Retrieve catalog services
        ResponseEntity<List<EntityModel<PluginConfigurationDto>>> catalogServices = catalogServicesClient
                .retrieveServices(datasetIpIds, applicationModes);
        // Retrive ui services
        List<UIPluginConfiguration> uiServices = uiPluginConfigurationService
                .retrieveActivePluginServices(datasetIpIds, applicationModes);

        try (Stream<PluginConfigurationDto> streamCatalogServices = HateoasUtils
                .unwrapCollection(catalogServices.getBody()).stream();
                Stream<UIPluginConfiguration> streamUiServices = uiServices.stream()) {
            // Map catalog service to dto
            Stream<PluginServiceDto> streamCatalogServicesDto = streamCatalogServices
                    .map(PluginServiceDto::fromPluginConfigurationDto);
            // Map ui service to dto
            Stream<PluginServiceDto> streamUiServicesDto = streamUiServices
                    .map(PluginServiceDto::fromUIPluginConfiguration);
            // Merge streams
            List<PluginServiceDto> results = Stream.concat(streamCatalogServicesDto, streamUiServicesDto)
                    .collect(Collectors.toList());

            return new ResponseEntity<>(assembler.toResources(results), HttpStatus.OK);
        }
    }

}
