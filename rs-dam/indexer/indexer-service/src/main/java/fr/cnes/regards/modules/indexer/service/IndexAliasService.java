/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */

package fr.cnes.regards.modules.indexer.service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.indexer.dao.IEsIndexAliasRepository;
import fr.cnes.regards.modules.indexer.domain.EsIndexAlias;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Service responsible for managing catalog index alias
 * Also handles caching to avoid frequent database access in order to get catalog index alias.
 */
@Service
public class IndexAliasService {

    private final IEsIndexAliasRepository repository;

    public IndexAliasService(IEsIndexAliasRepository repository) {
        this.repository = repository;
    }

    /**
     * Retrieves an esIndexAlias by its name, using a cache to avoid frequent DB access
     */
    @Cacheable(cacheNames = "esIndexAliases", key = "#aliasName")
    @MultitenantTransactional(readOnly = true)
    public EsIndexAlias getByAlias(String aliasName) {
        return repository.findByAlias(aliasName).orElse(null);
    }

    @CachePut(cacheNames = "esIndexAliases", key = "#aliasName")
    @MultitenantTransactional
    public EsIndexAlias saveOrUpdate(String aliasName, String currentIndex) {
        return repository.findByAlias(aliasName).map(existing -> {
            if (!currentIndex.equals(existing.getCurrent())) {
                existing.setCurrent(currentIndex);
                return repository.save(existing);
            }
            return existing;
        }).orElseGet(() -> repository.save(new EsIndexAlias(aliasName, currentIndex)));
    }

    /**
     * Evicts the cache entry for a given alias
     */
    @CacheEvict(cacheNames = "esIndexAliases", key = "#aliasName")
    public void evict(String aliasName) {
        // Called when alias is removed or refresh is needed
    }

    /**
     * Sets the building index for the given alias (or clears it if buildingIndex is null).
     * <p>
     * If the alias does not exist, an IllegalStateException is thrown.
     */
    @CachePut(cacheNames = "esIndexAliases", key = "#aliasName")
    @MultitenantTransactional
    public EsIndexAlias setBuilding(String aliasName, String buildingIndex) {
        return repository.findByAlias(aliasName).map(existing -> {
            existing.setBuilding(buildingIndex);
            return repository.save(existing);
        }).orElseThrow(() -> new IllegalStateException("Alias '" + aliasName + "' does not exist"));

    }

    /**
     * Clears the building index for the given alias, if present.
     */
    @CachePut(cacheNames = "esIndexAliases", key = "#aliasName")
    @MultitenantTransactional
    public EsIndexAlias clearBuilding(String aliasName) {
        return setBuilding(aliasName, null);
    }

}
