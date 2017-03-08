/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain.projects.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import fr.cnes.regards.modules.accessrights.domain.projects.Role;

/**
 * Can only annotate the field <code>parentRole</code> of a {@link Role}.
 * <p/>
 * Specifies that the annotated role must:
 * <ul>
 * <li>have a non <code>null</code> <code>parentRole</code> if not the role "PUBLIC"</li>
 * <li>have a <code>null</code> <code>parentRole</code> if the role "PUBLIC" or "INSTANCE_ADMIN"</li>
 * </ul>
 *
 * @author Xavier-Alexandre Brochard
 * @author Sylvain Vissiere-Guerinet
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
@Constraint(validatedBy = HasParentOrPublicOrInstanceAdminValidator.class)
public @interface HasParentOrPublicOrInstanceAdmin {

    String message() default "Role should have a parent role";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
