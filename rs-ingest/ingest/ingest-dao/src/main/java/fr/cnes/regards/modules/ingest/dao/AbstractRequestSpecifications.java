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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.utils.SpecificationUtils;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.dto.request.SearchRequestsParameters;
import java.util.Set;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

/**
 * @author Léo Mieulet
 */
public final class AbstractRequestSpecifications {

    private AbstractRequestSpecifications() {
        throw new IllegalStateException("Utility class");
    }

    public static Specification<AbstractRequest> searchAllByRemoteStepGroupId(String groupId) {
        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();

            if (groupId != null) {
                Path<Object> attributeRequested = root.get("remoteStepGroupIds");
                predicates.add(SpecificationUtils.buildPredicateIsJsonbArrayContainingElements(attributeRequested, Lists.newArrayList(groupId), cb));
            }

            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }



    public static Specification<AbstractRequest> searchAllByFilters(SearchRequestsParameters filters) {
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
            if (filters.getProviderIds() != null && !filters.getProviderIds().isEmpty()) {
                Set<Predicate> providerIdsPredicates = Sets.newHashSet();
                for (String providerId: filters.getProviderIds()) {
                    if (providerId.startsWith(SpecificationUtils.LIKE_CHAR) || providerId.endsWith(SpecificationUtils.LIKE_CHAR)) {
                        providerIdsPredicates.add(cb.like(root.get("providerId"), providerId));
                    } else {
                        providerIdsPredicates.add(cb.equal(root.get("providerId"), providerId));
                    }
                }
                // Use the OR operator between each provider id
                predicates.add(cb.or(providerIdsPredicates.toArray(new Predicate[providerIdsPredicates.size()])));
            }
            if (filters.getRequestType() != null) {
                predicates.add(cb.equal(root.get("dtype"), filters.getRequestType().name()));
            }
            if (filters.getState() != null) {
                predicates.add(cb.equal(root.get("state"), filters.getState()));
            }
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

}
