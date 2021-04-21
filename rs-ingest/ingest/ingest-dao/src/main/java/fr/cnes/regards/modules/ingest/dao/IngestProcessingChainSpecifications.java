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
package fr.cnes.regards.modules.ingest.dao;

import com.google.common.collect.Sets;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import java.util.Set;
import javax.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

/**
 * JPA Repository to manage {@link IngestProcessingChain} entities.
 * @author Sébastien Binda
 */
public final class IngestProcessingChainSpecifications {

    private static final String LIKE_CHAR = "%";

    private IngestProcessingChainSpecifications() {
    }

    /**
     * Filter on the given attributes
     */
    public static Specification<IngestProcessingChain> search(String name) {
        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();
            if (name != null) {
                predicates.add(cb.like(root.get("name"), LIKE_CHAR + name + LIKE_CHAR));
            }
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

}
