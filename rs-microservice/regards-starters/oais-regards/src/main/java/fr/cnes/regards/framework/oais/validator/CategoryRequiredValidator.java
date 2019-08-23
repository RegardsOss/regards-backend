/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.oais.validator;

import java.util.Collection;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import fr.cnes.regards.framework.oais.PreservationDescriptionInformation;
import fr.cnes.regards.framework.oais.adapter.InformationPackageMap;

/**
 * Check if context information has at least one category.
 *
 * @author Marc SORDI
 *
 */
public class CategoryRequiredValidator implements ConstraintValidator<CategoryRequired, InformationPackageMap> {

    @Override
    public boolean isValid(InformationPackageMap value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }

        Object categories = value.get(PreservationDescriptionInformation.CONTEXT_INFO_CATEGORIES);
        if (categories == null) {
            return false;
        }

        if (Collection.class.isAssignableFrom(categories.getClass())) {
            @SuppressWarnings("unchecked")
            Collection<String> cats = (Collection<String>) categories;
            return !cats.isEmpty();
        } else {
            return false;
        }

    }

}
