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
package fr.cnes.regards.modules.workermanager.service.config;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.workermanager.dao.IWorkerConfigRepository;
import fr.cnes.regards.modules.workermanager.domain.config.WorkerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A cache holding values of {@link WorkerConfig}.
 * To improve performances, the cache is structured to be access by tenant then by content type, as this:
 * <p>tenant  -></p>
 * <p>       content type -></p>
 * <p>                    worker type</p>
 *
 * @author Léo Mieulet
 */
@Service
public class WorkerConfigCacheService {

    @Autowired
    private IWorkerConfigRepository workerConfigRepository;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * WorkerConfig cache is used to avoid useless database request as worker config rarely change!<br/>
     */
    private final LoadingCache<String, Map<String, String>> workerConfigsCachePerTenant = CacheBuilder.newBuilder()
                                                                                                      .build(new WorkerCacheLoader());

    /**
     * Get the worker type as an optional
     */
    public Optional<String> getWorkerType(String contentType) {
        Map<String, String> workerConfigs = getWorkerConfigs();
        if (workerConfigs.containsKey(contentType)) {
            return Optional.of(workerConfigs.get(contentType));
        }
        return Optional.empty();
    }

    /**
     * Clean all values on the cache relative to current tenant
     */
    public void cleanCache() {
        String tenant = runtimeTenantResolver.getTenant();
        workerConfigsCachePerTenant.invalidate(tenant);
    }

    /**
     * Get the Map<Content type, Worker type> for the current tenant
     * If the cache is empty, it inits these data from DB
     *
     * @return all enabled {@link WorkerConfig}
     */
    public Map<String, String> getWorkerConfigs() {
        String tenant = runtimeTenantResolver.getTenant();
        return workerConfigsCachePerTenant.getUnchecked(tenant);
    }

    /**
     * Cache {@link WorkerConfig}s in a map of Map<Content type, Worker type>
     */
    class WorkerCacheLoader extends CacheLoader<String, Map<String, String>> {

        @Override
        @Nonnull
        public Map<String, String> load(@Nonnull String tenant) {
            List<WorkerConfig> workerConfigs = workerConfigRepository.findAll();
            // Reorganise worker configs into a Map with content type as key and worker type as value
            Map<String, String> workerTypeByContentType = new HashMap<>();
            for (WorkerConfig workerConfig : workerConfigs) {
                for (String contentType : workerConfig.getContentTypeInputs()) {
                    workerTypeByContentType.put(contentType, workerConfig.getWorkerType());
                }
            }
            return workerTypeByContentType;
        }
    }
}
