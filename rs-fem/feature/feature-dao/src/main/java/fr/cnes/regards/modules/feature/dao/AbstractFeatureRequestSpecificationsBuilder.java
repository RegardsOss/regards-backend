/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.framework.jpa.utils.AbstractSpecificationsBuilder;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.SearchFeatureRequestParameters;
import jakarta.annotation.Nullable;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Set;

/**
 * Builder for specifications with all classes extended AbstractFeatureRequest
 *
 * @author Stephane Cortine
 */
public abstract class AbstractFeatureRequestSpecificationsBuilder<T>
    extends AbstractSpecificationsBuilder<T, SearchFeatureRequestParameters> {

    protected boolean searchInFeatureEntity = true;

    @Override
    protected void addSpecificationsFromParameters(SearchFeatureRequestParameters parameters) {
        add(useValuesRestriction("state", parameters.getStates()));

        add(after("registrationDate", parameters.getLastUpdate().getAfter()));
        add(before("registrationDate", parameters.getLastUpdate().getBefore()));

        add(useValuesRestriction("step", parameters.getSteps()));
        
        add(useValuesRestriction("id", parameters.getRequestIds()));

        if (this.searchInFeatureEntity) {
            add(equalsWithFeatureEntity("sessionOwner", parameters.getSource()));
            add(equalsWithFeatureEntity("session", parameters.getSession()));
            add(useValuesRestrictionLikeWithFeatureEntity("providerId", parameters.getProviderIds()));
        }
    }

    /**
     * Add restriction from the given value to a joined {@link FeatureEntity} elements by urn
     */
    protected Specification<T> equalsWithFeatureEntity(String pathToField, @Nullable String value) {
        return (root, query, criteriaBuilder) -> {
            if (value == null) {
                return null;
            }
            Root<FeatureEntity> fr = query.from(FeatureEntity.class);
            Set<Predicate> predicates = Sets.newHashSet();
            predicates.add(criteriaBuilder.equal(fr.get("urn"), root.get("urn")));
            predicates.add(criteriaBuilder.equal(fr.get(pathToField), value));

            return criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

    /**
     * Add restriction from the given valuesrestriction to a joined {@link FeatureEntity} elements by urn
     */
    protected Specification<T> useValuesRestrictionLikeWithFeatureEntity(String pathToField,
                                                                         @Nullable
                                                                         ValuesRestriction<?> valuesRestriction) {
        if (valuesRestriction == null) {
            return null;
        }
        Collection<?> values = valuesRestriction.getValues();
        Assert.notEmpty(values, "Values must not be empty");

        return (root, query, criteriaBuilder) -> {
            Set<Predicate> predicates = Sets.newHashSet();
            Root<FeatureEntity> fr = query.from(FeatureEntity.class);
            predicates.add(criteriaBuilder.equal(fr.get("urn"), root.get("urn")));
            predicates.add(createValuesRestrictionPredicate(fr, criteriaBuilder, pathToField, valuesRestriction));
            return criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

}
