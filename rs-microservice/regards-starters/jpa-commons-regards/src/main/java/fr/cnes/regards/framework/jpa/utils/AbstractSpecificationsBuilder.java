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
import fr.cnes.regards.framework.jpa.restriction.ValuesRestrictionMode;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.annotation.Nullable;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class AbstractSpecificationsBuilder<T, R extends AbstractSearchParameters<T>> {

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
                                                                          "%" + value + "%");
        }
    }

    @SuppressWarnings("unchecked")
    protected Specification<T> likeIgnoreCase(String pathToField, @Nullable String value) {
        if (!StringUtils.hasLength(value)) {
            return null;
        } else {
            return (root, query, criteriaBuilder) -> criteriaBuilder.like(criteriaBuilder.upper((Expression<String>) getPath(
                root,
                pathToField)), ("%" + value + "%").toUpperCase());
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
        Collection<?> values = valuesRestriction.getValues();
        Assert.notEmpty(values, "Values must not be empty");
        if (valuesRestriction.getMode() == ValuesRestrictionMode.INCLUDE) {
            return isIncluded(pathToField, values);
        }
        return isExcluded(pathToField, values);
    }

    protected Specification<T> isIncluded(String pathToField, Collection<?> values) {
        Assert.notNull(values, "Values must not be null");
        Assert.notEmpty(values, "Values must not be empty");

        return (root, query, criteriaBuilder) -> getPath(root, pathToField).in(values);
    }

    protected Specification<T> isExcluded(String pathToField, Collection<?> values) {
        Assert.notNull(values, "Values must not be null");
        Assert.notEmpty(values, "Values must not be empty");

        return (root, query, criteriaBuilder) -> getPath(root, pathToField).in(values).not();
    }

    protected Specification<T> useValuesRestrictionJoined(String join,
                                                          String pathToField,
                                                          @Nullable ValuesRestriction<?> valuesRestriction) {
        if (valuesRestriction == null) {
            return null;
        }
        Collection<?> values = valuesRestriction.getValues();
        Assert.notEmpty(values, "Values must not be empty");

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
        Assert.notEmpty(values, "Values must not be empty");

        return (root, query, criteriaBuilder) -> root.join(join).get(pathToField).in(values).not();
    }

    protected Specification<T> useValuesRestrictionJoinSet(String pathToField,
                                                           @Nullable ValuesRestriction<?> valuesRestriction) {
        if (valuesRestriction == null) {
            return null;
        }
        Collection<?> values = valuesRestriction.getValues();
        Assert.notEmpty(values, "Values must not be empty");

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
        Assert.notEmpty(values, "Values must not be empty");

        return (root, query, criteriaBuilder) -> root.joinSet(pathToField).in(values).not();
    }

    private Path<T> getPath(Root<T> root, String attributeName) {
        Path<T> path = root;
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

}
