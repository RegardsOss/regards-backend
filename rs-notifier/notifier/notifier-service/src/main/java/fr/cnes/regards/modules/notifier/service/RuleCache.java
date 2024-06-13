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
package fr.cnes.regards.modules.notifier.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import fr.cnes.regards.framework.amqp.event.notifier.NotificationRequestEvent;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.notifier.dao.IRuleRepository;
import fr.cnes.regards.modules.notifier.domain.Rule;
import fr.cnes.regards.modules.notifier.domain.plugin.IRuleMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Cache for available {@link Rule} by tenant
 * As a rule contains an instance of {@link IRuleMatcher} plugin interface configuration, this cache lets you access
 * the list of available rules that needs to be tested against incoming {@link NotificationRequestEvent}
 *
 * @author Kevin Marchois
 * @author Léo Mieulet
 */
@Component
public class RuleCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuleCache.class);

    private final IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Store the list of existing {@link Rule} by tenant
     * It is used to avoid useless database request as {@link Rule} rarely change!<br/>
     */
    private final LoadingCache<String, Set<Rule>> ruleCachePerTenant;

    public RuleCache(IRuntimeTenantResolver runtimeTenantResolver,
                     IRuleRepository ruleRepo) {
        this.runtimeTenantResolver = runtimeTenantResolver;
        ruleCachePerTenant = CacheBuilder.newBuilder().build(new CacheLoader<>() {

            @Override
            public Set<Rule> load(String tenant) {
                return ruleRepo.findByRulePluginActiveTrue();
            }
        });
    }

    /**
     * Get all enabled {@link Rule} for the current tenant
     * When the cache is empty, load existing rules from database
     *
     * @return all enabled {@link Rule}
     * @throws ExecutionException if access to repository raises a checked exception
     */
    public Set<Rule> getRules() throws ExecutionException {
        String tenant = runtimeTenantResolver.getTenant();
        return ruleCachePerTenant.get(tenant);
    }

    /**
     * Clean all {@link Rule} in cache for current tenant
     */
    public void clear() {
        String tenant = runtimeTenantResolver.getTenant();
        LOGGER.info("Clear rule cache of tenant {}", tenant);
        ruleCachePerTenant.invalidate(tenant);
    }
}
