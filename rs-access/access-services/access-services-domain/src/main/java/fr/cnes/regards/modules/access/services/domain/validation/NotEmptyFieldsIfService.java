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
package fr.cnes.regards.modules.access.services.domain.validation;

import fr.cnes.regards.modules.access.services.domain.ui.UIPluginDefinition;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginTypesEnum;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Ensure the that annotated {@link UIPluginDefinition} has non empty attributes <code>applicationModes</code> and <code>entityTypes</code>
 * if it is a {@link UIPluginTypesEnum#SERVICE}. Else they lust be <code>empty</code>.
 *
 * @author Xavier-Alexandre Brochard
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
@Constraint(validatedBy = NotEmptyFieldsIfServiceValidator.class)
@Documented
public @interface NotEmptyFieldsIfService {

    String message() default "The UIPluginDefinition should have non-empty [attributeModes] and [entityTypes] if it is a SERVICE, else empty ";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
