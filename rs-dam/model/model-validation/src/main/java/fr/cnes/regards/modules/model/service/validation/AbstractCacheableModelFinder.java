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
package fr.cnes.regards.modules.model.service.validation;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.model.domain.ModelAttrAssoc;
import fr.cnes.regards.modules.model.dto.event.ModelChangeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.validation.Errors;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Cache proxy to handle model attributes
 *
 * @author Marc SORDI
 */
public abstract class AbstractCacheableModelFinder
    implements IModelFinder, ApplicationListener<ApplicationReadyEvent>, IBatchHandler<ModelChangeEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCacheableModelFinder.class);

    /**
     * Model cache is used to avoid useless database request as models rarely change!<br/>
     * tenant key -> model key / attributes val
     */
    private final Map<String, LoadingCache<String, Optional<List<ModelAttrAssoc>>>> modelCacheMap = new ConcurrentHashMap<>();

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(ModelChangeEvent.class, this);
    }

    @Override
    public List<ModelAttrAssoc> findByModel(String model) {
        String tenant = runtimeTenantResolver.getTenant();
        try {
            return getTenantCache(tenant).get(model).orElse(null);
        } catch (ExecutionException e) {
            LOGGER.error("Error during cache initialisation", e);
            return null;
        }
    }

    private LoadingCache<String, java.util.Optional<List<ModelAttrAssoc>>> getTenantCache(String tenant) {
        return modelCacheMap.computeIfAbsent(tenant,
                                             t -> CacheBuilder.newBuilder()
                                                              .expireAfterWrite(60, TimeUnit.MINUTES)
                                                              .build(new CacheLoader<>() {

                                                                  @Override
                                                                  public Optional<List<ModelAttrAssoc>> load(String modelName) {
                                                                      List<ModelAttrAssoc> attributesByModel = loadAttributesByModel(
                                                                          modelName);
                                                                      return Optional.ofNullable(attributesByModel);
                                                                  }
                                                              }));
    }

    private void cleanTenantCache(String tenant, String model) {
        LoadingCache<String, Optional<List<ModelAttrAssoc>>> modelCache = modelCacheMap.get(tenant);
        if (modelCache != null) {
            modelCache.invalidate(model);
        }
    }

    protected abstract List<ModelAttrAssoc> loadAttributesByModel(String modelName);

    @Override
    public Errors validate(ModelChangeEvent message) {
        return null;
    }

    @Override
    public void handleBatch(List<ModelChangeEvent> messages) {
        String tenant = runtimeTenantResolver.getTenant();
        messages.stream().map(ModelChangeEvent::getModel).distinct().forEach(model -> {
            LOGGER.info("Change detected for model \"{}\" of tenant \"{}\"", model, tenant);
            cleanTenantCache(tenant, model);
        });
    }

    @Override
    public boolean isDedicatedDLQEnabled() {
        return false;
    }
}
