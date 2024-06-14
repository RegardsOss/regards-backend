/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.plugins.service;

import fr.cnes.regards.framework.encryption.IEncryptionService;
import fr.cnes.regards.framework.encryption.exception.EncryptionException;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.dto.PluginConfigurationDto;
import fr.cnes.regards.framework.modules.plugins.dto.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.dto.PluginParamDescriptor;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.NestedPluginParam;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.PluginParamType;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.StringPluginParam;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Service to instantiate plugins from the provided {@link PluginConfiguration}.
 */
@Service
public class PluginInstantiationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginInstantiationService.class);

    private final PluginConfigurationService pluginDaoService;

    private final IEncryptionService encryptionService;

    public PluginInstantiationService(PluginConfigurationService pluginDaoService, IEncryptionService encryptionService) {
        this.pluginDaoService = pluginDaoService;
        this.encryptionService = encryptionService;
    }

    /**
     * Instantiate a plugin.
     *
     * @param pluginConf        {@link PluginConfiguration} plugin configuration
     * @param tenantPluginCache plugin cache for this tenant
     * @param dynamicParameters plugin parameters (including potential dynamic ones)
     * @return plugin instance
     */
    @MultitenantTransactional(noRollbackFor = { ModuleException.class })
    public <T> T instantiatePlugin(PluginConfiguration pluginConf,
                                   ConcurrentMap<String, Object> tenantPluginCache,
                                   IPluginParam... dynamicParameters)
        throws ModuleException {
        // Check if all parameters are really dynamic
        for (IPluginParam dynamicParameter : dynamicParameters) {
            if (!dynamicParameter.isDynamic()) {
                String errorMessage = String.format(
                    "The parameter \"%s\" is not identified as dynamic. Plugin instantiation is cancelled.",
                    dynamicParameter.getName());
                LOGGER.error(errorMessage);
                throw new UnexpectedDynamicParameterException(errorMessage);
            }
        }

        // Get the plugin implementation associated
        PluginMetaData pluginMetadata = getPluginMetadata(pluginConf.getPluginId());

        if (!Objects.equals(pluginMetadata.getVersion(), pluginConf.getVersion())) {
            throw new CannotInstanciatePluginException(String.format(
                "Plugin configuration version (%s) is different from plugin one (%s).",
                pluginConf.getVersion(),
                pluginMetadata.getVersion()));
        }

        PluginConfigurationDto pluginConfDto = pluginConf.toDto();
        decryptSensibleParameter(pluginMetadata, pluginConfDto);

        LOGGER.info("New plugin instantiation for {}configuration {} of plugin {}",
                    dynamicParameters.length > 0 ? "dynamic " : "",
                    pluginConf.getBusinessId(),
                    pluginMetadata.getPluginId());

        return PluginUtils.getPlugin(pluginConfDto, pluginMetadata, tenantPluginCache, dynamicParameters);
    }

    /**
     * Retrieve Plugin type parameters from given plugin conf and instantiate the plugin if not already in cache.
     *
     * @param plgConf           {@link PluginConfiguration} to check and instantiate missing inner parameter plugins
     * @param pluginCacheTenant Cache of already instantiated plugin conf for the current tenant
     */
    @MultitenantTransactional
    public void instantiateInnerPlugins(PluginConfiguration plgConf,
                                        ConcurrentHashMap<String, Object> pluginCacheTenant) {
        Iterator<PluginConfiguration> it = getInnerPluginsConf(plgConf).descendingIterator();
        while (it.hasNext()) {
            PluginConfiguration innerConf = it.next();
            pluginCacheTenant.computeIfAbsent(innerConf.getBusinessId(), bid -> {
                try {
                    return instantiatePlugin(innerConf, pluginCacheTenant);
                } catch (ModuleException e) {
                    throw new RsRuntimeException(e);
                }
            });
        }
    }

    /**
     * Retrieve ordered list of {@link PluginConfiguration} matching all the given plugin inner plugin parameters.
     *
     * @param pluginConf {@link PluginConfiguration} to check
     * @return Ordered list of inner plugin configuration of the fiven plugin configuration
     */
    private LinkedList<PluginConfiguration> getInnerPluginsConf(PluginConfiguration pluginConf) {
        LinkedList<PluginConfiguration> innerConfList = new LinkedList<>();

        for (PluginParamDescriptor paramType : getPluginMetadata(pluginConf.getPluginId()).getParameters()) {
            if (paramType.getType() == PluginParamType.PLUGIN) {
                NestedPluginParam pluginParam = (NestedPluginParam) pluginConf.getParameter(paramType.getName());
                if ((pluginParam != null) && pluginParam.hasValue() && !pluginParam.getValue()
                                                                                   .equals(pluginConf.getBusinessId())) {
                    PluginConfiguration innerPluginConf = pluginDaoService.findCompleteByBusinessId(pluginParam.getValue());
                    // Add inner plugin to result list
                    innerConfList.add(innerPluginConf);
                    // Check if inner plugin contains other inner plugins and add them to result list
                    innerConfList.addAll(getInnerPluginsConf(innerPluginConf));
                }
            }
        }
        LOGGER.debug("Found {} inner plugin(s) for : {}", innerConfList.size(), pluginConf.getBusinessId());
        return innerConfList;
    }

    /**
     * Retrieve plugin {@link PluginMetaData}
     *
     * @throws PluginMetadataNotFoundRuntimeException if {@link PluginMetaData} not found.
     * @param pluginId Plugin identifier to load
     * @return PluginMetaData
     */
    private PluginMetaData getPluginMetadata(String pluginId) {
        PluginMetaData pluginMetadata = PluginUtils.getPluginMetadata(pluginId);
        if (pluginMetadata == null) {
            LOGGER.debug("No plugin metadata found for plugin configuration id {}", pluginId);
            logAvailablePluginMetadata();
            throw new PluginMetadataNotFoundRuntimeException("Metadata not found for plugin configuration identifier "
                                                             + pluginId);
        }
        return pluginMetadata;
    }

    /**
     * Print scanned plugin available
     */
    private void logAvailablePluginMetadata() {
        StringBuilder buf = new StringBuilder();
        for (Map.Entry<String, PluginMetaData> entry : PluginUtils.getPlugins().entrySet()) {

            // Interfaces
            Iterator<String> interfaceIt = entry.getValue().getInterfaceNames().iterator();
            buf.delete(0, buf.length());
            buf.append("[");
            if (interfaceIt.hasNext()) {
                buf.append(interfaceIt.next());
                while (interfaceIt.hasNext()) {
                    buf.append(",");
                    buf.append(interfaceIt.next());
                }
            }
            buf.append("]");

            LOGGER.debug("Available pluginMap metadata : {} -> {} / {} / {}",
                         entry.getKey(),
                         entry.getValue().getPluginId(),
                         entry.getValue().getPluginClassName(),
                         buf);
        }
    }

    /**
     * Set decrypted value for plugin instantiation.
     */
    private void decryptSensibleParameter(PluginMetaData pluginMetadata, PluginConfigurationDto conf)
        throws EncryptionException {
        for (PluginParamDescriptor paramType : pluginMetadata.getParameters()) {
            // only decrypt STRING plugin parameter for now.
            if (paramType.getType() == PluginParamType.STRING) {
                StringPluginParam pluginParam = (StringPluginParam) conf.getParameter(paramType.getName());
                if (Boolean.TRUE.equals((pluginParam != null) && paramType.isSensible()) && pluginParam.hasValue()) {
                    conf.getParameters().remove(pluginParam);
                    StringPluginParam decryptedParam = pluginParam.clone();
                    decryptedParam.setValue(encryptionService.decrypt(decryptedParam.getValue()));
                    conf.getParameters().add(decryptedParam);
                }
            }
        }
    }
}
