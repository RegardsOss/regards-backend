/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.Set;

import javax.persistence.criteria.Predicate;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.utils.SpecificationUtils;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;

/**
 * Specification class to filter DAO searches on {@link AIPEntity} entities
 * @author Léo Mieulet
 */
public final class AIPEntitySpecification {

    private AIPEntitySpecification() {
        throw new IllegalStateException("Utility class");
    }

    public static Specification<AIPEntity> searchAll(SearchAIPsParameters filters, Pageable page) {
        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();
            if (filters.getState() != null) {
                predicates.add(cb.equal(root.get("state"), filters.getState()));
            }
            if (filters.getFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("lastUpdate"), filters.getFrom()));
            }
            if (filters.getTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("lastUpdate"), filters.getTo()));
            }
            predicates.addAll(OAISEntitySpecification
                    .buildCommonPredicate(root, cb, filters.getTags(), filters.getSessionOwner(),
                                          filters.getSession(), filters.getProviderId(), filters.getStorages(),
                                          filters.getCategories()));
            query.orderBy(cb.desc(root.get("creationDate")));

            // Add order
            Sort.Direction defaultDirection = Sort.Direction.ASC;
            String defaultAttribute = "creationDate";
            query.orderBy(SpecificationUtils.buildOrderBy(page, root, cb, defaultAttribute, defaultDirection));

            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

}