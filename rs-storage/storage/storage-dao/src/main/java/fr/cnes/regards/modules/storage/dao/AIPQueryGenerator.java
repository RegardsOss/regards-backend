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
package fr.cnes.regards.modules.storage.dao;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;

/**
 * Specification class to filter DAO searches on {@link AIP} entities
 * Note there is an index on json_aip.properties.pdi.contextInformation.tags,
 * so you must use the same accessor defined in the index V3.0.5__storage_replace_tags_column_fix.sql
 * (#>'{properties,pdi,contextInformation,tags})
 * @author Léo Mieulet
 */
public class AIPQueryGenerator {

    private AIPQueryGenerator() {
    }

    /**
     * Return an SQL query that retrieve all entities matching provided criteria
     * @param tags must be present in the AIP
     */
    public static String searchAIPContainingAllTags(AIPState state, OffsetDateTime from, OffsetDateTime to,
            List<String> tags, String session, String providerId, Set<String> aipIds, Set<String> aipIdsExcluded) {
        Set<String> predicates = generatePredicates(state, from, to, session, providerId, aipIds, aipIdsExcluded);
        if (tags != null && !tags.isEmpty()) {
            predicates.add(getConjunctionTagPredicate(tags));
        }
        return createQuery(predicates);
    }

    /**
     * Return an SQL query that retrieve all AIPEntity ids matching provided criteria
     * @param tags must be present in the AIP
     */
    public static String searchAIPIdContainingAllTags(AIPState state, OffsetDateTime from, OffsetDateTime to, List<String> tags,
            String session, String providerId, Set<String> aipIds, Set<String> aipIdsExcluded) {
        Set<String> predicates = generatePredicates(state, from, to, session, providerId, aipIds, aipIdsExcluded);
        if ((tags != null) && !tags.isEmpty()) {
            predicates.add(getConjunctionTagPredicate(tags));
        }
        return createIdQuery(predicates);
    }

    /**
     * Return an SQL query that retrieve all entities matching provided criteria
     * @param tags At least one provided tag will be present in the AIP
     */
    public static String searchAIPContainingAtLeastOneTag(AIPState state, OffsetDateTime from, OffsetDateTime to,
            List<String> tags, String session, String providerId, Set<String> aipIds, Set<String> aipIdsExcluded) {
        Set<String> predicates = generatePredicates(state, from, to, session, providerId, aipIds, aipIdsExcluded);
        if (tags != null && !tags.isEmpty()) {
            predicates.add(getDisjunctionTagPredicate(tags));
        }
        return createQuery(predicates);

    }

    /**
     * Return an SQL query that retrieve all tags used by a set of entities
     */
    public static String searchAipTagsUsingSQL(AIPState state, OffsetDateTime from, OffsetDateTime to,
            List<String> tags, String session, String providerId, Set<String> aipIds, Set<String> aipIdsExcluded) {
        StringBuilder request = new StringBuilder(
                "SELECT distinct jsonb_array_elements_text(json_aip#>'{properties,pdi,contextInformation,tags}') "
                        + "FROM {h-schema}t_aip ");
        Set<String> predicates = generatePredicates(state, from, to, session, providerId, aipIds, aipIdsExcluded);
        if (tags != null && !tags.isEmpty()) {
            predicates.add(getConjunctionTagPredicate(tags));
        }
        if (!predicates.isEmpty()) {
            request.append("WHERE ");
            Joiner.on(" AND ").appendTo(request, predicates);
        }
        // Do not handle pagination here. See CustomizedAIPEntityRepository for pagination
        return request.toString();
    }

    private static Set<String> generatePredicates(AIPState state, OffsetDateTime from, OffsetDateTime to,
            String session, String providerId, Set<String> aipIds, Set<String> aipIdsExcluded) {
        Set<String> predicates = Sets.newHashSet();
        if (state != null) {
            predicates.add("(state = '" + state.getName() + "')");
        }
        if (from != null) {
            Timestamp time = Timestamp.valueOf(from.minusNanos(1).atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());
            predicates.add("(date > '" + time.toString() + "')");
        }
        if (to != null) {
            Timestamp time = Timestamp.valueOf(to.plusSeconds(1).atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());
            predicates.add("(date < '" + time.toString() + "')");
        }
        if (session != null) {
            predicates.add("(session = '" + session + "')");
        }
        if (aipIds != null && !aipIds.isEmpty()) {
            Set<String> aipIncludedPredicates = Sets.newHashSet();
            for (String aipId : aipIds) {
                aipIncludedPredicates.add("'" + aipId + "'");
            }
            predicates.add("(aip_id in (" + String.join(" , ", aipIncludedPredicates) + "))");
        }
        if (aipIdsExcluded != null && !aipIdsExcluded.isEmpty()) {
            Set<String> aipExcludedPredicates = Sets.newHashSet();
            for (String aipId : aipIdsExcluded) {
                aipExcludedPredicates.add("'" + aipId + "'");
            }
            predicates.add("(aip_id not in (" + String.join(" , ", aipExcludedPredicates) + "))");
        }
        if (providerId != null && !providerId.isEmpty()) {
            predicates.add("(provider_id like '%" + providerId + "%')");
        }
        return predicates;
    }

    private static String createQuery(Set<String> predicates) {
        StringBuilder request = new StringBuilder("SELECT * ");
        return addFromNWhere(predicates, request);
    }

    private static String createIdQuery(Set<String> predicates) {
        StringBuilder request = new StringBuilder("SELECT id ");
        return addFromNWhere(predicates, request);
    }

    private static String addFromNWhere(Set<String> predicates, StringBuilder request) {
        request.append("FROM {h-schema}t_aip ");
        if (!predicates.isEmpty()) {
            request.append("WHERE ");
            Joiner.on(" AND ").appendTo(request, predicates);
        }
        request.append(" ORDER BY id");
        return request.toString();
    }

    private static String getConjunctionTagPredicate(List<String> tags) {
        Set<String> tagPredicates = Sets.newHashSet();
        for (String tag : tags) {
            tagPredicates.add("'" + tag + "'");
        }
        return "(json_aip#>'{properties,pdi,contextInformation,tags}' @> jsonb_build_array("
                + String.join(" , ", tagPredicates) + "))";
    }

    /**
     * We use here a workaround, we use
     * jsonb_exists_any instead of  ?|
     * Otherwise JDBC would detect the question mark and ask for another parameter
     * @param tags one of the tag present in the AIP
     * @return sql constraint
     */
    private static String getDisjunctionTagPredicate(List<String> tags) {
        Set<String> tagPredicates = Sets.newHashSet();
        for (String tag : tags) {
            tagPredicates.add("'" + tag + "'");
        }
        return "(jsonb_exists_any(json_aip#>'{properties,pdi,contextInformation,tags}', array["
                + String.join(" , ", tagPredicates) + "]))";
    }

}
