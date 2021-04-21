/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.sessionmanager.dao;

import com.google.common.collect.Sets;
import fr.cnes.regards.modules.sessionmanager.domain.Session;
import fr.cnes.regards.modules.sessionmanager.domain.SessionState;
import java.time.OffsetDateTime;
import java.util.Set;
import javax.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

/**
 * Specification class to filter DAO searches on {@link Session} entities
 * @author Léo Mieulet
 */
public class SessionSpecifications {

    private SessionSpecifications() {}

    /**
     * Filter on given attributes and return result
     * @return {@link Specification}<{@link Session}>
     */
    public static Specification<Session> search(String source, String name, OffsetDateTime from, OffsetDateTime to, SessionState state, boolean onlyLastSession) {
        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();
            if (name != null) {
                // Search with insensitive case
                predicates.add(cb.like(cb.lower(root.get("name")), cb.lower(cb.literal("%" + name + "%"))));
            }

            if (source != null) {
                // Search with insensitive case
                predicates.add(cb.like(cb.lower(root.get("source")), cb.lower(cb.literal("%" + source + "%"))));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("creationDate"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("creationDate"), to));
            }
            if (state != null) {
                predicates.add(cb.equal(root.get("state"), state));
            }
            if (onlyLastSession) {
                predicates.add(cb.equal(root.get("isLatest"), true));
            }

            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

}