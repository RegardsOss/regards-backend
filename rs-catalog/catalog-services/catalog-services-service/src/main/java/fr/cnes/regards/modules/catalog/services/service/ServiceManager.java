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
package fr.cnes.regards.modules.catalog.services.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.catalog.services.domain.IService;
import fr.cnes.regards.modules.catalog.services.domain.LinkPluginsDatasets;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.catalog.services.domain.annotations.CatalogServicePlugin;
import fr.cnes.regards.modules.catalog.services.domain.dto.PluginConfigurationDto;
import fr.cnes.regards.modules.catalog.services.service.link.ILinkPluginsDatasetsService;
import fr.cnes.regards.plugins.utils.PluginUtilsRuntimeException;

/**
 * Class managing the execution of {@link IService} plugins
 *
 * @author Sylvain Vissiere-Guerinet
 */
@Service
@MultitenantTransactional
public class ServiceManager implements IServiceManager {

    /**
     * The service managing plugins
     */
    private final IPluginService pluginService;

    /**
     * Service linking plugins with datasets
     */
    private final ILinkPluginsDatasetsService linkPluginsDatasetsService;

    /**
     * Finds the application mode of the given plugin configuration
     */
    private static final Function<PluginConfiguration, CatalogServicePlugin> GET_CATALOG_SERVICE_PLUGIN_ANNOTATION = pPluginConfiguration -> {
        try {
            return AnnotationUtils.findAnnotation(Class.forName(pPluginConfiguration.getPluginClassName()),
                                                  CatalogServicePlugin.class);
        } catch (ClassNotFoundException e) {
            // No exception should occurs there. If any occurs it should set the application into maintenance mode so we
            // can safely rethrow as a runtime
            throw new PluginUtilsRuntimeException("Could not instanciate plugin", e);
        }
    };

    /**
     * Builds a pedicate telling if the passed {@link PluginConfiguration} is applicable on passed {@link ServiceScope}.
     * Returns <code>true</code> if passed <code>pServiceScope</code> is <code>null</code>.
     */
    private static final Function<ServiceScope, Predicate<PluginConfiguration>> IS_APPLICABLE_ON = pServiceScope -> configuration -> (pServiceScope == null)
            || Arrays.asList(GET_CATALOG_SERVICE_PLUGIN_ANNOTATION.apply(configuration).applicationModes())
                    .contains(pServiceScope);

    /**
     * For a {@link PluginConfiguration}, return its corresponding DTO, in which we have added fields <code>applicationModes</code>
     * and <code>entityTypes</code>
     */
    private static final Function<PluginConfiguration, PluginConfigurationDto> PLUGIN_CONFIGURATION_TO_DTO = pPluginConfiguration -> new PluginConfigurationDto(
            pPluginConfiguration,
            Sets.newHashSet(GET_CATALOG_SERVICE_PLUGIN_ANNOTATION.apply(pPluginConfiguration).applicationModes()),
            Sets.newHashSet(GET_CATALOG_SERVICE_PLUGIN_ANNOTATION.apply(pPluginConfiguration).entityTypes()));

    /**
     * Constructor
     *
     * @param pPluginService
     *            the service managing plugins
     * @param pLinkPluginsDatasetsService
     *            service linking plugins with datasets
     */
    public ServiceManager(final IPluginService pPluginService,
            final ILinkPluginsDatasetsService pLinkPluginsDatasetsService) {
        pluginService = pPluginService;
        linkPluginsDatasetsService = pLinkPluginsDatasetsService;
    }

    @Override
    public List<PluginConfigurationDto> retrieveServices(String pDatasetId, final ServiceScope pServiceScope) {
        final LinkPluginsDatasets datasetPlugins = linkPluginsDatasetsService.retrieveLink(pDatasetId);
        final Set<PluginConfiguration> services = datasetPlugins.getServices();

        try (Stream<PluginConfiguration> stream = services.stream()) {
            return stream.filter(IS_APPLICABLE_ON.apply(pServiceScope)).map(PLUGIN_CONFIGURATION_TO_DTO)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public ResponseEntity<?> apply(final String pDatasetId, final String pServiceName,
            final Map<String, String> dynamicParameters) throws ModuleException {
        final PluginConfiguration conf = pluginService.getPluginConfigurationByLabel(pServiceName);
        // is it a Service configuration?
        if (!conf.getInterfaceNames().contains(IService.class.getName())) {
            throw new EntityInvalidException(
                    pServiceName + " is not a label of a " + IService.class.getName() + " plugin configuration");
        }
        // is it a service applyable to this dataset?
        if (!linkPluginsDatasetsService.retrieveLink(pDatasetId).getServices().contains(conf)) {
            throw new EntityInvalidException(
                    pServiceName + " is not a service applicable to the dataset with id " + pDatasetId);
        }

        // Build dynamic parameters
        PluginParametersFactory factory = PluginParametersFactory.build();
        dynamicParameters.forEach(factory::addParameterDynamic);

        IService toExecute = (IService) pluginService
                .getPlugin(conf, factory.getParameters().toArray(new PluginParameter[factory.getParameters().size()]));
        return toExecute.apply();
    }

}
