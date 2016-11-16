/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain.projects.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import fr.cnes.regards.modules.accessrights.domain.projects.DefaultRoleNames;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;

/**
 * Implement the logic to validate the constraint specified by {@link HasParentOrPublic} annotation.
 *
 * @author Xavier-Alexandre Brochard
 */
public class HasParentOrPublicValidator implements ConstraintValidator<HasParentOrPublic, Role> {

    /*
     * (non-Javadoc)
     *
     * @see javax.validation.ConstraintValidator#initialize(java.lang.annotation.Annotation)
     */
    @Override
    public void initialize(final HasParentOrPublic pArg0) {
        // Nothing to initialize for now
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.validation.ConstraintValidator#isValid(java.lang.Object, javax.validation.ConstraintValidatorContext)
     */
    @Override
    public boolean isValid(final Role pValue, final ConstraintValidatorContext pContext) {
        return ((!pValue.getName().equals(DefaultRoleNames.PUBLIC.toString())) && (pValue.getParentRole() != null))
                || ((pValue.getName().equals(DefaultRoleNames.PUBLIC.toString())) && (pValue.getParentRole() == null));
    }

}
