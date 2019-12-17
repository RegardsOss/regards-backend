package fr.cnes.regards.modules.ingest.dao;

import fr.cnes.regards.framework.oais.urn.EntityType;
import java.util.List;
import java.util.Set;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.utils.SpecificationUtils;

/**
 * Specification class to filter on common attributes shared by SIP and AIP
 * @author Léo Mieulet
 */
public final class OAISEntitySpecification {

    private OAISEntitySpecification() {
        throw new IllegalStateException("Utility class");
    }

    public static Set<Predicate> buildCommonPredicate(Root<?> root, CriteriaBuilder cb, List<String> tags,
            String sessionOwner, String session, EntityType ipType, Set<String> providerIds, Set<String> categories) {

        Set<Predicate> predicates = Sets.newHashSet();
        if ((tags != null) && !tags.isEmpty()) {
            Path<Object> attributeRequested = root.get("tags");
            predicates.add(SpecificationUtils.buildPredicateIsJsonbArrayContainingOneOfElement(attributeRequested, tags,
                                                                                               cb));
        }
        if (sessionOwner != null) {
            predicates.add(cb.equal(root.get("sessionOwner"), sessionOwner));
        }
        if (session != null) {
            predicates.add(cb.equal(root.get("session"), session));
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
        if ((categories != null) && !categories.isEmpty()) {
            Path<Object> attributeRequeted = root.get("categories");
            predicates.add(SpecificationUtils
                    .buildPredicateIsJsonbArrayContainingOneOfElement(attributeRequeted, Lists.newArrayList(categories),
                                                                      cb));
        }
        return predicates;
    }
}
