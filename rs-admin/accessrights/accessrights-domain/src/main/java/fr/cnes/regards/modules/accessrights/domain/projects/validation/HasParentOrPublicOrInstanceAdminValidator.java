/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain.projects.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;

/**
 * Implement the logic to validate the constraint specified by {@link HasParentOrPublicOrInstanceAdmin} annotation.
 *
 * @author Xavier-Alexandre Brochard
 * @author Sylvain Vissiere-Guerinet
 */
public class HasParentOrPublicOrInstanceAdminValidator
        implements ConstraintValidator<HasParentOrPublicOrInstanceAdmin, Role> {

    @Override
    public void initialize(final HasParentOrPublicOrInstanceAdmin pArg0) {
        // Nothing to initialize for now
    }

    @Override
    public boolean isValid(final Role pValue, final ConstraintValidatorContext pContext) {
        if (pValue == null) {
            return true;
        }
        boolean shouldHaveParentRole = !pValue.getName().equals(DefaultRole.PUBLIC.toString())
                && !pValue.getName().equals(DefaultRole.INSTANCE_ADMIN.toString());
        return ((!shouldHaveParentRole) && (pValue.getParentRole() == null))
                || ((shouldHaveParentRole) && (pValue.getParentRole() != null));
    }

}
