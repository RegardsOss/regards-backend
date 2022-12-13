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
import fr.cnes.regards.framework.jpa.utils.AbstractSpecificationsBuilder;
import fr.cnes.regards.modules.feature.domain.FeatureSimpleEntity;
import fr.cnes.regards.modules.feature.domain.SearchFeatureSimpleEntityParameters;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;

import javax.persistence.criteria.Predicate;
import java.util.Collection;
import java.util.Set;

/**
 * @author Stephane Cortine
 */
public class FeatureSimpleEntitySpecificationBuilder
    extends AbstractSpecificationsBuilder<FeatureSimpleEntity, SearchFeatureSimpleEntityParameters> {

    @Override
    protected void addSpecificationsFromParameters() {
        if (parameters != null) {
            specifications.add(equals("model", parameters.getModel()));

            specifications.add(equals("sessionOwner", parameters.getSource()));
            specifications.add(equals("session", parameters.getSession()));

            specifications.add(useValuesRestrictionLike("providerId", parameters.getProviderIds()));

            specifications.add(after("lastUpdate", parameters.getLastUpdate().getAfter()));
            specifications.add(before("lastUpdate", parameters.getLastUpdate().getBefore()));

            specifications.add(equals("disseminationPending", parameters.getDisseminationPending()));
        }
    }

    protected Specification<FeatureSimpleEntity> useValuesRestrictionLike(String pathToField,
                                                                          ValuesRestriction<String> valuesRestriction) {
        if (valuesRestriction == null) {
            return null;
        }
        Collection<String> values = valuesRestriction.getValues();
        if (CollectionUtils.isEmpty(values)) {
            return null;
        }
        if (valuesRestriction.getMode() == ValuesRestrictionMode.INCLUDE) {
            return isIncludedString(pathToField, values);
        }
        return isExcludedString(pathToField, values);
    }

    protected Specification<FeatureSimpleEntity> isIncludedString(String pathToField, Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        } else {
            return (root, query, criteriaBuilder) -> {
                Set<Predicate> predicates = Sets.newHashSet();
                for (String value : values) {
                    predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("providerId")),
                                                        value.toLowerCase() + "%"));
                }
                return criteriaBuilder.or(predicates.toArray(new Predicate[predicates.size()]));
            };
        }
    }

    protected Specification<FeatureSimpleEntity> isExcludedString(String pathToField, Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        } else {
            return (root, query, criteriaBuilder) -> {
                Set<Predicate> predicates = Sets.newHashSet();
                for (String value : values) {
                    predicates.add(criteriaBuilder.notLike(criteriaBuilder.lower(root.get("providerId")),
                                                           value.toLowerCase() + "%"));
                }
                return criteriaBuilder.or(predicates.toArray(new Predicate[predicates.size()]));
            };
        }
    }
}
