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

/**
 *
 */
package fr.cnes.regards.modules.indexer.service;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.indexer.domain.EsIndexAlias;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static fr.cnes.regards.modules.indexer.dao.EsRepository.ALIAS_SUFFIX;

/**
 * Resolves the actual Elasticsearch index name used for search and indexing,
 * based on the tenant and alias configuration stored in the database.
 */
@Component
public class IndexAliasResolver {

    private final IRuntimeTenantResolver tenantResolver;

    private final IndexAliasService indexAliasService;

    public IndexAliasResolver(IRuntimeTenantResolver tenantResolver, IndexAliasService indexAliasService) {
        this.tenantResolver = tenantResolver;
        this.indexAliasService = indexAliasService;
    }

    /**
     * Builds the alias name from the current tenant
     */
    public String resolveAliasName() {
        return resolveAliasName(tenantResolver.getTenant());
    }

    /**
     * Builds the alias name from a given tenant (e.g. "tenant" -> "tenant_alias")
     * The suffix comes from the constant ALIAS_SUFFIX in {@link fr.cnes.regards.modules.indexer.dao.EsRepository}
     */
    public static String resolveAliasName(String tenant) {
        return tenant + ALIAS_SUFFIX;
    }

    /**
     * Resolves the current Elasticsearch index name for the given tenant
     */
    public String resolveCurrentIndex(String tenant) {
        String alias = resolveAliasName(tenant);
        EsIndexAlias aliasEntry = indexAliasService.getByAlias(alias);
        if (aliasEntry == null) {
            throw new IllegalStateException("No alias configured for tenant: " + tenant);
        }
        return aliasEntry.getCurrent();
    }

    /**
     * Returns the name of the index being built, if any
     */
    public Optional<String> resolveBuildingIndex(String tenant) {
        String alias = resolveAliasName(tenant);
        EsIndexAlias aliasEntry = indexAliasService.getByAlias(alias);
        return Optional.ofNullable(aliasEntry).map(EsIndexAlias::getBuilding);
    }

}
