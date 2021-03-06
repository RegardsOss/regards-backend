/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import org.springframework.data.jpa.domain.Specification;

import fr.cnes.regards.modules.feature.domain.request.FeatureUpdateRequest;
import fr.cnes.regards.modules.feature.dto.FeatureRequestsSelectionDTO;

/**
 * JPA Specification to search for {@link FeatureUpdateRequest} from {@link IFeatureUpdateRequestRepository}
 *
 * @author Sébastien Binda
 *
 */
public class FeatureUpdateRequestSpecification {

    private FeatureUpdateRequestSpecification() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Creates search {@link Specification} for {@link FeatureUpdateRequest}s
     * @param selection {@link FeatureRequestsSelectionDTO}
     * @param page {@link Pageable}
     * @return {@link Specification}
     */
    public static Specification<FeatureUpdateRequest> searchAllByFilters(FeatureRequestsSelectionDTO selection,
            Pageable page) {
        return (root, query, cb) -> {
            Set<Predicate> predicates = FeatureRequestSpecificationsHelper.init(selection, true, root, query, cb,
                                                                                page);
            if (selection.getFilters() != null) {
                if (selection.getFilters().getProviderId() != null) {
                    predicates.add(cb.like(cb.lower(root.get("providerId")),
                                           selection.getFilters().getProviderId().toLowerCase() + "%"));
                }
            }
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

}
