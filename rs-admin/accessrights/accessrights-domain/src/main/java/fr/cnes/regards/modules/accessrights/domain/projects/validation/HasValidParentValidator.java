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
package fr.cnes.regards.modules.accessrights.domain.projects.validation;

import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Implement the logic to validate the constraint specified by {@link HasValidParent} annotation.
 *
 * @author Xavier-Alexandre Brochard
 * @author Sylvain Vissiere-Guerinet
 */
public class HasValidParentValidator implements ConstraintValidator<HasValidParent, Role> {

    @Override
    public void initialize(final HasValidParent pArg0) {
        // Nothing to initialize for now
    }

    @Override
    public boolean isValid(final Role pRole, final ConstraintValidatorContext pContext) {
        if (pRole == null) {
            return true;
        }
        String roleName = pRole.getName();
        boolean shouldHaveParentRole = !(roleName.equals(DefaultRole.PUBLIC.toString())
            || roleName.equals(DefaultRole.INSTANCE_ADMIN.toString())
            || roleName.equals(DefaultRole.PROJECT_ADMIN.toString()));
        if (shouldHaveParentRole) {
            Role parentRole = pRole.getParentRole();
            if ((parentRole == null) || !parentRole.isNative()) {
                return false;
            }
            // INSTANCE_ADMIN and PROJECT_ADMIN cannot have any children
            String parentRoleName = parentRole.getName();
            return !((parentRoleName.equals(DefaultRole.INSTANCE_ADMIN.toString()))) || (parentRoleName.equals(
                DefaultRole.PROJECT_ADMIN.toString()));
        } else {
            return pRole.getParentRole() == null;
        }
    }

}
