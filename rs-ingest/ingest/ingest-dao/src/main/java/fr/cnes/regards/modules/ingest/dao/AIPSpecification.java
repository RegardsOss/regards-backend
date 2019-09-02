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
import fr.cnes.regards.framework.jpa.utils.CustomPostgresDialect;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

/**
 * Specification class to filter DAO searches on {@link AIPEntity} entities
 * @author Léo Mieulet
 */
public class AIPSpecification {

    private static final String LIKE_CHAR = "%";

    public static Specification<AIPEntity> searchAll(AIPState state, OffsetDateTime from, OffsetDateTime to,
            List<String> tags, String sessionOwner, String session, String providerId, List<String> storages,
            List<String> categories) {
        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();
            if (tags != null && !tags.isEmpty()) {
                Path<Object> attributeRequeted = root.get("tags");
                predicates.add(buildPredicateIsJsonbArrayContainingArray(tags, attributeRequeted, cb));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("lastUpdate"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("lastUpdate"), to));
            }
            if (state != null) {
                predicates.add(cb.equal(root.get("state"), state));
            }
            if (sessionOwner != null) {
                predicates.add(cb.equal(root.get("ingestMetadata").get("sessionOwner"), sessionOwner));
            }
            if (session != null) {
                predicates.add(cb.equal(root.get("ingestMetadata").get("session"), session));
            }
            if (providerId != null) {
                if (providerId.startsWith(LIKE_CHAR) || providerId.endsWith(LIKE_CHAR)) {
                    predicates.add(cb.like(root.get("providerId"), providerId));
                } else {
                    predicates.add(cb.equal(root.get("providerId"), providerId));
                }
            }
            if (storages != null && !storages.isEmpty()) {
                Set<Predicate> storagePredicates = Sets.newHashSet();
                for (String storage: storages) {
                    storagePredicates.add(cb.isTrue(
                            cb.function(CustomPostgresDialect.JSONB_CONTAINS,
                                    Boolean.class,
                                    root.get("ingestMetadata").get("storages"),
                                    cb.function(
                                            CustomPostgresDialect.JSONB_LITERAL,
                                            String.class,
                                            cb.literal("[{\"storage\": \"" + storage + "\"}]")
                                    )
                            )
                    ));
                }
                // Use the OR operator between each storage
                predicates.add(cb.or(storagePredicates.toArray(new Predicate[storagePredicates.size()])));
            }
            if (categories != null && !categories.isEmpty()) {
                Path<Object> attributeRequeted = root.get("categories");
                predicates.add(buildPredicateIsJsonbArrayContainingArray(categories, attributeRequeted, cb));
            }

            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

    /**
     * Return a predicate that check if a JSONB string array (ie ["a", "b"]) contains all textSearched items
     * @param textSearched
     * @param attributeRequested
     * @param cb
     * @return
     */
    private static Predicate buildPredicateIsJsonbArrayContainingArray(List<String> textSearched, Path<Object> attributeRequested, CriteriaBuilder cb) {
        // Create an empty array
        Expression<List> allowedValuesContraint = cb.function(CustomPostgresDialect.EMPTY_STRING_ARRAY, List.class);
        for (String category : textSearched) {
            // Append to that array every text researched
            allowedValuesContraint = cb.function("array_append", List.class,
                    allowedValuesContraint,
                    cb.function(CustomPostgresDialect.STRING_LITERAL, String.class, cb.literal(category))
            );
        }
        // Check the entity have every text researched
        return cb.isTrue(cb.function(CustomPostgresDialect.JSONB_EXISTS_ALL, Boolean.class, attributeRequested,
                allowedValuesContraint
        ));
    }

}