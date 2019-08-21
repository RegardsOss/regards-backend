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

import com.google.common.collect.Sets;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import javax.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

/**
 * JPA {@link Specification} to define {@link Predicate}s for criteria search for {@link SIPEntity} from repository.
 * @author Sébastien Binda
 */
public final class SIPEntitySpecifications {

    private static final String LIKE_CHAR = "%";

    private SIPEntitySpecifications() {
    }

    /**
     * Filter on the given attributes and return result ordered by descending
     * ingestDate
     * @param providerIds list of providerId to keep or ignore (depends on areIdListInclusive)
     * @param sipIds list of sipId to keep or ignore (depends on areIdListInclusive)
     * @param sessionOwner
     * @param session
     * @param from
     * @param states list of states
     * @param ingestChain name of the chain
     * @param areIdListInclusive true when sipIds and providerIds should be include in the request, otherwise these
     *                         lists would rather remove entries from returning result
     */
    public static Specification<SIPEntity> search(List<String> providerIds, List<String> sipIds, String sessionOwner, String session,
            OffsetDateTime from, List<SIPState> states, String ingestChain, boolean areIdListInclusive) {
        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();
            if (sessionOwner != null) {
                predicates.add(cb.equal(root.get("ingestMetadata").get("sessionOwner"), sessionOwner));
            }
            if (session != null) {
                predicates.add(cb.equal(root.get("ingestMetadata").get("session"), session));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("ingestDate"), from));
            }
            if (states != null && !states.isEmpty()) {
                Set<Predicate> statePredicates = Sets.newHashSet();
                for (SIPState state : states) {
                    statePredicates.add(cb.equal(root.get("state"), state));
                }
                predicates.add(cb.or(statePredicates.toArray(new Predicate[statePredicates.size()])));
            }
            if (providerIds != null && !providerIds.isEmpty()) {
                Set<Predicate> providerIdPredicates = Sets.newHashSet();
                for (String providerId : providerIds) {
                    // Use the like operator only if the providerId contains a % directly in the chain
                    if (providerId.startsWith(LIKE_CHAR) || providerId.endsWith(LIKE_CHAR)) {
                        if (areIdListInclusive) {
                            providerIdPredicates.add(cb.like(root.get("providerId"), providerId));
                        } else {
                            providerIdPredicates.add(cb.notLike(root.get("providerId"), providerId));
                        }
                    } else {
                        if (areIdListInclusive) {
                            providerIdPredicates.add(cb.equal(root.get("providerId"), providerId));
                        } else {
                            providerIdPredicates.add(cb.notEqual(root.get("providerId"), providerId));
                        }
                    }
                }
                predicates.add(cb.or(providerIdPredicates.toArray(new Predicate[providerIdPredicates.size()])));
            }
            if (sipIds != null && !sipIds.isEmpty()) {
                Set<Predicate> sipIdsPredicates = Sets.newHashSet();
                for (String sipId: sipIds) {
                    if (areIdListInclusive) {
                        sipIdsPredicates.add(cb.equal(root.get("sipId"), sipId));
                    } else {
                        sipIdsPredicates.add(cb.notEqual(root.get("sipId"), sipId));
                    }
                }
                predicates.add(cb.or(sipIdsPredicates.toArray(new Predicate[sipIdsPredicates.size()])));
            }
            if (ingestChain != null) {
                predicates.add(cb.equal(root.get("ingestMetadata").get("ingestChain"), ingestChain));
            }
            query.orderBy(cb.desc(root.get("ingestDate")));
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

}
