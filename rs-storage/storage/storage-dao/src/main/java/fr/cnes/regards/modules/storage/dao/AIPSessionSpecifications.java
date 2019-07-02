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

import com.google.common.collect.Sets;
import fr.cnes.regards.modules.storage.domain.database.AIPSession;
import java.time.OffsetDateTime;
import java.util.Set;
import javax.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

/**
 * Specification class to filter DAO searches on {@link AIPSession} entities
 *
 * @author Léo Mieulet
 */
public class AIPSessionSpecifications {

    private static final String SIP_SESSION_LAST_ACTIVATION_DATE = "lastActivationDate";

    private AIPSessionSpecifications() {
    }

    /**
     * Filter on given attributes and return result
     *
     * @return {@link Specification}<{@link AIPSession}>
     */
    public static Specification<AIPSession> search(String id, OffsetDateTime from, OffsetDateTime to) {
        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get(SIP_SESSION_LAST_ACTIVATION_DATE), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get(SIP_SESSION_LAST_ACTIVATION_DATE), to));
            }
            if (id != null) {
                predicates.add(cb.like(root.get("id"), "%" + id + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }
}