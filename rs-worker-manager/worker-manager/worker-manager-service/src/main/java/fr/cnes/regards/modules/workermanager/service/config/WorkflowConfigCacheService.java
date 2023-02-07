/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.workermanager.service.config;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.workermanager.dao.IWorkflowRepository;
import fr.cnes.regards.modules.workermanager.domain.config.WorkflowConfig;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cache for {@link WorkflowConfig}s. Workflow cache is used to avoid useless database request as workflow configs
 * rarely change!
 *
 * @author Iliana Ghazali
 **/
@Service
public class WorkflowConfigCacheService {

    private final IWorkflowRepository workflowConfigRepository;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final LoadingCache<String, Map<String, WorkflowConfig>> workflowConfigsCachePerTenant = CacheBuilder.newBuilder()
                                                                                                                .build(
                                                                                                                    new WorkflowCacheLoader());

    public WorkflowConfigCacheService(IWorkflowRepository workflowConfigRepository,
                                      IRuntimeTenantResolver runtimeTenantResolver) {
        this.workflowConfigRepository = workflowConfigRepository;
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    /**
     * Get the workflow as an optional
     */
    public Optional<WorkflowConfig> getWorkflowConfig(String workflowType) {
        return Optional.ofNullable(getWorkflowConfigs().get(workflowType));
    }

    /**
     * Clean all values on the cache relative to current tenant
     */
    public void cleanCache() {
        String tenant = runtimeTenantResolver.getTenant();
        workflowConfigsCachePerTenant.invalidate(tenant);
    }

    /**
     * Get the Map<Workflow type, Workflow> for the current tenant
     * If the cache is empty, it inits data from DB.
     *
     * @return all enabled {@link WorkflowConfig}
     */
    public Map<String, WorkflowConfig> getWorkflowConfigs() {
        String tenant = runtimeTenantResolver.getTenant();
        return workflowConfigsCachePerTenant.getUnchecked(tenant);
    }

    /**
     * Cache {@link WorkflowConfig}s in a map of Map<Workflow type, WorkflowConfig>
     */
    class WorkflowCacheLoader extends CacheLoader<String, Map<String, WorkflowConfig>> {

        @Override
        @Nonnull
        public Map<String, WorkflowConfig> load(@Nonnull String tenant) {
            return workflowConfigRepository.findAll()
                                           .stream()
                                           .collect(Collectors.toMap(WorkflowConfig::getWorkflowType,
                                                                     Function.identity()));
        }

    }

}
