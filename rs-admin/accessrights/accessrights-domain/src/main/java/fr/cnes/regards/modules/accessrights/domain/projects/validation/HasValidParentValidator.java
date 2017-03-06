/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain.projects.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;

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
            if (!parentRole.isNative()) {
                return false;
            }
            // INSTANCE_ADMIN and PROJECT_ADMIN cannot have any children
            String parentRoleName = parentRole.getName();
            return !((parentRoleName.equals(DefaultRole.INSTANCE_ADMIN))
                    || (parentRoleName.equals(DefaultRole.PROJECT_ADMIN)));
        } else {
            return pRole.getParentRole() == null;
        }
    }

}
