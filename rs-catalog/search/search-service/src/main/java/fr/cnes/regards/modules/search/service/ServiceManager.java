/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.search.domain.IService;
import fr.cnes.regards.modules.search.domain.LinkPluginsDatasets;
import fr.cnes.regards.modules.search.domain.ServiceScope;
import fr.cnes.regards.modules.search.service.link.ILinkPluginsDatasetsService;

/**
 * Class managing the execution of {@link IService} plugins
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
@Service
@MultitenantTransactional
public class ServiceManager {

    private final IPluginService pluginService;

    private final ILinkPluginsDatasetsService linkPluginsDatasetsService;

    public ServiceManager(IPluginService pPluginService, ILinkPluginsDatasetsService pLinkPluginsDatasetsService) {
        pluginService = pPluginService;
        linkPluginsDatasetsService = pLinkPluginsDatasetsService;
    }

    /**
     * retrieve all PluginConfiguration in the system for plugins of type {@link IService} linked to a dataset for a
     * given scope
     *
     * @param pServiceScope
     *            scope we are interrested in
     * @param pDatasetId
     *            id of dataset
     *
     * @return PluginConfigurations in the system for plugins of type {@link IService} linked to a dataset for a given
     *         scope
     * @throws EntityNotFoundException
     *             thrown is the pDatasetId does not represent any Dataset.
     */
    public Set<PluginConfiguration> retrieveServices(Long pDatasetId, ServiceScope pServiceScope)
            throws EntityNotFoundException {
        LinkPluginsDatasets datasetPlugins = linkPluginsDatasetsService.retrieveLink(pDatasetId);
        Set<PluginConfiguration> servicesConf = datasetPlugins.getServices();
        switch (pServiceScope) {
            case QUERY:
                return servicesConf.stream().filter(sc -> {
                    try {
                        return ((IService) Class.forName(sc.getPluginClassName()).newInstance()).isApplyableOnQuery();
                    } catch (Exception e) {
                        // no exception should occurs there. If any occurs it should set the application into
                        // maintenance mode so we can safely rethrow as a runtime
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toSet());

            case ONE:
                return servicesConf.stream().filter(sc -> {
                    try {
                        return ((IService) Class.forName(sc.getPluginClassName()).newInstance()).isApplyableOnOneData();
                    } catch (Exception e) {
                        // no exception should occurs there. If any occurs it should set the application into
                        // maintenance mode so we can safely rethrow as a runtime
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toSet());

            case MANY:
                return servicesConf.stream().filter(sc -> {
                    try {
                        return ((IService) Class.forName(sc.getPluginClassName()).newInstance())
                                .isApplyableOnManyData();
                    } catch (Exception e) {
                        // no exception should occurs there. If any occurs it should set the application into
                        // maintenance mode so we can safely rethrow as a runtime
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toSet());
            default:
                throw new IllegalArgumentException(pServiceScope + " is not a valid value for the Service scope");
        }
    }

    public ResponseEntity<?> apply(Long pDatasetId, String pServiceName, Map<String, String> pDynamicParameters)
            throws ModuleException {
        PluginConfiguration conf = pluginService.getPluginConfigurationByLabel(pServiceName);
        // is it a Service configuration?
        if (!conf.getInterfaceName().equals(IService.class.getName())) {
            throw new EntityInvalidException(
                    pServiceName + " is not a label of a " + pServiceName + " plugin configuration");
        }
        // is it a service applyable to this dataset?
        if (!linkPluginsDatasetsService.retrieveLink(pDatasetId).getServices().contains(conf)) {
            throw new EntityInvalidException(
                    pServiceName + " is not a service applyable to the dataset with id " + pDatasetId);
        }
        pDynamicParameters.forEach(conf::setParameterDynamicValue);
        IService toExecute = (IService) pluginService.getPlugin(conf);
        return toExecute.apply();
    }

}
