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
package fr.cnes.regards.modules.catalog.services.service;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.dto.PluginParamDescriptor;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.PluginParamType;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.plugins.PluginParameterUtils;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.catalog.services.domain.LinkPluginsDatasets;
import fr.cnes.regards.modules.catalog.services.domain.ServicePluginParameters;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.catalog.services.domain.annotations.CatalogServicePlugin;
import fr.cnes.regards.modules.catalog.services.domain.annotations.GetCatalogServicePluginAnnotation;
import fr.cnes.regards.modules.catalog.services.domain.dto.PluginConfigurationDto;
import fr.cnes.regards.modules.catalog.services.domain.plugins.IService;
import fr.cnes.regards.modules.catalog.services.plugins.AbstractCatalogServicePlugin;
import fr.cnes.regards.modules.catalog.services.service.link.ILinkPluginsDatasetsService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class managing the execution of {@link IService} plugins
 *
 * @author Sylvain Vissiere-Guerinet
 */
@Service
@MultitenantTransactional
public class ServiceManager implements IServiceManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceManager.class);

    /**
     * Finds the application mode of the given plugin configuration
     */
    private static final Function<PluginConfiguration, CatalogServicePlugin> GET_CATALOG_SERVICE_PLUGIN_ANNOTATION = new GetCatalogServicePluginAnnotation();

    /**
     * Builds a predicate telling if the passed {@link PluginConfiguration} is applicable on passed {@link ServiceScope}.
     * Returns <code>true</code> if passed <code>pServiceScope</code> is <code>null</code>.
     */
    private static final Function<List<ServiceScope>, Predicate<PluginConfiguration>> IS_APPLICABLE_ON = serviceScope -> configuration ->
        (serviceScope == null)
        || Arrays.asList(GET_CATALOG_SERVICE_PLUGIN_ANNOTATION.apply(configuration).applicationModes())
                 .containsAll(serviceScope);

    /**
     * The service managing plugins
     */
    private final IPluginService pluginService;

    /**
     * Service linking plugins with datasets
     */
    private final ILinkPluginsDatasetsService linkPluginsDatasetsService;

    /**
     * Constructor
     *
     * @param pluginService              the service managing plugins
     * @param linkPluginsDatasetsService service linking plugins with datasets
     */
    public ServiceManager(final IPluginService pluginService,
                          final ILinkPluginsDatasetsService linkPluginsDatasetsService) {
        this.pluginService = pluginService;
        this.linkPluginsDatasetsService = linkPluginsDatasetsService;
    }

    @Override
    public List<PluginConfigurationDto> retrieveServices(List<String> datasetIds, List<ServiceScope> serviceScopes) {
        Set<PluginConfiguration> allServices = getServicesAssociatedToAllDatasets();

        if ((datasetIds != null) && !datasetIds.isEmpty()) {
            Set<PluginConfiguration> datasetsCommonServices = Sets.newHashSet();
            boolean first = true;
            // Get all services associated to each dataset given
            for (String datasetId : datasetIds) {
                final LinkPluginsDatasets datasetPlugins = linkPluginsDatasetsService.retrieveLink(datasetId);
                final Set<PluginConfiguration> datasetServices = datasetPlugins.getServices();
                if (first) {
                    datasetsCommonServices.addAll(datasetServices);
                    first = false;
                } else {
                    datasetsCommonServices.retainAll(datasetServices);
                }
            }
            for (PluginConfiguration datasetService : datasetsCommonServices) {
                if (!allServices.contains(datasetService)) {
                    allServices.add(datasetService);
                }
            }
        }

        try (Stream<PluginConfiguration> stream = allServices.stream()) {
            return stream.filter(IS_APPLICABLE_ON.apply(serviceScopes))
                         .map(PluginConfigurationDto::new)
                         .collect(Collectors.toList());
        }
    }

    @Override
    public ResponseEntity<StreamingResponseBody> apply(final String pluginConfigurationBusinessId,
                                                       final ServicePluginParameters servicePluginParameters,
                                                       HttpServletResponse response) throws ModuleException {

        LOGGER.info("Applying plugin service {}", pluginConfigurationBusinessId);
        final PluginConfiguration conf = pluginService.getPluginConfiguration(pluginConfigurationBusinessId);
        // is it a Service configuration?
        if (!conf.getInterfaceNames().contains(IService.class.getName())) {
            throw new EntityInvalidException(pluginConfigurationBusinessId
                                             + " is not a "
                                             + IService.class.getName()
                                             + " plugin configuration");
        }
        // is it a service applyable to this dataset?
        // TODO : Check if the current service is applicable for the given entities (throught the dataset associated)

        // Build dynamic parameters
        Set<IPluginParam> parameters = new HashSet<>();
        if (servicePluginParameters.getDynamicParameters() != null) {
            servicePluginParameters.getDynamicParameters().forEach((k, v) -> {
                Optional<PluginParamDescriptor> param = conf.getMetaData()
                                                            .getParameters()
                                                            .stream()
                                                            .filter(p -> p.getName().equals(k))
                                                            .findFirst();
                if (param.isPresent()) {
                    parameters.add(PluginParameterUtils.forType(param.get().getType(), k, v, true));
                } else {
                    LOGGER.warn("Invalid dynamic parameter  {} for plugin {} of type {}",
                                k,
                                pluginConfigurationBusinessId,
                                conf.getPluginId());
                    parameters.add(IPluginParam.build(k, v).dynamic());
                }
            });
        }

        IService toExecute;
        try {
            toExecute = pluginService.getPlugin(pluginConfigurationBusinessId,
                                                Iterables.toArray(parameters, IPluginParam.class));
        } catch (NotAvailablePluginConfigurationException e) {
            throw new ModuleException("Unable to apply disabled service.", e);
        }
        LOGGER.info("Applying plugin service {}", toExecute.getClass().getName());
        return toExecute.apply(servicePluginParameters, response);

    }

    private Set<PluginConfiguration> getServicesAssociatedToAllDatasets() {
        Set<PluginConfiguration> allServices = Sets.newHashSet();
        // 1. Retrieve all services configuration
        List<PluginConfiguration> confs = pluginService.getPluginConfigurationsByType(IService.class);

        // 2. Get all plugin conf with the applyToAllDataset parameter set to true.
        for (PluginConfiguration conf : confs) {
            IPluginParam param = conf.getParameter(AbstractCatalogServicePlugin.APPLY_TO_ALL_DATASETS_PARAM);
            if ((param != null) && (param.getType() == PluginParamType.BOOLEAN) && ((Boolean) param.getValue())) {
                allServices.add(conf);
            }
        }
        return allServices;
    }

}
