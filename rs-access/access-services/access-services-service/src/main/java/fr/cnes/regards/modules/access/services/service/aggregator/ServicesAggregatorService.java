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
package fr.cnes.regards.modules.access.services.service.aggregator;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.access.services.domain.aggregator.PluginServiceDto;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginConfiguration;
import fr.cnes.regards.modules.access.services.service.aggregator.cache.ServiceAggregatorKeyGenerator;
import fr.cnes.regards.modules.access.services.service.ui.IUIPluginConfigurationService;
import fr.cnes.regards.modules.catalog.services.client.ICatalogServicesClient;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.catalog.services.domain.dto.PluginConfigurationDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@MultitenantTransactional
public class ServicesAggregatorService implements IServicesAggregatorService {

    /**
     * The client providing catalog services
     */
    private final ICatalogServicesClient catalogServicesClient;

    /**
     * The service provinding ui services
     */
    private final IUIPluginConfigurationService uiPluginConfigurationService;

    public ServicesAggregatorService(ICatalogServicesClient catalogServicesClient,
                                     IUIPluginConfigurationService uiPluginConfigurationService) {
        this.catalogServicesClient = catalogServicesClient;
        this.uiPluginConfigurationService = uiPluginConfigurationService;
    }

    @Override
    @Cacheable(value = ServiceAggregatorKeyGenerator.CACHE_NAME,
        keyGenerator = ServiceAggregatorKeyGenerator.KEY_GENERATOR, sync = true)
    public List<PluginServiceDto> retrieveServices(final List<String> datasetIpIds,
                                                   final List<ServiceScope> applicationModes) {
        // Retrieve catalog services
        ResponseEntity<List<EntityModel<PluginConfigurationDto>>> catalogServices = catalogServicesClient.retrieveServices(
            datasetIpIds,
            applicationModes);
        // Retrive ui services
        List<UIPluginConfiguration> uiServices = uiPluginConfigurationService.retrieveActivePluginServices(datasetIpIds,
                                                                                                           applicationModes);

        try (Stream<PluginConfigurationDto> streamCatalogServices = HateoasUtils.unwrapCollection(catalogServices.getBody())
            .stream(); Stream<UIPluginConfiguration> streamUiServices = uiServices.stream()) {
            // Map catalog service to dto
            Stream<PluginServiceDto> streamCatalogServicesDto = streamCatalogServices.map(PluginServiceDto::fromPluginConfigurationDto);
            // Map ui service to dto
            Stream<PluginServiceDto> streamUiServicesDto = streamUiServices.map(PluginServiceDto::fromUIPluginConfiguration);
            // Merge streams
            return Stream.concat(streamCatalogServicesDto, streamUiServicesDto).collect(Collectors.toList());
        }
    }
}
