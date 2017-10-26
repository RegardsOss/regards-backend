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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletResponse;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.catalog.services.domain.LinkPluginsDatasets;
import fr.cnes.regards.modules.catalog.services.domain.ServicePluginParameters;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.catalog.services.domain.annotations.CatalogServicePlugin;
import fr.cnes.regards.modules.catalog.services.domain.annotations.GetCatalogServicePluginAnnotation;
import fr.cnes.regards.modules.catalog.services.domain.dto.PluginConfigurationDto;
import fr.cnes.regards.modules.catalog.services.domain.plugins.IService;
import fr.cnes.regards.modules.catalog.services.plugins.SampleServicePlugin;
import fr.cnes.regards.modules.catalog.services.service.link.ILinkPluginsDatasetsService;

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
    private static final Function<PluginConfiguration, CatalogServicePlugin> GET_CATALOG_SERVICE_PLUGIN_ANNOTATION = new GetCatalogServicePluginAnnotation();

    /**
     * Builds a pedicate telling if the passed {@link PluginConfiguration} is applicable on passed {@link ServiceScope}.
     * Returns <code>true</code> if passed <code>pServiceScope</code> is <code>null</code>.
     */
    private static final Function<ServiceScope, Predicate<PluginConfiguration>> IS_APPLICABLE_ON = pServiceScope -> configuration -> (pServiceScope == null)
            || Arrays.asList(GET_CATALOG_SERVICE_PLUGIN_ANNOTATION.apply(configuration).applicationModes())
                    .contains(pServiceScope);

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
        pluginService.addPluginPackage(IService.class.getPackage().getName());
        pluginService.addPluginPackage(SampleServicePlugin.class.getPackage().getName());
    }

    @Override
    public List<PluginConfigurationDto> retrieveServices(String pDatasetId, final ServiceScope pServiceScope) {
        if (pDatasetId == null) {
            return new ArrayList<>();
        }
        final LinkPluginsDatasets datasetPlugins = linkPluginsDatasetsService.retrieveLink(pDatasetId);
        final Set<PluginConfiguration> services = datasetPlugins.getServices();

        try (Stream<PluginConfiguration> stream = services.stream()) {
            return stream.filter(IS_APPLICABLE_ON.apply(pServiceScope)).map(PluginConfigurationDto::new)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public ResponseEntity<InputStreamResource> apply(final Long pPluginConfigurationId,
            final ServicePluginParameters pServicePluginParameters, HttpServletResponse response)
            throws ModuleException {
        final PluginConfiguration conf = pluginService.getPluginConfiguration(pPluginConfigurationId);
        // is it a Service configuration?
        if (!conf.getInterfaceNames().contains(IService.class.getName())) {
            throw new EntityInvalidException(
                    pPluginConfigurationId + " is not a " + IService.class.getName() + " plugin configuration");
        }
        // is it a service applyable to this dataset?
        // TODO : Check if the current service is applicable for the given entities (throught the dataset associated)

        // Build dynamic parameters
        PluginParametersFactory factory = PluginParametersFactory.build();
        if (pServicePluginParameters.getDynamicParameters() != null) {
            pServicePluginParameters.getDynamicParameters().forEach(factory::addParameterDynamic);
        }

        IService toExecute = (IService) pluginService
                .getPlugin(pPluginConfigurationId,
                           factory.getParameters().toArray(new PluginParameter[factory.getParameters().size()]));
        return toExecute.apply(pServicePluginParameters, response);

    }

}
