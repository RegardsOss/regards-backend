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
package fr.cnes.regards.framework.jpa.utils;

import fr.cnes.regards.framework.jpa.restriction.DatesRangeRestriction;
import fr.cnes.regards.framework.jpa.restriction.ValuesRestriction;
import fr.cnes.regards.framework.jpa.restriction.ValuesRestrictionMatchMode;
import fr.cnes.regards.framework.jpa.restriction.ValuesRestrictionMode;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.annotation.Nullable;
import javax.persistence.criteria.*;
import java.security.InvalidParameterException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * Generic builder for specifications
 */
public abstract class AbstractSpecificationsBuilder<T, R extends AbstractSearchParameters> {

    protected List<Specification<T>> specifications = new ArrayList<>();

    protected R parameters;

    protected abstract void addSpecificationsFromParameters();

    public AbstractSpecificationsBuilder<T, R> withParameters(R parameters) {
        this.parameters = parameters;
        return this;
    }

    public Specification<T> build() {
        addSpecificationsFromParameters();
        return this.toSpecification();
    }

    protected Specification<T> equals(String pathToField, @Nullable Long value) {
        if (value == null) {
            return null;
        } else {
            return (root, query, criteriaBuilder) -> criteriaBuilder.equal(getPath(root, pathToField), value);
        }
    }

    protected Specification<T> equals(String pathToField, @Nullable String value) {
        if (!StringUtils.hasLength(value)) {
            return null;
        } else {
            return (root, query, criteriaBuilder) -> criteriaBuilder.equal(getPath(root, pathToField), value);

        }
    }

    @SuppressWarnings("unchecked")
    protected Specification<T> equalsIgnoreCase(String pathToField, @Nullable String value) {
        if (!StringUtils.hasLength(value)) {
            return null;
        } else {
            return (root, query, criteriaBuilder) -> criteriaBuilder.equal(criteriaBuilder.upper((Expression<String>) getPath(
                root,
                pathToField)), value.toUpperCase());
        }
    }

    protected Specification<T> equals(String pathToField, @Nullable Enum<?> value) {
        if (value == null) {
            return null;
        } else {
            return (root, query, criteriaBuilder) -> criteriaBuilder.equal(getPath(root, pathToField), value);
        }
    }

    protected Specification<T> equals(String pathToField, @Nullable Boolean value) {
        if (value == null) {
            return null;
        } else {
            return (root, query, criteriaBuilder) -> criteriaBuilder.equal(getPath(root, pathToField), value);
        }
    }

    protected Specification<T> notEquals(String pathToField, @Nullable Enum<?> value) {
        if (value == null) {
            return null;
        } else {
            return (root, query, criteriaBuilder) -> criteriaBuilder.notEqual(getPath(root, pathToField), value);
        }
    }

    @SuppressWarnings("unchecked")
    protected Specification<T> like(String pathToField, @Nullable String value) {
        if (!StringUtils.hasLength(value)) {
            return null;
        } else {
            return (root, query, criteriaBuilder) -> criteriaBuilder.like((Expression<String>) getPath(root,
                                                                                                       pathToField),
                                                                          "%"
                                                                          + replacePostgresSpecialCharacters(value)
                                                                          + "%");
        }
    }

    @SuppressWarnings("unchecked")
    protected Specification<T> likeIgnoreCase(String pathToField, @Nullable String value) {
        if (!StringUtils.hasLength(value)) {
            return null;
        } else {
            return (root, query, criteriaBuilder) -> criteriaBuilder.like(criteriaBuilder.upper((Expression<String>) getPath(
                root,
                pathToField)), ("%" + replacePostgresSpecialCharacters(value) + "%").toUpperCase());
        }
    }

    @SuppressWarnings("unchecked")
    protected Specification<T> useDatesRestriction(String pathToField,
                                                   @Nullable DatesRangeRestriction datesRangeRestriction) {
        if (datesRangeRestriction == null) {
            return null;
        }
        OffsetDateTime dateAfter = datesRangeRestriction.getAfter();
        OffsetDateTime dateBefore = datesRangeRestriction.getBefore();
        if (dateAfter == null && dateBefore == null) {
            return null;
        }
        if (dateAfter == null) {
            return before(pathToField, dateBefore);
        }
        if (dateBefore == null) {
            return after(pathToField, dateAfter);
        }
        return ((root, query, criteriaBuilder) -> criteriaBuilder.between((Expression<OffsetDateTime>) getPath(root,
                                                                                                               pathToField),
                                                                          dateAfter,
                                                                          dateBefore));
    }

