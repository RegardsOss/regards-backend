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

import static fr.cnes.regards.modules.ingest.dao.AbstractRequestSpecifications.DISCRIMINANT_ATTRIBUTE;
import static fr.cnes.regards.modules.ingest.dao.AbstractRequestSpecifications.STATE_ATTRIBUTE;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.utils.SpecificationUtils;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.dto.request.ChooseVersioningRequestParameters;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeConstant;

/**
 * JPA {@link Specification} to define {@link Predicate}s for criteria search for {@link IngestRequest} from repository.
 * @author Léo Mieulet
 */
public final class IngestRequestSpecifications {

    private IngestRequestSpecifications() {
        throw new IllegalStateException("Utility class");
    }

    public static Specification<IngestRequest> searchByRemoteStepId(String remoteStepGroupId) {
        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();
            Path<Object> attributeRequeted = root.get("remoteStepGroupIds");
            predicates.add(SpecificationUtils
                    .buildPredicateIsJsonbArrayContainingElements(attributeRequeted,
                                                                  Lists.newArrayList(remoteStepGroupId), cb));
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

    public static Specification<IngestRequest> searchByRemoteStepIds(List<String> remoteStepGroupIds) {
        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();
            Path<Object> attributeRequeted = root.get("remoteStepGroupIds");
            predicates.add(SpecificationUtils.buildPredicateIsJsonbArrayContainingElements(attributeRequeted,
                                                                                           remoteStepGroupIds, cb));
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

    public static Specification<IngestRequest> searchAllByFilters(ChooseVersioningRequestParameters filters,
            Pageable page) {
        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();

            if (filters.getCreationDate().getFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("creationDate"), filters.getCreationDate().getFrom()));
            }
            if (filters.getCreationDate().getTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("creationDate"), filters.getCreationDate().getTo()));
            }
            if (filters.getSessionOwner() != null) {
                predicates.add(cb.equal(root.get("sessionOwner"), filters.getSessionOwner()));
            }
            if (filters.getSession() != null) {
                predicates.add(cb.equal(root.get("session"), filters.getSession()));
            }
            Set<String> requestIds = filters.getRequestIds();
            if ((requestIds != null) && !requestIds.isEmpty()) {
                Set<Predicate> idsPredicates = Sets.newHashSet();
                switch (filters.getRequestIdSelectionMode()) {
                    case EXCLUDE:
                        requestIds.forEach(requestId -> idsPredicates.add(cb.notEqual(root.get("id"), requestId)));
                        predicates.add(cb.and(idsPredicates.toArray(new Predicate[idsPredicates.size()])));
                        break;
                    case INCLUDE:
                    default:
                        requestIds.forEach(requestId -> idsPredicates.add(cb.equal(root.get("id"), requestId)));
                        predicates.add(cb.or(idsPredicates.toArray(new Predicate[idsPredicates.size()])));
                        break;
                }
            }

            if ((filters.getProviderIds() != null) && !filters.getProviderIds().isEmpty()) {
                Set<Predicate> providerIdsPredicates = Sets.newHashSet();
                for (String providerId : filters.getProviderIds()) {
                    if (providerId.startsWith(SpecificationUtils.LIKE_CHAR)
                            || providerId.endsWith(SpecificationUtils.LIKE_CHAR)) {
                        providerIdsPredicates.add(cb.like(root.get("providerId"), providerId));
                    } else {
                        providerIdsPredicates.add(cb.equal(root.get("providerId"), providerId));
                    }
                }
                // Use the OR operator between each provider id
                predicates.add(cb.or(providerIdsPredicates.toArray(new Predicate[0])));
            }
            if (filters.getRequestType() != null) {
                predicates.add(cb.equal(root.get(DISCRIMINANT_ATTRIBUTE), RequestTypeConstant.INGEST_VALUE));
            }
            Set<InternalRequestState> states = filters.getStates();
            if ((states != null) && !states.isEmpty()) {
                Set<Predicate> statePredicates = new HashSet<>();
                for (InternalRequestState state : filters.getStates()) {
                    statePredicates.add(cb.equal(root.get(STATE_ATTRIBUTE), state));
                }
                predicates.add(cb.or(statePredicates.toArray(new Predicate[0])));
            }
            if (filters.getStateExcluded() != null) {
                predicates.add(cb.notEqual(root.get(STATE_ATTRIBUTE), filters.getStateExcluded()));
            }

            // Add order
            Sort.Direction defaultDirection = Sort.Direction.ASC;
            String defaultAttribute = "id";
            query.orderBy(SpecificationUtils.buildOrderBy(page, root, cb, defaultAttribute, defaultDirection));

            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }
}
