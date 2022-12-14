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

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.utils.SpecificationUtils;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.AbstractFeatureRequest;
import fr.cnes.regards.modules.feature.domain.request.AbstractRequest;
import fr.cnes.regards.modules.feature.dto.FeatureRequestsSelectionDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.Set;

/**
 * JPA {@link Specification} to search for {@link AbstractRequest}s
 *
 * @author Léo Mieulet
 * @author Sébastien Binda
 */
public final class FeatureRequestSpecificationsHelper {

    /**
     * Creates search {@link Predicate}s for {@link Specification} search request about {@link AbstractFeatureRequest}s
     *
     * @param filters                            {@link FeatureRequestsSelectionDTO}
     * @param searchFiltersFromAssociatedFeature boolean
     * @param root
     * @param query
     * @param cb
     * @param page                               {@link Pageable}
     * @return {@link Specification}
     */
    public static Set<Predicate> init(FeatureRequestsSelectionDTO selection,
                                      boolean searchFiltersFromAssociatedFeature,
                                      Root<?> root,
                                      CriteriaQuery<?> query,
                                      CriteriaBuilder cb,
                                      Pageable page) {
        Set<Predicate> predicates = Sets.newHashSet();

        if (selection.getFilters() != null) {
            if (selection.getFilters().getFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("registrationDate"), selection.getFilters().getFrom()));
            }
            if (selection.getFilters().getTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("registrationDate"), selection.getFilters().getTo()));
            }
            if (selection.getFilters().getState() != null) {
                predicates.add(cb.equal(root.get("state"), selection.getFilters().getState()));
            }
            if ((selection.getFilters().getSteps() != null) && !selection.getFilters().getSteps().isEmpty()) {
                Set<Predicate> stepsPredicates = Sets.newHashSet();
                selection.getFilters()
                         .getSteps()
                         .forEach(step -> stepsPredicates.add(cb.equal(root.get("step"), step)));
                if (!stepsPredicates.isEmpty()) {
                    predicates.add(cb.or(stepsPredicates.toArray(new Predicate[stepsPredicates.size()])));
                }
            }
            // Some filters are not provided on request itself, we have to join with assocciated feature by urn if present
            if (searchFiltersFromAssociatedFeature) {

                if ((selection.getFilters().getProviderId() != null)) {
                    Root<FeatureEntity> fr = query.from(FeatureEntity.class);
                    predicates.add(cb.equal(fr.get("urn"), root.get("urn")));

                    predicates.add(cb.like(cb.lower(fr.get("providerId")),
                                           selection.getFilters().getProviderId().toLowerCase() + "%"));
                }

                if ((selection.getFilters().getSource() != null)) {
                    Root<FeatureEntity> fr = query.from(FeatureEntity.class);
                    predicates.add(cb.equal(fr.get("urn"), root.get("urn")));
                    predicates.add(cb.equal(fr.get("sessionOwner"), selection.getFilters().getSource()));
                }
                if ((selection.getFilters().getSession() != null)) {
                    Root<FeatureEntity> fr = query.from(FeatureEntity.class);
                    predicates.add(cb.equal(fr.get("urn"), root.get("urn")));
                    predicates.add(cb.equal(fr.get("session"), selection.getFilters().getSession()));
                }
            }
            if (!selection.getRequestIds().isEmpty()) {
                Set<Predicate> idsPredicates = Sets.newHashSet();
                switch (selection.getRequestIdSelectionMode()) {
                    case EXCLUDE:
                        selection.getRequestIds()
                                 .forEach(requestId -> idsPredicates.add(cb.notEqual(root.get("id"), requestId)));
                        predicates.add(cb.and(idsPredicates.toArray(new Predicate[idsPredicates.size()])));
                        break;
                    case INCLUDE:
                        selection.getRequestIds()
                                 .forEach(requestId -> idsPredicates.add(cb.equal(root.get("id"), requestId)));
                        predicates.add(cb.or(idsPredicates.toArray(new Predicate[idsPredicates.size()])));
                        break;
                    default:
                        break;
                }
            }
        }

        // Add order
        Sort.Direction defaultDirection = Sort.Direction.ASC;
        String defaultAttribute = "id";
        query.orderBy(SpecificationUtils.buildOrderBy(page, root, cb, defaultAttribute, defaultDirection));
        return predicates;
    }

}
