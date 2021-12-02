/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import fr.cnes.regards.framework.jpa.restriction.DatesRangeRestriction;
import fr.cnes.regards.framework.jpa.restriction.ValuesRestriction;
import fr.cnes.regards.framework.jpa.restriction.ValuesRestrictionMode;

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

    protected Specification<T> equals(String field, Long value) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(field), value);
    }

    protected Specification<T> equals(String field, String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        } else {
            return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(field), value);
        }
    }

    protected Specification<T> equalsIgnoreCase(String field, String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        } else {
            return (root, query, criteriaBuilder) -> criteriaBuilder.equal(criteriaBuilder.upper(root.get(field)), value.toUpperCase());
        }
    }

    protected Specification<T> equals(String field, Enum<?> value) {
        if (value == null) {
            return null;
        } else {
            return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(field), value);
        }
    }

    protected Specification<T> equals(String field, Boolean value) {
        if (value == null) {
            return null;
        } else {
            return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(field), value);
        }
    }

    protected Specification<T> like(String field, String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        } else {
            return (root, query, criteriaBuilder) -> criteriaBuilder.like(root.get(field), "%" + value + "%");
        }
    }

    protected Specification<T> likeIgnoreCase(String field, String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        } else {
            return (root, query, criteriaBuilder) -> criteriaBuilder.like(criteriaBuilder.upper(root.get(field)), ("%" + value + "%").toUpperCase());
        }
    }

    protected Specification<T> useDatesRestriction(String field, DatesRangeRestriction datesRangeRestriction) {
        if (datesRangeRestriction == null) {
            return null;
        }
        OffsetDateTime dateAfter = datesRangeRestriction.getAfter();
        OffsetDateTime dateBefore = datesRangeRestriction.getBefore();
        if (dateAfter == null && dateBefore == null) {
            return null;
        }
        if (dateAfter == null) {
            return before(field, dateBefore);
        }
        if (dateBefore == null) {
            return after(field, dateAfter);
        }
        return ((root, query, criteriaBuilder) -> criteriaBuilder.between(root.get(field), dateBefore, dateAfter));
    }

    protected Specification<T> before(String field, OffsetDateTime date) {
        if (date == null) {
            return null;
        } else {
            return ((root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get(field), date));
        }
    }

    protected Specification<T> after(String field, OffsetDateTime date) {
        if (date == null) {
            return null;
        } else {
            return ((root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get(field), date));
        }
    }

    protected Specification<T> joinedEquals(String join, String field, String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        } else {
            return ((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.join(join).get(field), value));
        }
    }

    protected Specification<T> isMember(String field, String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        } else {
            return ((root, query, criteriaBuilder) -> criteriaBuilder.isMember(value, root.get(field)));
        }
    }

    protected Specification<T> useValuesRestriction(String field, ValuesRestriction<?> valuesRestriction) {
        if (valuesRestriction == null) {
            return null;
        }
        Collection<?> values = valuesRestriction.getValues();
        if (CollectionUtils.isEmpty(values)) {
            return null;
        }
        if (valuesRestriction.getMode() == ValuesRestrictionMode.INCLUDE) {
            return isIncluded(field, values);
        }
        return isExcluded(field, values);
    }

    protected Specification<T> isIncluded(String field, Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return null;
        } else {
            return (root, query, criteriaBuilder) -> root.get(field).in(values);
        }
    }

    protected Specification<T> isExcluded(String field, Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return null;
        } else {
            return (root, query, criteriaBuilder) -> root.get(field).in(values).not();
        }
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
