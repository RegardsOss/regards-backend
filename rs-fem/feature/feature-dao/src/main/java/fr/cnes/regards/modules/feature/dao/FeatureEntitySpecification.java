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

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.utils.SpecificationUtils;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.dto.FeaturesSearchParameters;
import fr.cnes.regards.modules.feature.dto.FeaturesSelectionDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Predicate;
import java.util.List;
import java.util.Set;

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
    public static Specification<FeatureEntity> searchAllByFilters(FeaturesSelectionDTO selection, Pageable page) {

        return (root, query, criteriaBuilder) -> {

            Set<Predicate> predicates = Sets.newHashSet();
            FeaturesSearchParameters filters = selection.getFilters();
            List<Long> featureIds = selection.getFeatureIds();

            if (filters != null) {
                if (filters.getModel() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("model"), filters.getModel()));
                }
                if (filters.getSource() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("sessionOwner"), filters.getSource()));
                }
                if (filters.getSession() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("session"), filters.getSession()));
                }
                if (filters.getProviderId() != null) {
                    predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("providerId")), filters.getProviderId().toLowerCase() + "%"));
                }
                if (filters.getFrom() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("lastUpdate"), filters.getFrom()));
                }
                if (filters.getTo() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("lastUpdate"), filters.getTo()));
                }
            }

            if (!featureIds.isEmpty()) {
                Set<Predicate> idsPredicates = Sets.newHashSet();
                switch (selection.getFeatureIdsSelectionMode()) {
                    case EXCLUDE:
                        featureIds.forEach(requestId -> idsPredicates.add(criteriaBuilder.notEqual(root.get("id"), requestId)));
                        predicates.add(criteriaBuilder.and(idsPredicates.toArray(new Predicate[0])));
                        break;
                    case INCLUDE:
                        featureIds.forEach(requestId -> idsPredicates.add(criteriaBuilder.equal(root.get("id"), requestId)));
                        predicates.add(criteriaBuilder.or(idsPredicates.toArray(new Predicate[0])));
                        break;
                    default:
                        break;
                }
            }

            query.orderBy(SpecificationUtils.buildOrderBy(page, root, criteriaBuilder, "id", Sort.Direction.ASC));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

}
