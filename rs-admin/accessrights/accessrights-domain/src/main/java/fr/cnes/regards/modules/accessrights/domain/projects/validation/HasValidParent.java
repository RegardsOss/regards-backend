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
package fr.cnes.regards.modules.accessrights.domain.projects.validation;

import fr.cnes.regards.modules.accessrights.domain.projects.Role;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allow to validate the field <code>parentRole</code> of a {@link Role}.
 * <p/>
 * Specifies that the annotated role must:
 * <ul>
 * <li>have a <code>null</code> <code>parentRole</code> if it is the role "PUBLIC" or "INSTANCE_ADMIN" or
 * "PROJECT_ADMIN"</li>
 * <li>have a non <code>null</code> which is not "INSTANCE_ADMIN" or "PROJECT_ADMIN" <code>parentRole</code>
 * otherwise</li>
 * </ul>
 *
 * @author Xavier-Alexandre Brochard
 * @author Sylvain Vissiere-Guerinet
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
@Constraint(validatedBy = HasValidParentValidator.class)
public @interface HasValidParent {

    String message() default "Role should have a parent role which is native unless it is one of the following roles: PUBLIC, INSTANCE_ADMIN, PROJECT_ADMIN";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
