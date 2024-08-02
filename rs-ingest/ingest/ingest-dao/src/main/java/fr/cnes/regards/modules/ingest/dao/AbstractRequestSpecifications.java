/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * JPA {@link Specification} to search for {@link AbstractRequest}s
 *
 * @author Léo Mieulet
 * @author Sébastien Binda
 */
public final class AbstractRequestSpecifications {

    public static final String DISCRIMINANT_ATTRIBUTE = "dtype";

    public static final String STATE_ATTRIBUTE = "state";

    private AbstractRequestSpecifications() {
        throw new IllegalStateException("Utility class");
    }

    public static Specification<AbstractRequest> searchAllByRemoteStepGroupId(List<String> groupIds) {
        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();

            if ((groupIds != null) && !groupIds.isEmpty()) {
                Path<Object> attributeRequested = root.get("remoteStepGroupIds");
                predicates.add(SpecificationUtils.buildPredicateIsJsonbArrayContainingOneOfElement(attributeRequested,
                                                                                                   groupIds,
                                                                                                   cb));
            }

            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

    public static Specification<AbstractRequest> searchRequestBlockingAipUpdatesCreator() {
        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();

            predicates.add(AbstractRequestSpecifications.aggregateRequest(cb,
                                                                          AbstractRequestSpecifications.searchAipDisseminationCreator(
                                                                              root,
                                                                              cb),
                                                                          AbstractRequestSpecifications.searchOAISDeletionCreator(
                                                                              root,
                                                                              cb)));

            predicates.add(AbstractRequestSpecifications.getRunningRequestFilter(root, cb));

            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

    public static Specification<AbstractRequest> searchRequestBlockingUpdate(Optional<String> sessionOwner,
                                                                             Optional<String> session) {
        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();

            predicates.add(AbstractRequestSpecifications.aggregateRequest(cb,
                                                                          AbstractRequestSpecifications.searchAipDisseminationCreator(
                                                                              root,
                                                                              cb),
                                                                          AbstractRequestSpecifications.searchOAISDissemiation(
                                                                              root,
                                                                              cb,
                                                                              sessionOwner,
                                                                              session),
                                                                          AbstractRequestSpecifications.searchOAISDeletion(
                                                                              root,
                                                                              cb,
                                                                              sessionOwner,
                                                                              session),
                                                                          AbstractRequestSpecifications.searchOAISDeletionCreator(
                                                                              root,
                                                                              cb),
                                                                          AbstractRequestSpecifications.searchPostProcess(
                                                                              root,
                                                                              cb,
                                                                              sessionOwner,
                                                                              session)));

            predicates.add(AbstractRequestSpecifications.getRunningRequestFilter(root, cb));

            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

    public static Specification<AbstractRequest> searchRequestBlockingOAISDeletion(Optional<String> sessionOwner,
                                                                                   Optional<String> session) {
        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();

            predicates.add(AbstractRequestSpecifications.aggregateRequest(cb,
                                                                          AbstractRequestSpecifications.searchAipDisseminationCreator(
                                                                              root,
                                                                              cb),
                                                                          AbstractRequestSpecifications.searchUpdate(
                                                                              root,
                                                                              cb,
                                                                              sessionOwner,
                                                                              session),
                                                                          AbstractRequestSpecifications.searchAipUpdatesCreator(
                                                                              root,
                                                                              cb),
                                                                          AbstractRequestSpecifications.searchPostProcess(
                                                                              root,
                                                                              cb,
                                                                              sessionOwner,
                                                                              session),
                                                                          AbstractRequestSpecifications.searchOAISDissemiation(
                                                                              root,
                                                                              cb,
                                                                              sessionOwner,
                                                                              session),
                                                                          AbstractRequestSpecifications.searchOAISDeletionCreator(
                                                                              root,
                                                                              cb)));

            predicates.add(AbstractRequestSpecifications.getRunningRequestFilter(root, cb));

            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

    public static Specification<AbstractRequest> searchRequestBlockingOAISDeletionCreator(Optional<String> sessionOwner,
                                                                                          Optional<String> session) {
        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();

            predicates.add(AbstractRequestSpecifications.aggregateRequest(cb,
                                                                          AbstractRequestSpecifications.searchAipDisseminationCreator(
                                                                              root,
                                                                              cb),
                                                                          AbstractRequestSpecifications.searchUpdate(
                                                                              root,
                                                                              cb,
                                                                              sessionOwner,
                                                                              session),
                                                                          AbstractRequestSpecifications.searchOAISDeletionCreator(
                                                                              root,
                                                                              cb)));

            predicates.add(AbstractRequestSpecifications.getRunningRequestFilter(root, cb));

            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

    public static Specification<AbstractRequest> searchRequestBlockingAIPPostProcess(Optional<String> sessionOwner,
                                                                                     Optional<String> session) {
        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();

            predicates.add(AbstractRequestSpecifications.aggregateRequest(cb,
                                                                          AbstractRequestSpecifications.searchUpdate(
                                                                              root,
                                                                              cb,
                                                                              sessionOwner,
                                                                              session),
                                                                          AbstractRequestSpecifications.searchOAISDeletion(
                                                                              root,
                                                                              cb,
                                                                              sessionOwner,
                                                                              session),
                                                                          AbstractRequestSpecifications.searchAipUpdatesCreator(
                                                                              root,
                                                                              cb),
                                                                          AbstractRequestSpecifications.searchOAISDeletionCreator(
                                                                              root,
                                                                              cb)));
            predicates.add(AbstractRequestSpecifications.getRunningRequestFilter(root, cb));

            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

    public static Predicate searchOAISDeletion(Root<AbstractRequest> root,
                                               CriteriaBuilder cb,
                                               Optional<String> sessionOwner,
                                               Optional<String> session) {
        return AbstractRequestSpecifications.searchMicroRequest(root,
                                                                cb,
                                                                sessionOwner,
                                                                session,
                                                                RequestTypeConstant.OAIS_DELETION_VALUE);
    }

    public static Predicate searchOAISDissemiation(Root<AbstractRequest> root,
                                                   CriteriaBuilder cb,
                                                   Optional<String> sessionOwner,
                                                   Optional<String> session) {
        return AbstractRequestSpecifications.searchMicroRequest(root,
                                                                cb,
                                                                sessionOwner,
                                                                session,
                                                                RequestTypeConstant.AIP_DISSEMINATION_VALUE);
    }

    public static Predicate searchIngest(Root<AbstractRequest> root,
                                         CriteriaBuilder cb,
                                         Optional<String> sessionOwner,
                                         Optional<String> session) {
        return AbstractRequestSpecifications.searchMicroRequest(root,
                                                                cb,
                                                                sessionOwner,
                                                                session,
                                                                RequestTypeConstant.INGEST_VALUE);
    }

    public static Predicate searchUpdate(Root<AbstractRequest> root,
                                         CriteriaBuilder cb,
                                         Optional<String> sessionOwner,
                                         Optional<String> session) {
        return AbstractRequestSpecifications.searchMicroRequest(root,
                                                                cb,
                                                                sessionOwner,
                                                                session,
                                                                RequestTypeConstant.UPDATE_VALUE);
    }

    public static Predicate searchPostProcess(Root<AbstractRequest> root,
                                              CriteriaBuilder cb,
                                              Optional<String> sessionOwner,
                                              Optional<String> session) {
        return AbstractRequestSpecifications.searchMicroRequest(root,
                                                                cb,
                                                                sessionOwner,
                                                                session,
                                                                RequestTypeConstant.AIP_POST_PROCESS_VALUE);
    }

    public static Predicate searchOAISDeletionCreator(Root<AbstractRequest> root, CriteriaBuilder cb) {
        return AbstractRequestSpecifications.searchMacroRequest(root,
                                                                cb,
                                                                RequestTypeConstant.OAIS_DELETION_CREATOR_VALUE);
    }

    public static Predicate searchAipUpdatesCreator(Root<AbstractRequest> root, CriteriaBuilder cb) {
        return AbstractRequestSpecifications.searchMacroRequest(root,
                                                                cb,
                                                                RequestTypeConstant.AIP_UPDATES_CREATOR_VALUE);
    }

    public static Predicate searchAipDisseminationCreator(Root<AbstractRequest> root, CriteriaBuilder cb) {
        return AbstractRequestSpecifications.searchMacroRequest(root,
                                                                cb,
                                                                RequestTypeConstant.AIP_DISSEMINATION_CREATOR_VALUE);
    }

    public static Predicate searchMicroRequest(Root<AbstractRequest> root,
                                               CriteriaBuilder cb,
                                               Optional<String> sessionOwner,
                                               Optional<String> session,
                                               String requestType) {
        Set<Predicate> predicates = Sets.newHashSet();
        if (sessionOwner.isPresent()) {
            predicates.add(cb.equal(root.get("sessionOwner"), sessionOwner.get()));
        }
        if (session.isPresent()) {
            predicates.add(cb.equal(root.get("session"), session.get()));
        }
        predicates.add(cb.equal(root.get(DISCRIMINANT_ATTRIBUTE), requestType));
        return cb.and(predicates.toArray(new Predicate[predicates.size()]));
    }

    public static Predicate searchMacroRequest(Root<AbstractRequest> root, CriteriaBuilder cb, String requestType) {
        return cb.and(cb.equal(root.get(DISCRIMINANT_ATTRIBUTE), requestType));
    }

    public static Predicate getRunningRequestFilter(Root<AbstractRequest> root, CriteriaBuilder cb) {
        Set<Predicate> statePredicates = Sets.newHashSet();
        ArrayList<InternalRequestState> runningStates = Lists.newArrayList(InternalRequestState.CREATED,
                                                                           InternalRequestState.RUNNING,
                                                                           InternalRequestState.WAITING_NOTIFIER_RESPONSE,
                                                                           InternalRequestState.WAITING_REMOTE_STORAGE);
        for (InternalRequestState state : runningStates) {
            statePredicates.add(cb.equal(root.get(STATE_ATTRIBUTE), state));
        }
        // Also add the limit
        // Use the OR operator between each state
        return cb.or(statePredicates.toArray(new Predicate[statePredicates.size()]));
    }

    public static Predicate aggregateRequest(CriteriaBuilder cb, Predicate... predicates) {
        // Use the OR operator between each state
        return cb.or(predicates);
    }

    public static Specification<AbstractRequest> searchRequestBlockingAipDisseminationCreator() {
        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();
            predicates.add(AbstractRequestSpecifications.aggregateRequest(cb,
                                                                          AbstractRequestSpecifications.searchAipDisseminationCreator(
                                                                              root,
                                                                              cb),
                                                                          AbstractRequestSpecifications.searchOAISDeletionCreator(
                                                                              root,
                                                                              cb),
                                                                          AbstractRequestSpecifications.searchAipUpdatesCreator(
                                                                              root,
                                                                              cb),
                                                                          AbstractRequestSpecifications.searchUpdate(
                                                                              root,
                                                                              cb,
                                                                              Optional.empty(),
                                                                              Optional.empty()),
                                                                          AbstractRequestSpecifications.searchOAISDeletion(
                                                                              root,
                                                                              cb,
                                                                              Optional.empty(),
                                                                              Optional.empty())));

            predicates.add(AbstractRequestSpecifications.getRunningRequestFilter(root, cb));

            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

    public static Specification<AbstractRequest> searchRequestBlockingAipDissemination(Optional<String> sessionOwnerOp,
                                                                                       Optional<String> sessionOp) {
        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();
            predicates.add(AbstractRequestSpecifications.aggregateRequest(cb,
                                                                          AbstractRequestSpecifications.searchUpdate(
                                                                              root,
                                                                              cb,
                                                                              sessionOwnerOp,
                                                                              sessionOp),
                                                                          AbstractRequestSpecifications.searchOAISDeletion(
                                                                              root,
                                                                              cb,
                                                                              sessionOwnerOp,
                                                                              sessionOp)));

            predicates.add(AbstractRequestSpecifications.getRunningRequestFilter(root, cb));

            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }
}
