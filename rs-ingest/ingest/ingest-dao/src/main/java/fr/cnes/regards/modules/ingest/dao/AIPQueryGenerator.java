/*
 * Copyright 2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.dao;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.utils.CustomPostgresDialect;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.domain.dto.NativeSelectQuery;
import fr.cnes.regards.modules.ingest.dto.aip.SearchFacetsAIPsParameters;
import fr.cnes.regards.modules.ingest.dto.request.SearchSelectionMode;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

/**
 * Query generator to build SQL queries to run against OAISEntity repository on {@link fr.cnes.regards.modules.ingest.dto.aip.AIP} entities
 *
 * @author Léo Mieulet
 */
public class AIPQueryGenerator {

    public static final String FROM_AIP = "{h-schema}t_aip ";

    private AIPQueryGenerator() {
    }

    /**
     * Return an SQL query that retrieve all tags used by a set of entities
     */
    public static NativeSelectQuery searchAipTagsUsingSQL(SearchFacetsAIPsParameters filters) {
        NativeSelectQuery query = new NativeSelectQuery("distinct jsonb_array_elements_text(tags)", FROM_AIP);

        query = generatePredicates(query,
                                   filters.getState(),
                                   filters.getLastUpdate().getFrom(),
                                   filters.getLastUpdate().getTo(),
                                   filters.getSessionOwner(),
                                   filters.getSession(),
                                   filters.getProviderIds(),
                                   filters.getAipIds(),
                                   filters.getSelectionMode() == SearchSelectionMode.INCLUDE,
                                   filters.getTags(),
                                   filters.getCategories(),
                                   filters.getStorages());

        // Do not handle pagination here. See CustomizedAIPEntityRepository for pagination
        return query;
    }

    /**
     * Return an SQL query that retrieve all storages used by a set of entities
     */
    public static NativeSelectQuery searchAipStoragesUsingSQL(SearchFacetsAIPsParameters filters) {
        NativeSelectQuery query = new NativeSelectQuery("distinct jsonb_array_elements_text(storages)", FROM_AIP);

        query = generatePredicates(query,
                                   filters.getState(),
                                   filters.getLastUpdate().getFrom(),
                                   filters.getLastUpdate().getTo(),
                                   filters.getSessionOwner(),
                                   filters.getSession(),
                                   filters.getProviderIds(),
                                   filters.getAipIds(),
                                   filters.getSelectionMode() == SearchSelectionMode.INCLUDE,
                                   filters.getTags(),
                                   filters.getCategories(),
                                   filters.getStorages());

        // Do not handle pagination here. See CustomizedAIPEntityRepository for pagination
        return query;
    }

    /**
     * Return an SQL query that retrieve all categories used by a set of entities
     */
    public static NativeSelectQuery searchAipCategoriesUsingSQL(SearchFacetsAIPsParameters filters) {
        NativeSelectQuery query = new NativeSelectQuery("distinct jsonb_array_elements_text(categories)", FROM_AIP);

        query = generatePredicates(query,
                                   filters.getState(),
                                   filters.getLastUpdate().getFrom(),
                                   filters.getLastUpdate().getTo(),
                                   filters.getSessionOwner(),
                                   filters.getSession(),
                                   filters.getProviderIds(),
                                   filters.getAipIds(),
                                   filters.getSelectionMode() == SearchSelectionMode.INCLUDE,
                                   filters.getTags(),
                                   filters.getCategories(),
                                   filters.getStorages());

        // Do not handle pagination here. See CustomizedAIPEntityRepository for pagination
        return query;
    }

    private static NativeSelectQuery generatePredicates(NativeSelectQuery query,
                                                        AIPState state,
                                                        OffsetDateTime from,
                                                        OffsetDateTime to,
                                                        String sessionOwner,
                                                        String session,
                                                        Set<String> providerIds,
                                                        List<String> aipIds,
                                                        boolean areAipIdsInclude,
                                                        List<String> tags,
                                                        Set<String> categories,
                                                        Set<String> storages) {
        if (state != null) {
            query.andPredicate("(state = :state)", "state", state.toString());
        }
        if (from != null) {
            Timestamp time = Timestamp.valueOf(from.minusNanos(1).atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());
            query.andPredicate("(last_update > :timeFrom)", "timeFrom", time);
        }
        if (to != null) {
            Timestamp time = Timestamp.valueOf(to.plusSeconds(1).atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());
            query.andPredicate("(last_update < :timeTo)", "timeTo", time);
        }
        if (sessionOwner != null) {
            query.andPredicate("(session_owner = :sessionOwner)", "sessionOwner", sessionOwner);
        }
        if (session != null) {
            query.andPredicate("(session_name = :sessionName)", "sessionName", session);
        }
        if ((aipIds != null) && !aipIds.isEmpty()) {
            if (areAipIdsInclude) {
                query.andListPredicate("(aip_id in (", "))", "aipId", aipIds);
            } else {
                query.andListPredicate("(aip_id not in (", "))", "aipIdExcluded", aipIds);
            }
        }
        if ((providerIds != null) && !providerIds.isEmpty()) {
            query.addOneOfStringLike("provider_id", providerIds);
        }

        if ((tags != null) && !tags.isEmpty()) {
            query = getDisjunctionPredicate("tags", query, Sets.newHashSet(tags));
        }
        if ((categories != null) && !categories.isEmpty()) {
            query = getDisjunctionPredicate("categories", query, categories);
        }
        if ((storages != null) && !storages.isEmpty()) {
            query = getDisjunctionPredicate("storages", query, storages);
        }
        return query;
    }

    private static NativeSelectQuery getDisjunctionPredicate(String propertyName,
                                                             NativeSelectQuery query,
                                                             Set<String> tags) {
        query.andListPredicate("(" + CustomPostgresDialect.JSONB_EXISTS_ANY + "(" + propertyName + ", array[",
                               "]))",
                               propertyName,
                               tags);
        return query;
    }
}
