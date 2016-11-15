/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.authentication.internal.service;

import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import fr.cnes.regards.cloud.gateway.authentication.plugins.IAuthenticationPlugin;
import fr.cnes.regards.framework.module.rest.exception.ModuleEntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.plugins.service.IPluginService;
import fr.cnes.regards.plugins.utils.PluginUtilsException;

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
    private static final Logger LOG = LoggerFactory.getLogger(InternalAuthenticationPluginService.class);

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
        pluginService.addPluginPackage("fr.cnes.regards.cloud.gateway.authentication.plugins");
    }

    @Override
    public List<PluginConfiguration> retrieveIdentityProviderPlugins() {
        return pluginService.getPluginConfigurationsByType(IAuthenticationPlugin.class);
    }

    @Override
    public PluginConfiguration retrieveIdentityProviderPlugin(final Long pPluginConfigurationId)
            throws ModuleEntityNotFoundException {
        try {
            return pluginService.getPluginConfiguration(pPluginConfigurationId);
        } catch (final PluginUtilsException e) {
            LOG.error(e.getMessage(), e);
            throw new ModuleEntityNotFoundException(pPluginConfigurationId.toString(), PluginConfiguration.class);
        }
    }

    @Override
    public PluginConfiguration createIdentityProviderPlugin(final PluginConfiguration pPluginConfigurationToCreate)
            throws ModuleException {
        try {
            return pluginService.savePluginConfiguration(pPluginConfigurationToCreate);
        } catch (final PluginUtilsException e) {
            LOG.error(e.getMessage(), e);
            throw new ModuleException(e.getMessage());
        }

    }

    @Override
    public PluginConfiguration updateIdentityProviderPlugin(final PluginConfiguration pPluginConfigurationToUpdate)
            throws ModuleException {
        try {
            return pluginService.updatePluginConfiguration(pPluginConfigurationToUpdate);
        } catch (final PluginUtilsException e) {
            LOG.error(e.getMessage(), e);
            throw new ModuleException(e.getMessage());
        }
    }

    @Override
    public void deleteIdentityProviderPlugin(final Long pPluginConfigurationId) throws ModuleEntityNotFoundException {
        pluginService.deletePluginConfiguration(pPluginConfigurationId);
    }

}
