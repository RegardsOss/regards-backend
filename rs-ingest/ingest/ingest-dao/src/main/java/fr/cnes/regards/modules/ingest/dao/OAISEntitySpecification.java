package fr.cnes.regards.modules.ingest.dao;


import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.utils.CustomPostgresDialect;
import fr.cnes.regards.framework.jpa.utils.SpecificationUtils;
import java.util.List;
import java.util.Set;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/**
 * Specification class to filter on common attributes shared by SIP and AIP
 * @author Léo Mieulet
 */
public final class OAISEntitySpecification {

    private static final String INGEST_METADATA = "ingestMetadata";

    private OAISEntitySpecification() {
        throw new IllegalStateException("Utility class");
    }

    public static Set<Predicate> buildCommonPredicate(Root<?> root, CriteriaBuilder cb,List<String> tags,
            String sessionOwner, String session, String providerId, List<String> storages, List<String> categories) {

        Set<Predicate> predicates = Sets.newHashSet();
        if (tags != null && !tags.isEmpty()) {
            Path<Object> attributeRequeted = root.get("tags");
            predicates.add(SpecificationUtils.buildPredicateIsJsonbArrayContainingElements(attributeRequeted, tags, cb));
        }
        if (sessionOwner != null) {
            predicates.add(cb.equal(root.get(INGEST_METADATA).get("sessionOwner"), sessionOwner));
        }
        if (session != null) {
            predicates.add(cb.equal(root.get(INGEST_METADATA).get("session"), session));
        }
        if (providerId != null) {
            if (providerId.startsWith(SpecificationUtils.LIKE_CHAR) || providerId.endsWith(SpecificationUtils.LIKE_CHAR)) {
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
                                root.get(INGEST_METADATA).get("storages"),
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
            Path<Object> attributeRequeted = root.get(INGEST_METADATA).get("categories");
            predicates.add(SpecificationUtils.buildPredicateIsJsonbArrayContainingElements(attributeRequeted, categories, cb));
        }
        return predicates;
    }
}
