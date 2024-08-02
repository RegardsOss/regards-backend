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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to handle cache of instantiated {@link Plugin}s by tenant.
 */
@Service
public class PluginCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginCache.class);

    private final PluginInstantiationService pluginInstanceService;

    private final PluginConfigurationService pluginDaoService;

    /**
     * {@link IRuntimeTenantResolver}
     */
    private final IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Map<Tenant, Cache(PluginBusinessId, Plugin)).
     * A {@link Map} with all the {@link Plugin} currently instantiated by tenant.
     * One and only one {@link Plugin} should be instantiated by tenant.
     * <b>Note: </b> PluginService is used in multi-thread environment
     */
    private final Cache<String, ConcurrentHashMap<String, Object>> pluginCacheByTenant = Caffeine.newBuilder()
                                                                                                 .maximumSize(10000)
                                                                                                 .build();

    public PluginCache(PluginInstantiationService pluginInstanceService, PluginConfigurationService pluginDaoService,

                       IRuntimeTenantResolver runtimeTenantResolver) {
        this.pluginInstanceService = pluginInstanceService;
        this.pluginDaoService = pluginDaoService;
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    public ConcurrentHashMap<String, Object> getTenantCache(String tenant) {
        return pluginCacheByTenant.get(tenant, t -> new ConcurrentHashMap<>());
    }

    /**
     * Retrieve a plugin for a given tenant. If not present, add the new instantiated plugin in tenant cache map.
     *
     * @param tenant  current project
     * @param plgConf plugin conf to instantiate
     * @return new tenant plugin cache map
     */
    public Map<String, Object> getPluginForTenant(String tenant, PluginConfiguration plgConf) {
        ConcurrentHashMap<String, Object> pluginTenantCache = getTenantCache(tenant);
        pluginInstanceService.instantiateInnerPlugins(plgConf, pluginTenantCache);
        pluginTenantCache.computeIfAbsent(plgConf.getBusinessId(), bid -> {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                return pluginInstanceService.instantiatePlugin(pluginDaoService.findCompleteByBusinessId(plgConf.getBusinessId()),
                                                               pluginTenantCache);
            } catch (ModuleException e) {
                throw new RsRuntimeException(e);
            }
        });
        return pluginTenantCache;
    }

    /**
     * Clean recursively from the cache the given plugin and its dependencies.
     */
    public void cleanPluginRecursively(String tenant, String businessId) {
        runtimeTenantResolver.forceTenant(tenant);
        // get all dependent plugins to destroy
        Set<PluginConfiguration> parentPluginConfs = pluginDaoService.getDependentPlugins(businessId);
        parentPluginConfs.forEach(parent -> cleanPluginRecursively(tenant, parent.getBusinessId()));
        // destroy plugin instance from cache
        getTenantCache(tenant).computeIfPresent(businessId, (bid, instantiatedPlugin) -> {
            LOGGER.debug("Cleaning plugin from tenant cache '{}' with business id '{}'.", tenant, bid);
            PluginUtils.doDestroyPlugin(instantiatedPlugin);
            return null;
        });
    }

    public void cleanTenant(String tenant) {
        LOGGER.debug("Cleaning plugin tenant cache '{}'.", tenant);
        getTenantCache(tenant).forEach((bid, instantiatedPlugin) -> {
            LOGGER.debug("Cleaning plugin from tenant cache '{}' with business id '{}'.", tenant, bid);
            PluginUtils.doDestroyPlugin(instantiatedPlugin);
        });
        pluginCacheByTenant.invalidate(tenant);
    }

}
