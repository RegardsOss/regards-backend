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
package fr.cnes.regards.modules.model.service.validation.validator.iproperty.restriction;

import fr.cnes.regards.modules.model.domain.attributes.restriction.EnumerationRestriction;
import fr.cnes.regards.modules.model.dto.properties.StringArrayProperty;
import fr.cnes.regards.modules.model.dto.properties.StringProperty;
import fr.cnes.regards.modules.model.service.validation.validator.common.restriction.AbstractEnumerationValidator;

/**
 * Validate {@link StringProperty} or {@link StringArrayProperty} value with an {@link EnumerationRestriction}
 *
 * @author Marc Sordi
 */
public class EnumerationPropertyValidator extends AbstractEnumerationValidator {

    public EnumerationPropertyValidator(EnumerationRestriction pRestriction, String pAttributeKey) {
        super(pRestriction, pAttributeKey);
    }

    @Override
    protected String getStringValue(Object pTarget) {
        return ((StringProperty) pTarget).getValue();
    }

    @Override
    protected String[] getStringArrayValue(Object pTarget) {
        return ((StringArrayProperty) pTarget).getValue();
    }

    @Override
    protected boolean isString(Class<?> clazz) {
        return clazz == StringProperty.class;
    }

    @Override
    protected boolean isStringArray(Class<?> clazz) {
        return clazz == StringArrayProperty.class;
    }

}
