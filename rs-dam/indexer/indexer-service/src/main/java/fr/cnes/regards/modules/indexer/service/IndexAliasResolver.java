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

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.indexer.domain.EsIndexAlias;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static fr.cnes.regards.modules.indexer.dao.EsRepository.ALIAS_SUFFIX;

/**
 * Resolves the actual Elasticsearch index name used for search and indexing,
 * based on the tenant and alias configuration stored in the database.
 */
@Component
public class IndexAliasResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexAliasResolver.class);

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

    /**
     * Increments an index name by appending a short hash of the tenant and appending or increasing a numeric suffix.
     * Example1: if current index is {@code tenantA}, the next will be {@code tenantA_ab12cd_2}.
     * Example2: if current index is {@code tenantA_ab12cd_1}, the next will be {@code tenantA_ab12cd_2}.
     */
    public String resolveNextIndexName(String tenant) {
        String indexName = resolveBuildingIndex(tenant).orElse(resolveCurrentIndex(tenant));
        if (Strings.isBlank(indexName)) {
            throw new IllegalStateException("Index name cannot be null or empty for tenant: " + tenant);
        }
        Optional<ParsedBuildingIndex> parsed = parseIndex(indexName);
        if (parsed.isPresent()) {
            ParsedBuildingIndex p = parsed.get();
            return p.tenant() + "_" + p.hash() + "_" + (p.number() + 1);
        }
        // First case, we initialize it
        return buildIndexName(tenant, shortHash(tenant), 1);
    }

    /**
     * Try to interpret indexName as: tenant + "_" + hash + "_" + number
     */
    private Optional<ParsedBuildingIndex> parseIndex(String indexName) {
        // Find the last underscore, separates number part
        int lastUnderscore = indexName.lastIndexOf('_');
        if (lastUnderscore < 0 || lastUnderscore == indexName.length() - 1) {
            return Optional.empty();
        }
        String numPart = indexName.substring(lastUnderscore + 1);

        // Find the second-to-last underscore, separates hash part and tenant part
        int secondLastUnderscore = indexName.lastIndexOf('_', lastUnderscore - 1);
        if (secondLastUnderscore < 0 || secondLastUnderscore == lastUnderscore - 1) {
            return Optional.empty();
        }
        String hashPart = indexName.substring(secondLastUnderscore + 1, lastUnderscore);
        String tenant = indexName.substring(0, secondLastUnderscore);

        // Check 1: numPart must be a valid positive integer
        int number;
        try {
            if (numPart.isEmpty()) {
                return Optional.empty();
            }
            number = Integer.parseInt(numPart);
            if (number < 1) {
                return Optional.empty();
            }
        } catch (NumberFormatException e) {
            return Optional.empty();
        }

        // Check 2: hashPart must match the first 6 hex chars
        if (hashPart.length() != 6) {
            return Optional.empty();
        }
        String expected = shortHash(tenant);
        if (!expected.equalsIgnoreCase(hashPart)) {
            return Optional.empty();
        }

        return Optional.of(new ParsedBuildingIndex(tenant, hashPart.toLowerCase(), number));
    }

    /**
     * Compute hash of the input and return the first 6 hex chars
     */
    public static String shortHash(String tenant) {
        return String.format("%06x", tenant.hashCode() & 0xFFFFFF);
    }

    private record ParsedBuildingIndex(String tenant,
                                       String hash,
                                       int number) {

    }

    public static String buildIndexName(String tenant, String hash, int i) {
        return tenant + "_" + hash + "_" + i;
    }
}
