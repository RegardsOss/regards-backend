/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain.projects;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.regards.framework.test.report.annotation.Purpose;

/**
 * Validate hibernate constraints on {@link Role}.
 *
 * @author Xavier-Alexandre Brochard
 */
public class RoleValidationTest {

    /**
     * Javax validator
     */
    private static Validator validator;

    /**
     * A role
     */
    private static Role role;

    /**
     * Set up the validator
     */
    @BeforeClass
    public static void setUpValidator() {
        final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Before
    public void setUp() {
        final RoleFactory roleFactory = new RoleFactory();
        role = roleFactory.createInstanceAdmin();
    }

    /**
     * Check that the system prevents the role PUBLIC to have a parent.
     */
    @Test
    @Purpose("Check that the system prevents the role PUBLIC to have a parent.")
    public void testRoleIsPublicAndHasParent() {
        // Init the malformed role PUBLIC - it must have no parent
        role.setName(DefaultRoleNames.PUBLIC.toString());
        final Role parent = new Role();
        role.setParentRole(parent);

        // Run the validator
        final Set<ConstraintViolation<Role>> constraintViolations = validator.validate(role);

        // Check constraint violations
        Assert.assertEquals(1, constraintViolations.size());
    }

    /**
     * Check that the system allows the role PUBLIC to have a null parent.
     */
    @Test
    @Purpose("Check that the system allows the role PUBLIC to have a null parent.")
    public void testRoleIsPublicAndNullParent() {
        // Init the well formed role PUBLIC
        role.setName(DefaultRoleNames.PUBLIC.toString());
        role.setParentRole(null);

        // Run the validator
        final Set<ConstraintViolation<Role>> constraintViolations = validator.validate(role);

        // Check constraint violations
        Assert.assertEquals(0, constraintViolations.size());
    }

    /**
     * Check that the system prevent a role not PUBLIC to have a null parent.
     */
    @Test
    @Purpose("Check that the system prevent a role not PUBLIC to have a null parent.")
    public void testRoleIsNotPublicAndNullParent() {
        // Init the malformed role
        role.setName("RandomRoleName");
        role.setParentRole(null);

        // Run the validator
        final Set<ConstraintViolation<Role>> constraintViolations = validator.validate(role);

        // Check constraint violations
        Assert.assertEquals(1, constraintViolations.size());
    }

    /**
     * Check that the system allows a role not PUBLIC to have a non null parent.
     */
    @Test
    @Purpose("Check that the system allows a role not PUBLIC to have a non null parent.")
    public void testRoleIsNotPublicAndNotNullParent() {
        // Init the wellformed role
        role.setName("RandomRoleName");
        role.setParentRole(new Role());

        // Run the validator
        final Set<ConstraintViolation<Role>> constraintViolations = validator.validate(role);

        // Check constraint violations
        Assert.assertEquals(0, constraintViolations.size());
    }

}
