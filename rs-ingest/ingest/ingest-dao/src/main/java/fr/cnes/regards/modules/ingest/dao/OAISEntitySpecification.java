/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see <https://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.ingest.dao;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.utils.SpecificationUtils;
import fr.cnes.regards.framework.urn.EntityType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;

/**
 * Specification class to filter on common attributes shared by SIP and AIP
 *
 * @author Léo Mieulet
 */
public final class OAISEntitySpecification {

    private OAISEntitySpecification() {
        throw new IllegalStateException("Utility class");
    }

    public static Set<Predicate> buildCommonPredicate(Root<?> root,
                                                      CriteriaBuilder cb,
                                                      List<String> tags,
                                                      String sessionOwner,
                                                      String session,
                                                      EntityType ipType,
                                                      Set<String> providerIds,
                                                      Set<String> categories) {

        Set<Predicate> predicates = Sets.newHashSet();
        if ((tags != null) && !tags.isEmpty()) {
            Path<Object> attributeRequested = root.get("tags");
            predicates.add(SpecificationUtils.buildPredicateIsJsonbArrayContainingOneOfElement(attributeRequested,
                                                                                               tags,
                                                                                               cb));
        }
        if (sessionOwner != null) {
            predicates.add(cb.like(root.get("sessionOwner"), sessionOwner));
        }
        if (session != null) {
            predicates.add(cb.like(root.get("session"), session));
        }
        if (ipType != null) {
            predicates.add(cb.equal(root.get("ipType"), ipType));
        }
        if ((providerIds != null) && !providerIds.isEmpty()) {
            Set<Predicate> providerIdsPredicates = Sets.newHashSet();
            for (String providerId : providerIds) {
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
        if (!CollectionUtils.isEmpty(categories)) {
            predicates.add(root.get("category").in(categories));
        }
        return predicates;
    }
}
