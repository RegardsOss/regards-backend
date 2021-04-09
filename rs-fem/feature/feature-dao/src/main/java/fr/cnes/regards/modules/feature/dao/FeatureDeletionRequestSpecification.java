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
import org.springframework.data.jpa.domain.Specification;

import fr.cnes.regards.modules.feature.domain.request.FeatureDeletionRequest;
import fr.cnes.regards.modules.feature.dto.FeatureRequestsSelectionDTO;

/**
 * JPA Specification to search for {@link FeatureDeletionRequest} from {@link IFeatureDeletionRequestRepository}
 *
 * @author Sébastien Binda
 *
 */
public class FeatureDeletionRequestSpecification {

    private FeatureDeletionRequestSpecification() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Creates search {@link Specification} for {@link FeatureDeletionRequest}s
     * @param selection {@link FeatureRequestsSelectionDTO}
     * @param page {@link Pageable}
     * @return {@link Specification}
     */
    public static Specification<FeatureDeletionRequest> searchAllByFilters(FeatureRequestsSelectionDTO selection,
            Pageable page) {
        return (root, query, cb) -> {
            Set<Predicate> predicates = FeatureRequestSpecificationsHelper.init(selection, true, root, query, cb, page);
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

}
