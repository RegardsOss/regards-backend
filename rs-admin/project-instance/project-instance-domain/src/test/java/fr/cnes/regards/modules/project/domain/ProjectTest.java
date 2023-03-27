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
 * Unit testing of {@link ProjectTest}
 *
 * @author Sebastien Binda
 * @author Maxime Bouveron
 */
public class ProjectTest {

    /**
     * Validator
     */
    private static Validator validator;

    /**
     * Test Project
     */
    private Project projectTest;

    /**
     * Test Description
     */
    private final String description = "description";

    /**
     * Test icon
     */
    private final String icon = "icon";

    /**
     * Test ispublic
     */
    private final boolean ispublic = true;

    /**
     * Test name
     */
    private final String name = "name";

    /**
     * Test id
     */
    private final Long id = 0L;

    @Before
    public void setUp() {
        final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        this.projectTest = new Project(id, description, icon, ispublic, name);
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.project.domain.Project#Project()}.
     */
    @Test
    public void testProject() {
        final Project project = new Project();
        project.setLabel(project.getName());

        Assert.assertNull(project.getId());
        Assert.assertNotEquals(null, project.getDescription());
        Assert.assertNull(project.getIcon());
        Assert.assertFalse(project.isPublic());
        Assert.assertNotEquals(null, project.getName());

        final Set<ConstraintViolation<Project>> constraintViolations = validator.validate(project);
        Assert.assertEquals(0, constraintViolations.size());
    }

    @Test
    public void testProjectName() {
        Project project = new Project(id, description, icon, ispublic, "invalid project");
        project.setLabel(project.getName());
        Set<ConstraintViolation<Project>> constraintViolations = validator.validate(project);
        Assert.assertNotEquals(0, constraintViolations.size());

        project = new Project(id, description, icon, ispublic, "invalid,project");
        project.setLabel(project.getName());
        constraintViolations = validator.validate(project);
        Assert.assertNotEquals(0, constraintViolations.size());

        project = new Project(id, description, icon, ispublic, "invalid;project");
        project.setLabel(project.getName());
        constraintViolations = validator.validate(project);
        Assert.assertNotEquals(0, constraintViolations.size());

        project = new Project(id, description, icon, ispublic, "invalid (project)");
        project.setLabel(project.getName());
        constraintViolations = validator.validate(project);
        Assert.assertNotEquals(0, constraintViolations.size());

        project = new Project(id, description, icon, ispublic, "invalid [project]");
        project.setLabel(project.getName());
        constraintViolations = validator.validate(project);
        Assert.assertNotEquals(0, constraintViolations.size());

        project = new Project(id, description, icon, ispublic, "invalid [project]");
        project.setLabel(project.getName());
        constraintViolations = validator.validate(project);
        Assert.assertNotEquals(0, constraintViolations.size());

        project = new Project(id, description, icon, ispublic, "invalid=project");
        project.setLabel(project.getName());
        constraintViolations = validator.validate(project);
        Assert.assertNotEquals(0, constraintViolations.size());

        project = new Project(id, description, icon, ispublic, "invalid&project");
        project.setLabel(project.getName());
        constraintViolations = validator.validate(project);
        Assert.assertNotEquals(0, constraintViolations.size());

        project = new Project(id, description, icon, ispublic, "invalid\"project");
        project.setLabel(project.getName());
        constraintViolations = validator.validate(project);
        Assert.assertNotEquals(0, constraintViolations.size());

        project = new Project(id, description, icon, ispublic, "invalid/project");
        project.setLabel(project.getName());
        constraintViolations = validator.validate(project);
        Assert.assertNotEquals(0, constraintViolations.size());

        project = new Project(id, description, icon, ispublic, "valid-project");
        project.setLabel(project.getName());
        constraintViolations = validator.validate(project);
        Assert.assertEquals(0, constraintViolations.size());

        project = new Project(id, description, icon, ispublic, "valid_project");
        project.setLabel(project.getName());
        constraintViolations = validator.validate(project);
        Assert.assertEquals(0, constraintViolations.size());

        project = new Project(id, description, icon, ispublic, "validproject0");
        project.setLabel(project.getName());
        constraintViolations = validator.validate(project);
        Assert.assertEquals(0, constraintViolations.size());

        project = new Project(id, description, icon, ispublic, "validProject");
        project.setLabel(project.getName());
        constraintViolations = validator.validate(project);
        Assert.assertEquals(0, constraintViolations.size());
    }

    /**
     * Test method for {@link Project#Project(Long, String, String, boolean, String)}.
     */
    @Test
    public void testProjectWithId() {
        Project project = new Project(id, description, icon, ispublic, name);
        project.setLabel(project.getName());

        Assert.assertEquals(id, project.getId());
        Assert.assertEquals(description, project.getDescription());
        Assert.assertEquals(icon, project.getIcon());
        Assert.assertEquals(ispublic, project.isPublic());
        Assert.assertEquals(name, project.getName());

        Set<ConstraintViolation<Project>> constraintViolations = validator.validate(project);
        Assert.assertEquals(0, constraintViolations.size());

        // Check one constraint violation
        project = new Project(id, null, icon, ispublic, name);
        project.setLabel(project.getName());
        constraintViolations = validator.validate(project);
        Assert.assertEquals(1, constraintViolations.size());

        // Check two constraint violation
        project = new Project(id, null, icon, ispublic, null);
        project.setLabel("jfkf");
        constraintViolations = validator.validate(project);
        Assert.assertEquals(2, constraintViolations.size());
    }

    /**
     * Test method for {@link Project#Project(String, String, boolean, String)}.
     */
    @Test
    public void testProjectWithoutId() {
        Project project = new Project(description, icon, ispublic, name);
        project.setLabel(project.getName());
        Assert.assertEquals(description, project.getDescription());
        Assert.assertEquals(icon, project.getIcon());
        Assert.assertEquals(ispublic, project.isPublic());
        Assert.assertEquals(name, project.getName());

        Set<ConstraintViolation<Project>> constraintViolations = validator.validate(project);
        Assert.assertEquals(0, constraintViolations.size());

        // Check one constraint violation
        project = new Project(id, description, icon, ispublic, null);
        project.setLabel("jfkf");
        constraintViolations = validator.validate(project);
        Assert.assertEquals(1, constraintViolations.size());

        // Check two constraint violation
        project = new Project(id, null, icon, ispublic, null);
        project.setLabel("jfkf");
        constraintViolations = validator.validate(project);
        Assert.assertEquals(2, constraintViolations.size());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.project.domain.Project#getId()}.
     */
    @Test
    public void testGetId() {
        Assert.assertEquals(id, projectTest.getId());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.project.domain.Project#setId(java.lang.Long)}.
     */
    @Test
    public void testSetId() {
        final Long newId = 2L;
        projectTest.setId(newId);
        Assert.assertEquals(newId, projectTest.getId());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.project.domain.Project#getName()}.
     */
    @Test
    public void testGetName() {
        Assert.assertEquals(name, projectTest.getName());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.project.domain.Project#setName(java.lang.String)}.
     */
    @Test
    public void testSetName() {
        final String newName = "newName";
        projectTest.setName(newName);
        Assert.assertEquals(newName, projectTest.getName());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.project.domain.Project#getDescription()}.
     */
    @Test
    public void testGetDescription() {
        Assert.assertEquals(description, projectTest.getDescription());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.project.domain.Project#setDescription(java.lang.String)}.
     */
    @Test
    public void testSetDescription() {
        final String newDescription = "newDescription";
        projectTest.setDescription(newDescription);
        Assert.assertEquals(newDescription, projectTest.getDescription());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.project.domain.Project#getIcon()}.
     */
    @Test
    public void testGetIcon() {
        Assert.assertEquals(icon, projectTest.getIcon());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.project.domain.Project#setIcon(java.lang.String)}.
     */
    @Test
    public void testSetIcon() {
        final String newIcon = "newIcon";
        projectTest.setIcon(newIcon);
        Assert.assertEquals(newIcon, projectTest.getIcon());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.project.domain.Project#isPublic()}.
     */
    @Test
    public void testIsPublic() {
        Assert.assertEquals(ispublic, projectTest.isPublic());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.project.domain.Project#setPublic(boolean)}.
     */
    @Test
    public void testSetPublic() {
        projectTest.setPublic(!ispublic);
        Assert.assertEquals(!ispublic, projectTest.isPublic());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.project.domain.Project#equals(java.lang.Object)}.
     */
    @Test
    public void testEqualsObjectTrue() {
        final Project project = new Project(id, description, icon, ispublic, name);
        Assert.assertEquals(projectTest, project);
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.project.domain.Project#equals(java.lang.Object)}.
     */
    @Test
    public void testEqualsObjectFalse() {
        final Project project = new Project(3L, description, icon, ispublic, name);
        Assert.assertNotEquals(projectTest, project);
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.project.domain.Project#isDeleted()}.
     */
    @Test
    public void testIsDeleted() {
        Assert.assertTrue(!projectTest.isDeleted());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.project.domain.Project#setDeleted(boolean)}.
     */
    @Test
    public void testSetDeleted() {
        final boolean deleted = !projectTest.isDeleted();
        projectTest.setDeleted(deleted);
        Assert.assertEquals(deleted, projectTest.isDeleted());
    }

}
