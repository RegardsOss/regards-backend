/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.search.domain.IService;

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

    public ServiceManager(IPluginService pPluginService) {
        pluginService = pPluginService;
    }

    /**
     * retrieve all PluginConfiguration in the system for plugins of type {@link IService}
     *
     * @return
     */
    public List<PluginConfiguration> retrieveServices() {
        return pluginService.getPluginConfigurationsByType(IService.class);
    }

    public Set<DataObject> apply(String pServiceName, Map<String, String> pDynamicParameters, String pQuery)
            throws ModuleException {
        PluginConfiguration conf = pluginService.getPluginConfigurationByLabel(pServiceName);
        if (!conf.getInterfaceName().equals(IService.class.getName())) {
            throw new EntityInvalidException(
                    pServiceName + " is not a label of a " + pServiceName + " plugin configuration");
        }
        pDynamicParameters.forEach(conf::setParameterDynamicValue);
        IService toExecute = (IService) pluginService.getPlugin(conf);
        return toExecute.apply(pQuery);
    }

}
