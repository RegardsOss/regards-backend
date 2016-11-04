/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.domain;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * Class ProjectTest
 *
 * Test Project domain Pojos
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public class ProjectTest {

    /**
     * Validator
     */
    private static Validator validator;

    @BeforeClass
    public static void setUp() {
        final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    /**
     *
     * projectPojoTest
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void projectPojoTest() {
        final String description = "description";
        final String icon = "icon";
        final boolean ispublic = true;
        final String name = "name";
        final Long id = 0L;

        Project project = new Project(id, description, icon, ispublic, name);
        Assert.assertTrue(project.getDescription().equals(description));
        Assert.assertTrue(project.getName().equals(name));
        Assert.assertTrue(project.getId().equals(id));
        Assert.assertTrue(!project.isDeleted());
        Assert.assertTrue(project.isPublic() == ispublic);
        Set<ConstraintViolation<Project>> constraintViolations = validator.validate(project);
        Assert.assertEquals(0, constraintViolations.size());

        project = new Project(0L, description, icon, ispublic, null);
        constraintViolations = validator.validate(project);
        Assert.assertEquals(1, constraintViolations.size());

        project = new Project(0L, null, icon, ispublic, null);
        constraintViolations = validator.validate(project);
        Assert.assertEquals(2, constraintViolations.size());
    }

}
