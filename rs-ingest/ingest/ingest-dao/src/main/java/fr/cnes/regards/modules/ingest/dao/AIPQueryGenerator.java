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

import fr.cnes.regards.framework.jpa.restriction.ValuesRestrictionMode;
import fr.cnes.regards.framework.jpa.utils.CustomPostgresDialect;
import fr.cnes.regards.modules.ingest.domain.dto.NativeSelectQuery;
import fr.cnes.regards.modules.ingest.dto.AIPState;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;

import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.Collection;

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
    public static NativeSelectQuery searchAipTagsUsingSQL(SearchAIPsParameters filters) {
        NativeSelectQuery query = new NativeSelectQuery("distinct jsonb_array_elements_text(tags)", FROM_AIP);
        // Do not handle pagination here. See CustomizedAIPEntityRepository for pagination
        return generatePredicates(query, filters);
    }

    /**
     * Return an SQL query that retrieve all storages used by a set of entities
     */
    public static NativeSelectQuery searchAipStoragesUsingSQL(SearchAIPsParameters filters) {
        NativeSelectQuery query = new NativeSelectQuery("distinct jsonb_array_elements_text(storages)", FROM_AIP);
        // Do not handle pagination here. See CustomizedAIPEntityRepository for pagination
        return generatePredicates(query, filters);
    }

    /**
     * Return an SQL query that retrieve all categories used by a set of entities
     */
    public static NativeSelectQuery searchAipCategoriesUsingSQL(SearchAIPsParameters filters) {
        NativeSelectQuery query = new NativeSelectQuery("distinct jsonb_array_elements_text(categories)", FROM_AIP);
        // Do not handle pagination here. See CustomizedAIPEntityRepository for pagination
        return generatePredicates(query, filters);
    }

    private static NativeSelectQuery generatePredicates(NativeSelectQuery query, SearchAIPsParameters filters) {
        if (filters.getAipStates() != null) {
            query.andListPredicate("(state in (",
                                   "))",
                                   "state",
                                   filters.getAipStates().getValues().stream().map(AIPState::name).toList());
        }
        if (filters.getLastUpdate() != null && filters.getLastUpdate().getAfter() != null) {
            Timestamp time = Timestamp.valueOf(filters.getLastUpdate()
                                                      .getAfter()
                                                      .atZoneSameInstant(ZoneOffset.UTC)
                                                      .toLocalDateTime());
            query.andPredicate("(last_update > :timeFrom)", "timeFrom", time);
        }
        if (filters.getLastUpdate() != null && filters.getLastUpdate().getBefore() != null) {
            Timestamp time = Timestamp.valueOf(filters.getLastUpdate()
                                                      .getBefore()
                                                      .atZoneSameInstant(ZoneOffset.UTC)
                                                      .toLocalDateTime());
            query.andPredicate("(last_update < :timeTo)", "timeTo", time);
        }
        if (filters.getSessionOwner() != null) {
            query.andPredicate("(session_owner = :sessionOwner)", "sessionOwner", filters.getSessionOwner());
        }
        if (filters.getSession() != null) {
            query.andPredicate("(session_name = :sessionName)", "sessionName", filters.getSession());
        }
        if (filters.getAipIds() != null) {
            if (filters.getAipIds().getMode() == ValuesRestrictionMode.INCLUDE) {
                query.andListPredicate("(aip_id in (", "))", "aipId", filters.getAipIds().getValues());
            } else {
                query.andListPredicate("(aip_id not in (", "))", "aipIdExcluded", filters.getAipIds().getValues());
            }
        }
        if (filters.getProviderIds() != null) {
            query.addOneOfString("provider_id", filters.getProviderIds());
        }

        if (filters.getTags() != null) {
            query = getDisjunctionPredicate("tags", query, filters.getTags().getValues());
        }
        if (filters.getCategories() != null) {
            query = getDisjunctionPredicate("categories", query, filters.getCategories().getValues());
        }
        if (filters.getStorages() != null) {
            query = getDisjunctionPredicate("storages", query, filters.getStorages().getValues());
        }
        return query;
    }

    private static NativeSelectQuery getDisjunctionPredicate(String propertyName,
                                                             NativeSelectQuery query,
                                                             Collection<String> tags) {
        query.andListPredicate("(" + CustomPostgresDialect.JSONB_EXISTS_ANY + "(" + propertyName + ", array[",
                               "]))",
                               propertyName,
                               tags);
        return query;
    }
}
