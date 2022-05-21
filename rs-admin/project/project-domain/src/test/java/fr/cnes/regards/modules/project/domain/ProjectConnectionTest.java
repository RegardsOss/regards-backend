/**
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 * <p>
 * This file is part of REGARDS.
 * <p>
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.project.domain;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

/**
 * Unit testing of {@link ProjectConnection}
 *
 * @author Sebastien Binda
 * @author Maxime Bouveron
 */
public class ProjectConnectionTest {

    /**
     * Validator
     */
    private static Validator validator;

    /**
     * Test id
     */
    private final Long id = 0L;

    /**
     * Test microservice
     */
    private final String miroservice = "ms";

    /**
     * Test User Name
     */
    private final String user = "user";

    /**
     * Test Password
     */
    private final String pwd = "pwd";

    /**
     * Test DriverClassName
     */
    private final String driver = "driver";

    /**
     * Test URL
     */
    private final String url = "url";

    /**
     * Test Project
     */
    private Project projectTest;

    /**
     * Test ProjectConnection
     */
    private ProjectConnection connection;

    @Before
    public void setUp() {
        final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        projectTest = new Project(0L, "desc", "icon", true, "name");
        connection = new ProjectConnection(id, projectTest, miroservice, user, pwd, driver, url);
    }

    /**
     * Test method for
     * {@link ProjectConnection#ProjectConnection(Long, Project, String, String, String, String, String)}.
     */
    @Test
    public void testProjectConnectionWithId() {
        ProjectConnection projectConn = new ProjectConnection(id, projectTest, miroservice, user, pwd, driver, url);

        Assert.assertEquals(id, projectConn.getId());
        Assert.assertEquals(projectTest, projectConn.getProject());
        Assert.assertEquals(miroservice, projectConn.getMicroservice());
        Assert.assertEquals(user, projectConn.getUserName());
        Assert.assertEquals(pwd, projectConn.getPassword());
        Assert.assertEquals(driver, projectConn.getDriverClassName());
        Assert.assertEquals(url, projectConn.getUrl());

        Set<ConstraintViolation<ProjectConnection>> constraintViolations = validator.validate(projectConn);
        Assert.assertEquals(0, constraintViolations.size());

        projectConn = new ProjectConnection(id, null, miroservice, user, pwd, driver, url);
        constraintViolations = validator.validate(projectConn);
        Assert.assertEquals(1, constraintViolations.size());

        projectConn = new ProjectConnection(id, null, null, user, pwd, driver, url);
        constraintViolations = validator.validate(projectConn);
        Assert.assertEquals(2, constraintViolations.size());
    }

    /**
     * Test method for {@link ProjectConnection#ProjectConnection(Project, String, String, String, String, String)}.
     */
    @Test
    public void testProjectConnectionWithoutId() {
        ProjectConnection projectConn = new ProjectConnection(projectTest, miroservice, user, pwd, driver, url);

        Assert.assertEquals(projectTest, projectConn.getProject());
        Assert.assertEquals(miroservice, projectConn.getMicroservice());
        Assert.assertEquals(user, projectConn.getUserName());
        Assert.assertEquals(pwd, projectConn.getPassword());
        Assert.assertEquals(driver, projectConn.getDriverClassName());
        Assert.assertEquals(url, projectConn.getUrl());

        Set<ConstraintViolation<ProjectConnection>> constraintViolations = validator.validate(projectConn);
        Assert.assertEquals(0, constraintViolations.size());

        projectConn = new ProjectConnection(projectTest, null, user, pwd, driver, url);
        constraintViolations = validator.validate(projectConn);
        Assert.assertEquals(1, constraintViolations.size());

        projectConn = new ProjectConnection(null, null, user, pwd, driver, url);
        constraintViolations = validator.validate(projectConn);
        Assert.assertEquals(2, constraintViolations.size());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.project.domain.ProjectConnection#getId()}.
     */
    @Test
    public void testGetId() {
        Assert.assertEquals(id, connection.getId());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.project.domain.ProjectConnection#setId(java.lang.Long)}.
     */
    @Test
    public void testSetId() {
        final Long newId = 2L;
        connection.setId(newId);
        Assert.assertEquals(newId, connection.getId());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.project.domain.ProjectConnection#getProject()}.
     */
    @Test
    public void testGetProject() {
        Assert.assertEquals(projectTest, connection.getProject());
    }

    /**
     * Test method for {@link ProjectConnection#setProject(Project)}.
     */
    @Test
    public void testSetProject() {
        final Project newProject = new Project(2L, "desc2", "icon2", true, "name2");
        connection.setProject(newProject);
        Assert.assertEquals(newProject, connection.getProject());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.project.domain.ProjectConnection#getMicroservice()}.
     */
    @Test
    public void testGetMicroservice() {
        Assert.assertEquals(miroservice, connection.getMicroservice());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.project.domain.ProjectConnection#setMicroservice(java.lang.String)}.
     */
    @Test
    public void testSetMicroservice() {
        final String newMicroservice = "newMS";
        connection.setMicroservice(newMicroservice);
        Assert.assertEquals(newMicroservice, connection.getMicroservice());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.project.domain.ProjectConnection#getUserName()}.
     */
    @Test
    public void testGetUserName() {
        Assert.assertEquals(user, connection.getUserName());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.project.domain.ProjectConnection#setUserName(java.lang.String)}.
     */
    @Test
    public void testSetUserName() {
        final String newUser = "newUser";
        connection.setUserName(newUser);
        Assert.assertEquals(newUser, connection.getUserName());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.project.domain.ProjectConnection#getPassword()}.
     */
    @Test
    public void testGetPassword() {
        Assert.assertEquals(pwd, connection.getPassword());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.project.domain.ProjectConnection#setPassword(java.lang.String)}.
     */
    @Test
    public void testSetPassword() {
        final String newPwd = "newPwd";
        connection.setPassword(newPwd);
        Assert.assertEquals(newPwd, connection.getPassword());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.project.domain.ProjectConnection#getDriverClassName()}.
     */
    @Test
    public void testGetDriverClassName() {
        Assert.assertEquals(driver, connection.getDriverClassName());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.project.domain.ProjectConnection#setDriverClassName(java.lang.String)}.
     */
    @Test
    public void testSetDriverClassName() {
        final String newDriver = "newDriver";
        connection.setDriverClassName(newDriver);
        Assert.assertEquals(newDriver, connection.getDriverClassName());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.project.domain.ProjectConnection#getUrl()}.
     */
    @Test
    public void testGetUrl() {
        Assert.assertEquals(url, connection.getUrl());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.project.domain.ProjectConnection#setUrl(java.lang.String)}.
     */
    @Test
    public void testSetUrl() {
        final String newURL = "newUrl";
        connection.setUrl(newURL);
        Assert.assertEquals(newURL, connection.getUrl());
    }

}
