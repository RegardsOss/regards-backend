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
 * Class ProjectConnectionTest
 *
 * Test ProjectConnection domain Pojos
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public class ProjectConnectionTest {

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
    public void projectConnectionPojoTest() {

        final Long id = 0L;
        final String miroservice = "ms";
        final String user = "user";
        final String pwd = "pwd";
        final String driver = "driver";
        final String url = "url";
        // Check empty project
        final Project project = new Project(0L, "desc", "icon", true, "name");

        ProjectConnection connection = new ProjectConnection(id, project, miroservice, user, pwd, driver, url);
        Assert.assertTrue(connection.getId().equals(id));
        Assert.assertTrue(connection.getProject().equals(project));
        Assert.assertTrue(connection.getMicroservice().equals(miroservice));
        Assert.assertTrue(connection.getUserName().equals(user));
        Assert.assertTrue(connection.getPassword().equals(pwd));
        Assert.assertTrue(connection.getDriverClassName().equals(driver));
        Assert.assertTrue(connection.getUrl().equals(url));
        Set<ConstraintViolation<ProjectConnection>> constraintViolations = validator.validate(connection);
        Assert.assertEquals(0, constraintViolations.size());

        connection = new ProjectConnection(0L, null, miroservice, user, pwd, driver, url);
        constraintViolations = validator.validate(connection);
        Assert.assertEquals(1, constraintViolations.size());

        connection = new ProjectConnection(0L, project, null, user, pwd, driver, url);
        constraintViolations = validator.validate(connection);
        Assert.assertEquals(1, constraintViolations.size());

        connection = new ProjectConnection(0L, null, null, user, pwd, driver, url);
        constraintViolations = validator.validate(connection);
        Assert.assertEquals(2, constraintViolations.size());
    }

}
