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
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

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

    protected Specification<T> equals(String pathToField, Long value) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(getPath(root, pathToField), value);
    }

    protected Specification<T> equals(String pathToField, String value) {
        if (!StringUtils.hasLength(value)) {
            return null;
        } else {
            return (root, query, criteriaBuilder) -> criteriaBuilder.equal(getPath(root, pathToField), value);

        }
    }

    @SuppressWarnings("unchecked")
    protected Specification<T> equalsIgnoreCase(String pathToField, String value) {
        if (!StringUtils.hasLength(value)) {
            return null;
        } else {
            return (root, query, criteriaBuilder) -> criteriaBuilder.equal(criteriaBuilder.upper((Expression<String>) getPath(
                root,
                pathToField)), value.toUpperCase());
        }
    }

    protected Specification<T> equals(String pathToField, Enum<?> value) {
        if (value == null) {
            return null;
        } else {
            return (root, query, criteriaBuilder) -> criteriaBuilder.equal(getPath(root, pathToField), value);
        }
    }

    protected Specification<T> equals(String pathToField, Boolean value) {
        if (value == null) {
            return null;
        } else {
            return (root, query, criteriaBuilder) -> criteriaBuilder.equal(getPath(root, pathToField), value);
        }
    }

    protected Specification<T> notEquals(String pathToField, Enum<?> value) {
        if (value == null) {
            return null;
        } else {
            return (root, query, criteriaBuilder) -> criteriaBuilder.notEqual(getPath(root, pathToField), value);
        }
    }

    @SuppressWarnings("unchecked")
    protected Specification<T> like(String pathToField, String value) {
        if (!StringUtils.hasLength(value)) {
            return null;
        } else {
            return (root, query, criteriaBuilder) -> criteriaBuilder.like((Expression<String>) getPath(root,
                                                                                                       pathToField),
                                                                          "%" + value + "%");
        }
    }

    @SuppressWarnings("unchecked")
    protected Specification<T> likeIgnoreCase(String pathToField, String value) {
        if (!StringUtils.hasLength(value)) {
            return null;
        } else {
            return (root, query, criteriaBuilder) -> criteriaBuilder.like(criteriaBuilder.upper((Expression<String>) getPath(
                root,
                pathToField)), ("%" + value + "%").toUpperCase());
        }
    }

    @SuppressWarnings("unchecked")
    protected Specification<T> useDatesRestriction(String pathToField, DatesRangeRestriction datesRangeRestriction) {
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
    protected Specification<T> before(String pathToField, OffsetDateTime date) {
        if (date == null) {
            return null;
        } else {
            return ((root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo((Expression<OffsetDateTime>) getPath(
                root,
                pathToField), date));
        }
    }

    @SuppressWarnings("unchecked")
    protected Specification<T> after(String pathToField, OffsetDateTime date) {
        if (date == null) {
            return null;
        } else {
            return ((root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo((Expression<OffsetDateTime>) getPath(
                root,
                pathToField), date));
        }
    }

    protected Specification<T> joinedEquals(String join, String pathToField, String value) {
        if (!StringUtils.hasLength(value)) {
            return null;
        } else {
            return ((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.join(join).get(pathToField), value));
        }
    }

    @SuppressWarnings("unchecked")
    protected Specification<T> isMember(String pathToField, String value) {
        if (!StringUtils.hasLength(value)) {
            return null;
        } else {
            return ((root, query, criteriaBuilder) -> criteriaBuilder.isMember(value,
                                                                               (Expression<Collection<Object>>) getPath(
                                                                                   root,
                                                                                   pathToField)));
        }
    }

    protected Specification<T> useValuesRestriction(String pathToField, ValuesRestriction<?> valuesRestriction) {
        if (valuesRestriction == null) {
            return null;
        }
        Collection<?> values = valuesRestriction.getValues();
        if (CollectionUtils.isEmpty(values)) {
            return null;
        }
        if (valuesRestriction.getMode() == ValuesRestrictionMode.INCLUDE) {
            return isIncluded(pathToField, values);
        }
        return isExcluded(pathToField, values);
    }

    protected Specification<T> isIncluded(String pathToField, Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return null;
        } else {
            return (root, query, criteriaBuilder) -> getPath(root, pathToField).in(values);
        }
    }

    protected Specification<T> isExcluded(String pathToField, Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return null;
        } else {
            return (root, query, criteriaBuilder) -> getPath(root, pathToField).in(values).not();
        }
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
