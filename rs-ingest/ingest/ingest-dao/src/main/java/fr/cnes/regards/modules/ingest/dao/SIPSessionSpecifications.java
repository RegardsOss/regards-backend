/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import javax.persistence.criteria.Predicate;
import java.time.OffsetDateTime;
import java.util.Set;

import org.springframework.data.jpa.domain.Specification;

import com.google.common.collect.Sets;
import fr.cnes.regards.modules.ingest.domain.entity.SIPSession;

/**
 * JPA {@link Specification} to define {@link Predicate}s for criteria search for {@link SIPSession} from repository.
 * @author Sébastien Binda
 */
public final class SIPSessionSpecifications {

    private static final String SIP_SESSION_LAST_ACTIVATION_DATE = "lastActivationDate";

    private static final String LIKE_CHAR = "%";

    private SIPSessionSpecifications() {
    }

    /**
     * Filter on the given attributes (sessionId, owner, ingestDate and state) and return result ordered by descending ingestDate
     * @param id {@link String}
     * @param from {@link OffsetDateTime}
     * @param to {@link OffsetDateTime}
     * @return
     */
    public static Specification<SIPSession> search(String id, OffsetDateTime from, OffsetDateTime to) {
        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();
            if (id != null) {
                predicates.add(cb.like(root.get("id"), LIKE_CHAR + id + LIKE_CHAR));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get(SIP_SESSION_LAST_ACTIVATION_DATE), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get(SIP_SESSION_LAST_ACTIVATION_DATE), to));
            }
            query.orderBy(cb.desc(root.get(SIP_SESSION_LAST_ACTIVATION_DATE)));
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

}
