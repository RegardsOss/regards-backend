/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.access.services.domain.aggregator.PluginServiceDto;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginConfiguration;
import fr.cnes.regards.modules.access.services.service.ui.IUIPluginConfigurationService;
import fr.cnes.regards.modules.catalog.services.client.ICatalogServicesClient;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.catalog.services.domain.dto.PluginConfigurationDto;
import fr.cnes.regards.modules.entities.domain.Dataset;

/**
 * This controller returns aggregations of UI services and Catalog services.
 *
 * @author Xavier-Alexandre Brochard
 */
@RestController
@RequestMapping(ServicesAggregatorController.ROOT_PATH)
public class ServicesAggregatorController {

    public static final String ROOT_PATH = "/services";

    /**
     * This function builds a predicate on a {@link PluginServiceDto} which yields true if it has some application modes in the given set
     */
    private static final Function<ServiceScope, Predicate<PluginServiceDto>> IS_APPLICABLE_TO = pApplicationMode -> pPluginServiceDto -> pPluginServiceDto
            .getApplicationModes().contains(pApplicationMode));

    /**
     * The client providing catalog services
     */
    @Autowired
    private ICatalogServicesClient catalogServicesClient;

    /**
     * The service provinding ui services
     */
    @Autowired
    private IUIPluginConfigurationService uiPluginConfigurationService;

    /**
     * Returns all services applied to all datasets plus those of the given dataset
     *
     * @param pDatasetId
     *            the id of the {@link Dataset}
     * @param pApplicationModes
     *            the set of {@link ServiceScope}
     * @return the list of services configured for the given dataset and the given scope
     * @throws EntityNotFoundException
     */
    @RequestMapping(method = RequestMethod.GET, path = "/aggregated")
    @ResourceAccess(description = "Returns services applied to all datasets plus those of the given dataset")
    public ResponseEntity<List<PluginServiceDto>> retrieveServices(
            @RequestParam(value = "datasetIpId", required = false) final String pDatasetId,
            @RequestParam(value = "applicationMode", required = false) final ServiceScope pApplicationMode) {
        // Retrieve catalog services
        ResponseEntity<List<Resource<PluginConfigurationDto>>> catalogServices = catalogServicesClient
                .retrieveServices(pDatasetId, pApplicationMode);
        // Retrive ui services
        List<UIPluginConfiguration> uiServices = uiPluginConfigurationService.retrieveActivePluginServices(pDatasetId, pApplicationMode);
        // Pre-compute the application modes filter
        Predicate<PluginServiceDto> isApplicableToGivenModes = IS_APPLICABLE_TO.apply(pApplicationMode);

        try (Stream<PluginConfigurationDto> streamCatalogServices = HateoasUtils
                .unwrapCollection(catalogServices.getBody()).stream();
                Stream<UIPluginConfiguration> streamUiServices = uiServices.stream()) {
            // Map catalog service to dto
            Stream<PluginServiceDto> streamCatalogServicesDto = streamCatalogServices
                    .map(PluginServiceDto::fromPluginConfiguration);
            // Map ui service to dto
            Stream<PluginServiceDto> streamUiServicesDto = streamUiServices
                    .map(PluginServiceDto::fromUIPluginConfiguration)
            // Merge streams
            List<PluginServiceDto> results = Stream.concat(streamCatalogServicesDto, streamUiServicesDto)
                    .collect(Collectors.toList());
            return new ResponseEntity<>(results, HttpStatus.OK);
        }

    }

}
