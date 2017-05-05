/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.authentication.service;

import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.authentication.plugins.IAuthenticationPlugin;

/**
 *
 * Class InternalAuthenticationPluginService
 *
 * Internal authentication plugins manager
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Service
public class InternalAuthenticationPluginService implements IInternalAuthenticationPluginsService {

    /**
     * Class logger
     */
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(InternalAuthenticationPluginService.class);

    /**
     * Plugins service manager
     */
    private final IPluginService pluginService;

    public InternalAuthenticationPluginService(final IPluginService pPluginService) {
        super();
        pluginService = pPluginService;
    }

    @PostConstruct
    public void addPluginPackage() {
        pluginService.addPluginPackage(IAuthenticationPlugin.class.getPackage().getName());
    }

    @Override
    public List<PluginConfiguration> retrieveIdentityProviderPlugins() {
        return pluginService.getPluginConfigurationsByType(IAuthenticationPlugin.class);
    }

    @Override
    public PluginConfiguration retrieveIdentityProviderPlugin(final Long pPluginConfigurationId)
            throws ModuleException {

        return pluginService.getPluginConfiguration(pPluginConfigurationId);
    }

    @Override
    public PluginConfiguration createIdentityProviderPlugin(final PluginConfiguration pPluginConfigurationToCreate)
            throws ModuleException {
        return pluginService.savePluginConfiguration(pPluginConfigurationToCreate);
    }

    @Override
    public PluginConfiguration updateIdentityProviderPlugin(final PluginConfiguration pPluginConfigurationToUpdate)
            throws ModuleException {
        return pluginService.updatePluginConfiguration(pPluginConfigurationToUpdate);
    }

    @Override
    public void deleteIdentityProviderPlugin(final Long pPluginConfigurationId) throws ModuleException {
        pluginService.deletePluginConfiguration(pPluginConfigurationId);
    }

}