    @SuppressWarnings("unchecked")
    protected Specification<T> before(String pathToField, @Nullable OffsetDateTime date) {
        if (date == null) {
            return null;
        } else {
            return ((root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo((Expression<OffsetDateTime>) getPath(
                root,
                pathToField), date));
        }
    }

    @SuppressWarnings("unchecked")
    protected Specification<T> after(String pathToField, @Nullable OffsetDateTime date) {
        if (date == null) {
            return null;
        } else {
            return ((root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo((Expression<OffsetDateTime>) getPath(
                root,
                pathToField), date));
        }
    }

    protected Specification<T> joinedEquals(String join, String pathToField, @Nullable String value) {
        if (!StringUtils.hasLength(value)) {
            return null;
        } else {
            return ((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.join(join).get(pathToField), value));
        }
    }

    @SuppressWarnings("unchecked")
    protected Specification<T> isMember(String pathToField, @Nullable String value) {
        if (!StringUtils.hasLength(value)) {
            return null;
        } else {
            return (root, query, criteriaBuilder) -> criteriaBuilder.isMember(value,
                                                                              (Expression<Collection<Object>>) getPath(
                                                                                  root,
                                                                                  pathToField));
        }
    }

    protected Specification<T> isMember(String pathToField, @Nullable Collection<String> values) {
        if (values == null) {
            return null;
        } else {
            Assert.notEmpty(values, "Values must not be empty");
            return (root, query, criteriaBuilder) -> root.joinSet(pathToField).in(values);
        }
    }

    protected Specification<T> useValuesRestriction(String pathToField,
                                                    @Nullable ValuesRestriction<?> valuesRestriction) {
        if (valuesRestriction == null) {
            return null;
        }

        if (valuesRestriction.getValues().isEmpty() && valuesRestriction.getMode() == ValuesRestrictionMode.INCLUDE) {
            throw new InvalidParameterException(String.format(
                "Invalid search value restriction on %s. Forbidden empty list of values with include mode.",
                pathToField));
        }

        return (root, query, cb) -> createValuesRestrictionPredicate(root, cb, pathToField, valuesRestriction);
    }

    /**
     * Create specification by mapping {@link ValuesRestriction} values enum type with enum::name String values.
     */
    protected Specification<T> useValuesRestrictionEnumAsString(String pathToField,
                                                                @Nullable
                                                                ValuesRestriction<? extends Enum> valuesRestriction) {
        if (valuesRestriction == null) {
            return null;
        }
        Assert.notEmpty(valuesRestriction.getValues(), "Values must not be empty");

        return (root, query, cb) -> {
            Collection<Predicate> predicates = valuesRestriction.getValues()
                                                                .stream()
                                                                .map(value -> createIncludeValuesRestrictionPredicate(
                                                                    root,
                                                                    cb,
                                                                    pathToField,
                                                                    valuesRestriction.getMatchMode(),
                                                                    valuesRestriction.isIgnoreCase(),
                                                                    value.name()))
                                                                .toList();
            Predicate result = cb.or(predicates.toArray(Predicate[]::new));
            if (valuesRestriction.getMode() == ValuesRestrictionMode.EXCLUDE) {
                result = result.not();
            }
            return result;
        };
    }

    /**
     * Creates {@link Predicate} for the given {@link ValuesRestriction} associated to the given path.
     */
    protected Predicate createValuesRestrictionPredicate(Root<?> root,
                                                         CriteriaBuilder cb,
                                                         String pathToField,
                                                         ValuesRestriction valuesRestriction) {
        Predicate[] predicates = createIncludeValuesRestrictionPredicates(root,
                                                                          cb,
                                                                          pathToField,
                                                                          valuesRestriction).toArray(Predicate[]::new);
        Predicate result = cb.or(predicates);
        if (valuesRestriction.getMode() == ValuesRestrictionMode.EXCLUDE) {
            result = result.not();
        }
        return result;
    }

    /**
     * Creates all individual {@link Predicate}s for the given {@link ValuesRestriction} associated to the given path.
     */
    private Collection<Predicate> createIncludeValuesRestrictionPredicates(Root<?> root,
                                                                           CriteriaBuilder cb,
                                                                           String pathToField,
                                                                           ValuesRestriction valuesRestriction) {
        Collection<Predicate> predicates = new HashSet<>();
        for (Object value : valuesRestriction.getValues()) {
            predicates.add(createIncludeValuesRestrictionPredicate(root,
                                                                   cb,
                                                                   pathToField,
                                                                   valuesRestriction.getMatchMode(),
                                                                   valuesRestriction.isIgnoreCase(),
                                                                   value));
        }
        return predicates;
    }

    /**
     * Creates one {@link Predicate} associated to the given db field path and search value.
     */
    private Predicate createIncludeValuesRestrictionPredicate(Root<?> root,
                                                              CriteriaBuilder cb,
                                                              String pathToField,
                                                              ValuesRestrictionMatchMode matchMode,
                                                              boolean ignoreCase,
                                                              Object value) {
        Object lValue = value;
        Expression expr = getPath(root, pathToField);
        // If given value is a string calculate value and expression with matchMode and ignore case option
        if (value instanceof String sValue) {
            lValue = getLikeStringExpression(matchMode, sValue, ignoreCase);
            if (ignoreCase) {
                expr = cb.lower(expr);
            }
        }
        if (matchMode == ValuesRestrictionMatchMode.STRICT) {
            return cb.equal(expr, lValue);
        } else {
            return cb.like(expr, lValue.toString());
        }
    }

    /**
     * Return postregres expression for value depending on matching mode.
     * can be :
     * <ul>
     *     <li>%value% for contains</li>
     *     <li>value% for starts with</li>
     *     <li>%value for ends with</li>
     * </ul>
     */
    public static String getLikeStringExpression(ValuesRestrictionMatchMode matchMode,
                                                 String value,
                                                 boolean ignoreCase) {
        String ignoreCaseValue = ignoreCase ? value.toLowerCase() : value;
        switch (matchMode) {
            case CONTAINS -> {
                return ("%" + replacePostgresSpecialCharacters(ignoreCaseValue) + "%");
            }
            case STARTS_WITH -> {
                return (replacePostgresSpecialCharacters(ignoreCaseValue) + "%");
            }
            case ENDS_WITH -> {
                return ("%" + replacePostgresSpecialCharacters(ignoreCaseValue));
            }
            default -> {
                return ignoreCaseValue;
            }
        }
    }

    protected Specification<T> useValuesRestrictionJoined(String join,
                                                          String pathToField,
                                                          @Nullable ValuesRestriction<?> valuesRestriction) {
        if (valuesRestriction == null) {
            return null;
        }
        Collection<?> values = valuesRestriction.getValues();
        if (valuesRestriction.getMode() == ValuesRestrictionMode.INCLUDE) {
            return isIncludedJoined(join, pathToField, values);
        }
        return isExcludedJoined(join, pathToField, values);
    }

    protected Specification<T> isIncludedJoined(String join, String pathToField, Collection<?> values) {
        Assert.notNull(values, "Values must not be null");
        Assert.notEmpty(values, "Values must not be empty");

        return (root, query, criteriaBuilder) -> root.join(join).get(pathToField).in(values);
    }

    protected Specification<T> isExcludedJoined(String join, String pathToField, Collection<?> values) {
        Assert.notNull(values, "Values must not be null");
        // The list can empty when the user ticks select all in the front
        return (root, query, criteriaBuilder) -> root.join(join).get(pathToField).in(values).not();
    }

    protected Specification<T> useValuesRestrictionJoinSet(String pathToField,
                                                           @Nullable ValuesRestriction<?> valuesRestriction) {
        if (valuesRestriction == null) {
            return null;
        }
        Collection<?> values = valuesRestriction.getValues();
        if (valuesRestriction.getMode() == ValuesRestrictionMode.INCLUDE) {
            return isIncludedJoinSet(pathToField, values);
        }
        return isExcludedJoinSet(pathToField, values);
    }

    protected Specification<T> isIncludedJoinSet(String pathToField, Collection<?> values) {
        Assert.notNull(values, "Values must not be null");
        Assert.notEmpty(values, "Values must not be empty");

        return (root, query, criteriaBuilder) -> root.joinSet(pathToField).in(values);
    }

    protected Specification<T> isExcludedJoinSet(String pathToField, Collection<?> values) {
        Assert.notNull(values, "Values must not be null");
        // The list can empty when the user ticks select all in the front
        return (root, query, criteriaBuilder) -> root.joinSet(pathToField).in(values).not();
    }

    protected Specification<T> isJsonbArrayContainingOneOfElement(String path,
                                                                  ValuesRestriction<String> valuesRestriction) {
        if (valuesRestriction == null) {
            return null;
        }
        return (root, query, criteriaBuilder) -> {
            Path<Object> attributeRequested = root.get(path);
            Expression<List> allowedValuesConstraint = criteriaBuilder.function(CustomPostgresDialect.EMPTY_STRING_ARRAY,
                                                                                List.class);
            for (String text : valuesRestriction.getValues()) {
                // Append to that array every text researched
                allowedValuesConstraint = criteriaBuilder.function("array_append",
                                                                   List.class,
                                                                   allowedValuesConstraint,
                                                                   criteriaBuilder.function(CustomPostgresDialect.STRING_LITERAL,
                                                                                            String.class,
                                                                                            criteriaBuilder.literal(text)));
            }
            // Check the entity have every text researched
            return criteriaBuilder.isTrue(criteriaBuilder.function(CustomPostgresDialect.JSONB_EXISTS_ANY,
                                                                   Boolean.class,
                                                                   attributeRequested,
                                                                   allowedValuesConstraint));
        };
    }

    private Path<?> getPath(Root<?> root, String attributeName) {
        Path<?> path = root;
        for (String part : attributeName.split("\\.")) {
            path = path.get(part);
        }
        return path;
    }

    protected Specification<T> toSpecification() {
        Specification<T> result = null;
        for (Specification<T> specification : specifications) {
            if (specification != null) {
                if (result == null) {
                    result = Specification.where(specification);
                } else {
                    result = result.and(specification);
                }
            }
        }
        return result;
    }

    /**
     * Utility method to escape all postgres special characters for a like expression search.
     */
    public static String replacePostgresSpecialCharacters(String value) {
        return value.replace("_", "\\_");
    }

}
