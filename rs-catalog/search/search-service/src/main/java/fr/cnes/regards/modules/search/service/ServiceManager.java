/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.search.domain.LinkPluginsDatasets;
import fr.cnes.regards.modules.search.domain.ServiceScope;
import fr.cnes.regards.modules.search.plugin.IService;
import fr.cnes.regards.modules.search.service.link.ILinkPluginsDatasetsService;
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
     * Builds a pedicate telling if the passed {@link PluginConfiguration} is applicable on passed {@link ServiceScope}
     */
    private static final Function<ServiceScope, Predicate<PluginConfiguration>> IS_APPLICABLE_ON = pServiceScope -> configuration -> {
        try {
            switch (pServiceScope) {
                case QUERY:
                    return ((IService) Class.forName(configuration.getPluginClassName()).newInstance())
                            .isApplyableOnQuery();
                case ONE:
                    return ((IService) Class.forName(configuration.getPluginClassName()).newInstance())
                            .isApplyableOnOneData();
                case MANY:
                    return ((IService) Class.forName(configuration.getPluginClassName()).newInstance())
                            .isApplyableOnManyData();
                default:
                    throw new IllegalArgumentException(pServiceScope + " is not a valid value for the Service scope");
            }
        } catch (NullPointerException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            // No exception should occurs there. If any occurs it should set the application into maintenance mode so we
            // can safely rethrow as a runtime
            throw new PluginUtilsRuntimeException("Could not instanciate plugin", e);
        }
    };

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

    /**
     * Retrieve all PluginConfiguration in the system for plugins of type {@link IService} linked to a dataset for a
     * given scope
     *
     * @param pServiceScope
     *            scope we are interrested in
     * @param pDatasetId
     *            id of dataset
     * @return PluginConfigurations in the system for plugins of type {@link IService} linked to a dataset for a given
     *         scope
     * @throws EntityNotFoundException
     *             thrown is the pDatasetId does not represent any Dataset.
     */
    @Override
    public Set<PluginConfiguration> retrieveServices(final String pDatasetId, final ServiceScope pServiceScope)
            throws EntityNotFoundException {
        final LinkPluginsDatasets datasetPlugins = linkPluginsDatasetsService.retrieveLink(pDatasetId);
        final Set<PluginConfiguration> servicesConf = datasetPlugins.getServices();

        try (Stream<PluginConfiguration> stream = servicesConf.stream()) {
            return stream.filter(IS_APPLICABLE_ON.apply(pServiceScope)).collect(Collectors.toSet());
        }

    }

    @Override
    public ResponseEntity<?> apply(final String pDatasetId, final String pServiceName,
            final Map<String, String> pDynamicParameters) throws ModuleException {
        final PluginConfiguration conf = pluginService.getPluginConfigurationByLabel(pServiceName);
        // is it a Service configuration?
        if (!conf.getInterfaceNames().contains(IService.class.getName())) {
            throw new EntityInvalidException(
                    pServiceName + " is not a label of a " + IService.class.getName() + " plugin configuration");
        }
        // is it a service applyable to this dataset?
        if (!linkPluginsDatasetsService.retrieveLink(pDatasetId).getServices().contains(conf)) {
            throw new EntityInvalidException(
                    pServiceName + " is not a service applyable to the dataset with id " + pDatasetId);
        }
        pDynamicParameters.forEach(conf::setParameterDynamicValue);
        final IService toExecute = (IService) pluginService.getPlugin(conf);
        return toExecute.apply();
    }

}
