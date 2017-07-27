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

/**
 * This controller returns aggregations of UI services and Catalog services.
 *
 * @author Xavier-Alexandre Brochard
 */
//@RestController
//@RequestMapping(ServicesAggregatorController.ROOT_PATH)
public class ServicesAggregatorController {

    //    public static final String ROOT_PATH = "/aggregator/services";
    //
    //    /**
    //     * This function builds a predicate on a {@link PluginServiceDto} which yields true if it has some application modes in the given set
    //     */
    //    private static final Function<Set<ServiceScope>, Predicate<PluginServiceDto>> IS_APPLICABLE_TO = pApplicationModes -> pPluginServiceDto -> !Sets
    //            .intersection(pApplicationModes, pPluginServiceDto.getApplicationModes()).isEmpty());
    //
    //    /**
    //     * The client providing catalog services
    //     */
    //    @Autowired
    //    private ICatalogServicesClient catalogServicesClient;
    //
    //    /**
    //     * The service provinding ui services
    //     */
    //    @Autowired
    //    private IUIPluginConfigurationService uiPluginConfigurationService;
    //
    //    /**
    //     * Returns services applied to all datasets plus those of the given dataset
    //     *
    //     * @param pDatasetId
    //     *            the id of the {@link Dataset}
    //     * @param pApplicationModes
    //     *            the set of {@link ServiceScope}
    //     * @return the list of services configured for the given dataset and the given scope
    //     * @throws EntityNotFoundException
    //     */
    //    @RequestMapping(method = RequestMethod.GET)
    //    @ResourceAccess(description = "Returns services applied to all datasets plus those of the given dataset")
    //    public ResponseEntity<List<PluginServiceDto>> retrieveServices(@PathVariable("datasetIpId") final String pDatasetId,
    //            @RequestParam("applicationModes") final Set<ServiceScope> pApplicationModes) {
    //        // Retrieve catalog services
    //        ResponseEntity<Collection<Resource<PluginConfigurationDto>>> catalogServices = catalogServicesClient
    //                .retrieveServicesWithMeta(pDatasetId);
    //        // Retrive ui services
    //        List<UIPluginConfiguration> uiServices = uiPluginConfigurationService.retrieveActivePluginServices(pDatasetId);
    //        // Pre-compute the application modes filter
    //        Predicate<PluginServiceDto> isApplicableToGivenModes = IS_APPLICABLE_TO.apply(pApplicationModes);
    //
    //        try (Stream<PluginConfigurationDto> streamCatalogServices = HateoasUtils
    //                .unwrapCollection(catalogServices.getBody()).stream();
    //                Stream<UIPluginConfiguration> streamUiServices = uiServices.stream()) {
    //            // Map catalog service to dto
    //            Stream<PluginServiceDto> streamCatalogServicesDto = streamCatalogServices
    //                    .map(PluginServiceDto::fromPluginConfiguration).filter(isApplicableToGivenModes);
    //            // Map ui service to dto
    //            Stream<PluginServiceDto> streamUiServicesDto = streamUiServices
    //                    .map(PluginServiceDto::fromUIPluginConfiguration).filter(isApplicableToGivenModes);
    //            // Merge streams
    //            List<PluginServiceDto> results = Stream.concat(streamCatalogServicesDto, streamUiServicesDto)
    //                    .collect(Collectors.toList());
    //            return new ResponseEntity<>(results, HttpStatus.OK);
    //        }
    //
    //    }

}
