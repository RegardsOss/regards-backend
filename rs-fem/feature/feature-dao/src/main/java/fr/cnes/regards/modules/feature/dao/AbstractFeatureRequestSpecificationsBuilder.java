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
import fr.cnes.regards.framework.jpa.restriction.ValuesRestriction;
import fr.cnes.regards.framework.jpa.restriction.ValuesRestrictionMode;
import fr.cnes.regards.framework.jpa.utils.AbstractSearchParameters;
import fr.cnes.regards.framework.jpa.utils.AbstractSpecificationsBuilder;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.Collection;
import java.util.Set;

/**
 * Builder for specifications with all classes extended AbstractFeatureRequest
 *
 * @author Stephane Cortine
 */
public abstract class AbstractFeatureRequestSpecificationsBuilder<T, R extends AbstractSearchParameters<T>>
    extends AbstractSpecificationsBuilder<T, R> {

    protected Specification<T> equalsWithFeatureEntity(String pathToField, @Nullable String value) {
        return (root, query, criteriaBuilder) -> {
            if (value == null) {
                return null;
            }
            Root<FeatureEntity> fr = query.from(FeatureEntity.class);

            Set<Predicate> predicates = Sets.newHashSet();
            predicates.add(criteriaBuilder.equal(fr.get("urn"), root.get("urn")));
            predicates.add(criteriaBuilder.equal(fr.get(pathToField), value));

            return criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()]));//criteriaBuilder.equal(getPath(root, pathToField), value);
        };
    }

    protected Specification<T> useValuesRestrictionLike(String pathToField,
                                                        @Nullable ValuesRestriction<String> valuesRestriction) {
        if (valuesRestriction == null) {
            return null;
        }
        Collection<String> values = valuesRestriction.getValues();
        Assert.notEmpty(values, "Values must not be empty");

        if (valuesRestriction.getMode() == ValuesRestrictionMode.INCLUDE) {
            return isIncludedString(pathToField, values);
        }
        return isExcludedString(pathToField, values);
    }

    protected Specification<T> isIncludedString(String pathToField, Collection<String> values) {
        Assert.notNull(values, "Values must not be null");
        Assert.notEmpty(values, "Values must not be empty");

        return (root, query, criteriaBuilder) -> {
            Set<Predicate> predicates = Sets.newHashSet();
            for (String value : values) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get(pathToField)),
                                                    value.toLowerCase() + "%"));
            }
            return criteriaBuilder.or(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

    protected Specification<T> isExcludedString(String pathToField, Collection<String> values) {
        Assert.notNull(values, "Values must not be null");
        Assert.notEmpty(values, "Values must not be empty");

        return (root, query, criteriaBuilder) -> {
            Set<Predicate> predicates = Sets.newHashSet();
            for (String value : values) {
                predicates.add(criteriaBuilder.notLike(criteriaBuilder.lower(root.get(pathToField)),
                                                       value.toLowerCase() + "%"));
            }
            return criteriaBuilder.or(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

    protected Specification<T> useValuesRestrictionLikeWithFeatureEntity(String pathToField,
                                                                         @Nullable
                                                                         ValuesRestriction<String> valuesRestriction) {
        if (valuesRestriction == null) {
            return null;
        }
        Collection<String> values = valuesRestriction.getValues();
        Assert.notEmpty(values, "Values must not be empty");

        if (valuesRestriction.getMode() == ValuesRestrictionMode.INCLUDE) {
            return isIncludedStringWithFeatureEntity(pathToField, values);
        }
        return isExcludedStringWithFeatureEntity(pathToField, values);
    }

    protected Specification<T> isIncludedStringWithFeatureEntity(String pathToField, Collection<String> values) {
        Assert.notNull(values, "Values must not be null");
        Assert.notEmpty(values, "Values must not be empty");

        return (root, query, criteriaBuilder) -> {
            Set<Predicate> predicates = Sets.newHashSet();
            Root<FeatureEntity> fr = query.from(FeatureEntity.class);
            predicates.add(criteriaBuilder.equal(fr.get("urn"), root.get("urn")));

            Set<Predicate> likePredicates = Sets.newHashSet();
            for (String value : values) {
                likePredicates.add(criteriaBuilder.like(criteriaBuilder.lower(fr.get(pathToField)),
                                                        value.toLowerCase() + "%"));
            }
            predicates.add(criteriaBuilder.or(likePredicates.toArray(new Predicate[likePredicates.size()])));

            return criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

    protected Specification<T> isExcludedStringWithFeatureEntity(String pathToField, Collection<String> values) {
        Assert.notNull(values, "Values must not be null");
        Assert.notEmpty(values, "Values must not be empty");

        return (root, query, criteriaBuilder) -> {
            Set<Predicate> predicates = Sets.newHashSet();
            Root<FeatureEntity> fr = query.from(FeatureEntity.class);
            predicates.add(criteriaBuilder.equal(fr.get("urn"), root.get("urn")));

            Set<Predicate> notLikePredicates = Sets.newHashSet();
            for (String value : values) {
                notLikePredicates.add(criteriaBuilder.notLike(criteriaBuilder.lower(fr.get(pathToField)),
                                                              value.toLowerCase() + "%"));
            }
            predicates.add(criteriaBuilder.or(notLikePredicates.toArray(new Predicate[notLikePredicates.size()])));

            return criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }
}
