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
package fr.cnes.regards.modules.access.services.domain.validation;

import fr.cnes.regards.modules.access.services.domain.ui.UIPluginDefinition;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Validator enforcing {@link UIPluginDefinition} constraints
 *
 * @author Xavier-Alexandre Brochard
 */
public class NotEmptyFieldsIfServiceValidator
    implements ConstraintValidator<NotEmptyFieldsIfService, UIPluginDefinition> {

    @Override
    public void initialize(NotEmptyFieldsIfService pConstraintAnnotation) {
        // nothing to do
    }

    @Override
    public boolean isValid(UIPluginDefinition pValue, ConstraintValidatorContext pContext) {
        switch (pValue.getType()) {
            case SERVICE:
                return !pValue.getApplicationModes().isEmpty() && !pValue.getEntityTypes().isEmpty();
            default:
                return pValue.getApplicationModes().isEmpty() && pValue.getEntityTypes().isEmpty();
        }
    }
}
