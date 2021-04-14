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
package fr.cnes.regards.modules.feature.dao;

import java.util.Set;

import javax.persistence.criteria.Predicate;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.utils.SpecificationUtils;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.dto.FeaturesSearchParameters;

/**
 * JPA {@link Specification} builder for {@link FeatureEntity}
 *
 * @author Sébastien Binda
 *
 */
public class FeatureEntitySpecification {

    private FeatureEntitySpecification() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Creates search {@link Specification} for {@link FeatureEntity}s
     * @param selection {@link FeaturesSearchParameters}
     * @param page {@link Pageable}
     * @return {@link Specification}
     */
    public static Specification<FeatureEntity> searchAllByFilters(FeaturesSearchParameters selection, Pageable page) {
        return (root, query, cb) -> {

            Set<Predicate> predicates = Sets.newHashSet();

            if (selection.getModel() != null) {
                predicates.add(cb.equal(root.get("model"), selection.getModel()));
            }
            if (selection.getSource() != null) {
                predicates.add(cb.equal(root.get("sessionOwner"), selection.getSource()));
            }
            if (selection.getSession() != null) {
                predicates.add(cb.equal(root.get("session"), selection.getSession()));
            }
            if (selection.getProviderId() != null) {
                predicates.add(cb.equal(root.get("providerId"), selection.getProviderId()));
            }
            if (selection.getFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("lastUpdate"), selection.getFrom()));
            }
            if (selection.getTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("lastUpdate"), selection.getTo()));
            }

            // Add order
            Sort.Direction defaultDirection = Sort.Direction.ASC;
            String defaultAttribute = "id";
            query.orderBy(SpecificationUtils.buildOrderBy(page, root, cb, defaultAttribute, defaultDirection));

            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

}
