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
import fr.cnes.regards.framework.jpa.utils.SpecificationUtils;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.domain.dto.NativeSelectQuery;
import fr.cnes.regards.modules.ingest.dto.aip.SearchFacetsAIPsParameters;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

/**
 * Query generator to build SQL queries to run against OAISEntity repository on {@link fr.cnes.regards.modules.ingest.dto.aip.AIP} entities
 * @author Léo Mieulet
 */
public class AIPQueryGenerator {

    private AIPQueryGenerator() {
    }

    /**
     * Return an SQL query that retrieve all tags used by a set of entities
     * @return
     */
    public static NativeSelectQuery searchAipTagsUsingSQL(SearchFacetsAIPsParameters filters) {
        NativeSelectQuery query = new NativeSelectQuery("distinct jsonb_array_elements_text(tags)",
                "{h-schema}t_aip ");

        query = generatePredicates(query, filters.getState(), filters.getFrom(), filters.getTo(), filters.getSessionOwner(),
                filters.getSession(), filters.getProviderId(), filters.getAipIds(), filters.getAipIdsExcluded(),
                filters.getTags(), filters.getCategories(), filters.getStorages());

        // Do not handle pagination here. See CustomizedAIPEntityRepository for pagination
        return query;
    }

    /**
     * Return an SQL query that retrieve all storages used by a set of entities
     * @return
     */
    public static NativeSelectQuery searchAipStoragesUsingSQL(SearchFacetsAIPsParameters filters) {
        NativeSelectQuery query = new NativeSelectQuery("distinct jsonb_array_elements(storages)->>'pluginBusinessId'",
                "{h-schema}t_aip ");

        query = generatePredicates(query, filters.getState(), filters.getFrom(), filters.getTo(), filters.getSessionOwner(),
                filters.getSession(), filters.getProviderId(), filters.getAipIds(), filters.getAipIdsExcluded(),
                filters.getTags(), filters.getCategories(), filters.getStorages());

        // Do not handle pagination here. See CustomizedAIPEntityRepository for pagination
        return query;
    }

    /**
     * Return an SQL query that retrieve all categories used by a set of entities
     * @return
     */
    public static NativeSelectQuery searchAipCategoriesUsingSQL(SearchFacetsAIPsParameters filters) {
        NativeSelectQuery query = new NativeSelectQuery("distinct jsonb_array_elements_text(categories)",
                "{h-schema}t_aip ");

        query = generatePredicates(query, filters.getState(), filters.getFrom(), filters.getTo(), filters.getSessionOwner(),
                filters.getSession(), filters.getProviderId(), filters.getAipIds(), filters.getAipIdsExcluded(),
                filters.getTags(), filters.getCategories(), filters.getStorages());

        // Do not handle pagination here. See CustomizedAIPEntityRepository for pagination
        return query;
    }

    private static NativeSelectQuery generatePredicates(NativeSelectQuery query, AIPState state, OffsetDateTime from, OffsetDateTime to,
            String sessionOwner, String session, String providerId, Set<String> aipIds, Set<String> aipIdsExcluded,
            List<String> tags, Set<String> categories, Set<String> storages) {
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
        if (aipIds != null && !aipIds.isEmpty()) {
            query.andListPredicate("(aip_id in (", "))", "aipId", aipIds);
        }
        if (aipIdsExcluded != null && !aipIdsExcluded.isEmpty()) {
            query.andListPredicate("(aip_id not in (", "))", "aipIdExcluded", aipIdsExcluded);
        }
        if (providerId != null && !providerId.isEmpty()) {
            if (providerId.startsWith(SpecificationUtils.LIKE_CHAR) || providerId.endsWith(SpecificationUtils.LIKE_CHAR)) {
                query.andPredicate("(provider_id like :providerId)", "providerId", providerId);
            } else {
                query.andPredicate("(provider_id = :providerId)", "providerId", providerId);
            }
        }

        if (tags != null && !tags.isEmpty()) {
            query = getConjunctionPredicate("tags", query, Sets.newHashSet(tags));
        }
        if (categories != null && !categories.isEmpty()) {
            query = getConjunctionPredicate("categories", query, categories);
        }
        if (storages != null && !storages.isEmpty()) {
            query.addOneOf("(storages @> jsonb_build_array(json_build_object('pluginBusinessId',", ")))", "storages", storages);
        }
        return query;
    }

    private static NativeSelectQuery getConjunctionPredicate(String propertyName, NativeSelectQuery query, Set<String> tags) {
        query.andListPredicate("(" + propertyName + " @> jsonb_build_array(", "))", propertyName, tags);
        return query;
    }
}
