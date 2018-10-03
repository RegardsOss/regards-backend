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
import fr.cnes.regards.modules.storage.domain.database.AIPSession;

/**
 * Specification class to filter DAO searches on {@link AIP} entities
 *
 * @author Léo Mieulet
 */
public class AIPQueryGenerator {

    private AIPQueryGenerator() {
    }

    /**
     * Return an SQL query that retrieve all entities matching provided criteria
     */
    public static String search(AIPState state, OffsetDateTime from, OffsetDateTime to, List<String> tags,
            AIPSession session, String providerId, Set<String> aipIds, Set<String> aipIdsExcluded) {
        StringBuilder request = new StringBuilder("SELECT * " + "FROM {h-schema}t_aip ");
        Set<String> predicates = generatePredicates(state, from, to, tags, session, providerId, aipIds, aipIdsExcluded);
        if (!predicates.isEmpty()) {
            request.append("WHERE ");
            Joiner.on(" AND ").appendTo(request, predicates);
        }
        return request.toString();
    }

    /**
     * Return an SQL query that retrieve all tags used by a set of entities
     */
    public static String searchAipTagsUsingSQL(AIPState state, OffsetDateTime from, OffsetDateTime to,
            List<String> tags, AIPSession session, String providerId, Set<String> aipIds, Set<String> aipIdsExcluded) {
        StringBuilder request = new StringBuilder(
                "SELECT distinct jsonb_array_elements_text(json_aip->'properties'->'pdi'->'contextInformation'->'tags') "
                        + "FROM {h-schema}t_aip ");
        Set<String> predicates = generatePredicates(state, from, to, tags, session, providerId, aipIds, aipIdsExcluded);
        if (!predicates.isEmpty()) {
            request.append("WHERE ");
            Joiner.on(" AND ").appendTo(request, predicates);
        }
        // Do not handle pagination here. See CustomizedAIPEntityRepository for pagination
        return request.toString();
    }

    private static Set<String> generatePredicates(AIPState state, OffsetDateTime from, OffsetDateTime to,
            List<String> tags, AIPSession session, String providerId, Set<String> aipIds, Set<String> aipIdsExcluded) {
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
            predicates.add("(session = '" + session.getId() + "')");
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
        if (tags != null && !tags.isEmpty()) {
            Set<String> tagPredicates = Sets.newHashSet();
            for (String tag : tags) {
                tagPredicates.add("'" + tag + "'");
            }
            predicates.add("(json_aip->'properties'->'pdi'->'contextInformation'->'tags' @> jsonb_build_array("
                    + String.join(" , ", tagPredicates) + "))");
        }
        if (providerId != null && !providerId.isEmpty()) {
            predicates.add("(provider_id like '%" + providerId + "%')");
        }
        return predicates;
    }

}
