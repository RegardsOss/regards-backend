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
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeConstant;
import fr.cnes.regards.modules.ingest.dto.request.SearchRequestsParameters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

/**
 * @author Léo Mieulet
 */
public final class AbstractRequestSpecifications {

    private AbstractRequestSpecifications() {
        throw new IllegalStateException("Utility class");
    }

    public static Specification<AbstractRequest> searchAllByRemoteStepGroupId(List<String> groupIds) {
        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();

            if ((groupIds != null) && !groupIds.isEmpty()) {
                Path<Object> attributeRequested = root.get("remoteStepGroupIds");
                predicates.add(SpecificationUtils.buildPredicateIsJsonbArrayContainingOneOfElement(attributeRequested,
                                                                                                   groupIds, cb));
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

    public static Specification<AbstractRequest> searchRequestBlockingAipUpdatesCreator(Optional<String> sessionOwner, Optional<String> session) {

        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();

            predicates.add(AbstractRequestSpecifications.aggregateRequest(cb,
                    AbstractRequestSpecifications.searchStoreMetadata(root, cb, sessionOwner, session),
                    AbstractRequestSpecifications.searchOAISDeletion(root, cb)
            ));

            predicates.add(AbstractRequestSpecifications.getRunningRequestFilter(root, cb));

            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }


    public static Specification<AbstractRequest> searchRequestBlockingUpdate(Optional<String> sessionOwner, Optional<String> session) {

        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();

            predicates.add(AbstractRequestSpecifications.aggregateRequest(cb,
                    AbstractRequestSpecifications.searchStoreMetadata(root, cb, sessionOwner, session),
                    AbstractRequestSpecifications.searchStorageDeletion(root, cb, sessionOwner, session),
                    AbstractRequestSpecifications.searchOAISDeletion(root, cb)
            ));

            predicates.add(AbstractRequestSpecifications.getRunningRequestFilter(root, cb));

            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }


    public static Specification<AbstractRequest> searchRequestBlockingStore(Optional<String> sessionOwner, Optional<String> session) {

        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();

            predicates.add(AbstractRequestSpecifications.aggregateRequest(cb,
                    AbstractRequestSpecifications.searchUpdate(root, cb, sessionOwner, session),
                    AbstractRequestSpecifications.searchStorageDeletion(root, cb, sessionOwner, session),
                    AbstractRequestSpecifications.searchAipUpdatesCreator(root, cb),
                    AbstractRequestSpecifications.searchOAISDeletion(root, cb)
            ));

            predicates.add(AbstractRequestSpecifications.getRunningRequestFilter(root, cb));

            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }


    public static Specification<AbstractRequest> searchRequestBlockingOAISDeletion(Optional<String> sessionOwner, Optional<String> session) {

        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();

            predicates.add(AbstractRequestSpecifications.aggregateRequest(cb,
                    AbstractRequestSpecifications.searchStoreMetadata(root, cb, sessionOwner, session),
                    AbstractRequestSpecifications.searchUpdate(root, cb, sessionOwner, session),
                    AbstractRequestSpecifications.searchAipUpdatesCreator(root, cb)
            ));

            predicates.add(AbstractRequestSpecifications.getRunningRequestFilter(root, cb));

            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }


    public static Specification<AbstractRequest> searchRequestBlockingStorageDeletion(Optional<String> sessionOwner, Optional<String> session) {

        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();

            predicates.add(AbstractRequestSpecifications.aggregateRequest(cb,
                    AbstractRequestSpecifications.searchStoreMetadata(root, cb, sessionOwner, session),
                    AbstractRequestSpecifications.searchUpdate(root, cb, sessionOwner, session),
                    AbstractRequestSpecifications.searchAipUpdatesCreator(root, cb)
            ));

            predicates.add(AbstractRequestSpecifications.getRunningRequestFilter(root, cb));

            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

    public static Predicate searchStorageDeletion(Root<AbstractRequest> root, CriteriaBuilder cb, Optional<String> sessionOwner, Optional<String> session) {
        return AbstractRequestSpecifications.searchMicroRequest(root, cb, sessionOwner, session, RequestTypeConstant.STORAGE_DELETION_VALUE);
    }

    public static Predicate searchIngest(Root<AbstractRequest> root, CriteriaBuilder cb, Optional<String> sessionOwner, Optional<String> session) {
        return AbstractRequestSpecifications.searchMicroRequest(root, cb, sessionOwner, session, RequestTypeConstant.INGEST_VALUE);
    }

    public static Predicate searchStoreMetadata(Root<AbstractRequest> root, CriteriaBuilder cb, Optional<String> sessionOwner, Optional<String> session) {
        return AbstractRequestSpecifications.searchMicroRequest(root, cb, sessionOwner, session, RequestTypeConstant.STORE_METADATA_VALUE);
    }

    public static Predicate searchUpdate(Root<AbstractRequest> root, CriteriaBuilder cb, Optional<String> sessionOwner, Optional<String> session) {
        return AbstractRequestSpecifications.searchMicroRequest(root, cb, sessionOwner, session, RequestTypeConstant.UPDATE_VALUE);
    }

    public static Predicate searchOAISDeletion(Root<AbstractRequest> root, CriteriaBuilder cb) {
        return AbstractRequestSpecifications.searchMacroRequest(root, cb, RequestTypeConstant.OAIS_DELETION_VALUE);
    }

    public static Predicate searchAipUpdatesCreator(Root<AbstractRequest> root, CriteriaBuilder cb) {
        return AbstractRequestSpecifications.searchMacroRequest(root, cb, RequestTypeConstant.AIP_UPDATES_CREATOR_VALUE);
    }

    public static Predicate searchMicroRequest(Root<AbstractRequest> root, CriteriaBuilder cb,
            Optional<String> sessionOwner, Optional<String> session, String requestType) {
        Set<Predicate> predicates = Sets.newHashSet();
        if (sessionOwner.isPresent()) {
            predicates.add(cb.equal(root.get("sessionOwner"), sessionOwner.get()));
        }
        if (session.isPresent()) {
            predicates.add(cb.equal(root.get("session"), session.get()));
        }
        predicates.add(cb.equal(root.get("dtype"), requestType));
        return cb.and(predicates.toArray(new Predicate[predicates.size()]));
    }

    public static Predicate searchMacroRequest(Root<AbstractRequest> root, CriteriaBuilder cb, String requestType) {
        return cb.and(cb.equal(root.get("dtype"), requestType));
    }

    public static Predicate getRunningRequestFilter(Root<AbstractRequest> root, CriteriaBuilder cb) {
        Set<Predicate> statePredicates = Sets.newHashSet();
        ArrayList<InternalRequestState> runningStates = Lists.newArrayList(InternalRequestState.CREATED,
                InternalRequestState.RUNNING);
        for (InternalRequestState state : runningStates) {
            statePredicates.add(cb.equal(root.get("state"), state));
        }
        // Also add the limit
        // Use the OR operator between each state
        return cb.or(statePredicates.toArray(new Predicate[statePredicates.size()]));
    }


    public static Predicate aggregateRequest(CriteriaBuilder cb, Predicate ... predicates) {
        // Use the OR operator between each state
        return cb.or(predicates);
    }
}
