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

import java.time.OffsetDateTime;
import java.util.Set;

import javax.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import com.google.common.collect.Sets;

import fr.cnes.regards.modules.ingest.domain.entity.IngestProcessingChain;

/**
 * JPA Repository to manage {@link IngestProcessingChain} entities.
 * @author Sébastien Binda
 */
public final class IngestProcessingChainSpecifications {

    private static final String LIKE_CHAR = "%";

    private IngestProcessingChainSpecifications() {
    }

    /**
     * Filter on the given attributes (sessionId, owner, ingestDate and state) and return result ordered by descending ingestDate
     * @param id {@link String}
     * @param from {@link OffsetDateTime}
     * @param to {@link OffsetDateTime}
     * @return
     */
    public static Specification<IngestProcessingChain> search(String name) {
        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();
            if (name != null) {
                predicates.add(cb.like(root.get("name"), LIKE_CHAR + name + LIKE_CHAR));
            }
            query.orderBy(cb.desc(root.get("name")));
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

}
