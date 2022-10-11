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
package fr.cnes.regards.modules.ltamanager.service.settings;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.model.client.IModelClient;
import fr.cnes.regards.modules.model.domain.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Cache service that contains model names requested.
 *
 * @author Iliana Ghazali
 **/
@Service
@MultitenantTransactional
public class LtaModelCacheService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LtaModelCacheService.class);

    private static final int MODEL_NAME_EXPIRATION_IN_MIN = 60;

    private final IModelClient modelClient;

    public final long expireInCacheDurationInMin;

    private final Cache<String, Long> cache;

    public LtaModelCacheService(IModelClient modelClient,
                                @Value("${regards.ltamanager.cache.model.expires_minutes:"
                                       + MODEL_NAME_EXPIRATION_IN_MIN
                                       + "}") long expireInCacheDurationInMin) {
        this.modelClient = modelClient;
        this.expireInCacheDurationInMin = expireInCacheDurationInMin;
        cache = initCache();
    }

    private Cache<String, Long> initCache() {
        return CacheBuilder.newBuilder()
                           .expireAfterWrite(Duration.of(expireInCacheDurationInMin, ChronoUnit.MINUTES))
                           .build();
    }

    /**
     * Check if a model exists in cache. If not, retrieve it in the repository and add it to the cache if it was found.
     *
     * @param modelName modelName to retrieve
     * @return if model was found
     */
    public boolean modelExists(String modelName) {
        // check if model name exists in cache
        boolean isModelExists = this.getCacheEntry(modelName) != null;
        // if no model was found, check if it exists in the repository
        if (!isModelExists) {
            LOGGER.trace("\"{}\" was not found in cache. Searching it in modelRepository...", modelName);
            Long idModelRetrieved = retrieveModelId(modelName);
            if (idModelRetrieved != null) {
                LOGGER.trace("\"{}\" found in repository, adding the value to the cache.", modelName);
                this.addCacheEntry(modelName, idModelRetrieved);
                isModelExists = true;
            } else {
                LOGGER.error("\"{}\" was not found in cache or in the modelRepository! A datatype should be linked to "
                             + "an existing model.", modelName);
            }
        }
        return isModelExists;
    }

    private Long retrieveModelId(String modelName) {
        Long idModelRetrieved = null;
        try {
            FeignSecurityManager.asSystem();
            ResponseEntity<EntityModel<Model>> modelResponse = modelClient.getModel(modelName);
            if (modelResponse != null && modelResponse.getStatusCode().is2xxSuccessful()) {
                EntityModel<Model> responseBody = modelResponse.getBody();
                if (responseBody != null && responseBody.getContent() != null) {
                    idModelRetrieved = Objects.requireNonNull(responseBody.getContent()).getId();
                }
            }
            return idModelRetrieved;
        } finally {
            FeignSecurityManager.reset();
        }
    }

    public void addCacheEntry(String modelName, Long idModelFound) {
        cache.put(modelName, idModelFound);
    }

    public Long getCacheEntry(String modelName) {
        return cache.getIfPresent(modelName);
    }

}
