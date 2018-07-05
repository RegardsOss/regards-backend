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

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.database.AIPEntity;
import fr.cnes.regards.modules.storage.domain.database.AIPSession;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;


/**
 * Specification class to filter DAO searches on {@link AIP} entities
 *
 * @author Léo Mieulet
 */
public class AIPSpecification {
    private AIPSpecification() {
    }

    /**
     * Filter on given attributes and return a Specification that contains all query filters
     */
    public static Specification<AIPEntity> search(AIPState state, OffsetDateTime from, OffsetDateTime to, List<String> tags, AIPSession session, Set<String> aipIds, Set<String> aipIdsExcluded) {
        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();
            if (state != null) {
                predicates.add(cb.equal(root.get("state"), state));
            }
            if (from != null) {
                predicates.add(cb.greaterThan(root.get("submissionDate"), from.minusNanos(1)));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("lastEvent").get("date"), to.plusSeconds(1)));
            }
            if (session != null) {
                predicates.add(cb.equal(root.get("session"), session));
            }
            if (aipIds != null && !aipIds.isEmpty()) {
                predicates.add(root.get("ipId").in(aipIds.toArray()));
            }
            if (aipIdsExcluded != null && !aipIdsExcluded.isEmpty()) {
                predicates.add(root.get("ipId").in(aipIdsExcluded.toArray()).not());
            }
            if (tags != null && !tags.isEmpty()) {
                Expression<Set<String>> tagsExpr = root.get("tags");
                for (String tag : tags) {
                    predicates.add(cb.isMember(tag, tagsExpr));
                }
            }

            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }


    /**
     * Return an SQL query that retrieve all tags used by a set of entities
     */
    public static String searchAipTagsUsingSQL(AIPState state, OffsetDateTime from, OffsetDateTime to, List<String> tags, AIPSession session, Set<String> aipIds, Set<String> aipIdsExcluded) {
        Set<String> predicates = Sets.newHashSet();
        StringBuilder request = new StringBuilder("select distinct t_aip_tag.value from storage.t_aip left outer join " +
                "storage.t_aip_tag on t_aip.id=t_aip_tag.aip_id ");
        if (state != null) {
            predicates.add("(state = '" + state.getName() + "')");
        }
        if (from != null) {
            Timestamp time = Timestamp.valueOf(from.minusNanos(1).atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());
            predicates.add("(submission_date > '" + time.toString() + "')");
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
            predicates.add("(ip_id in (" + String.join(" , ", aipIncludedPredicates) + "))");
        }
        if (aipIdsExcluded != null && !aipIdsExcluded.isEmpty()) {
            Set<String> aipExcludedPredicates = Sets.newHashSet();
            for (String aipId : aipIdsExcluded) {
                aipExcludedPredicates.add("'" + aipId + "'");
            }
            predicates.add("(ip_id not in (" + String.join(" , ", aipExcludedPredicates) + "))");
        }
        if (tags != null && !tags.isEmpty()) {
            int i = 0;
            for (String tag : tags) {
                predicates.add("('" + tag + "' in (select tags" + i + ".value " +
                        "from storage.t_aip_tag tags" + i + " where t_aip.id=tags" + i + ".aip_id))");
                i++;
            }
        }
        if (!predicates.isEmpty()) {
            request.append("WHERE ");
            Joiner.on(" AND ").appendTo(request, predicates);
        }
        return request.toString();
    }


}

