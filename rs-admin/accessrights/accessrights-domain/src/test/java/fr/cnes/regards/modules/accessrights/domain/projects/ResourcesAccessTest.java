/**
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.domain.projects;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.security.role.DefaultRole;

/**
 * Unit testing of {@link ResourcesAccess}
 *
 * @author Maxime Bouveron
 */
public class ResourcesAccessTest {

    /**
     * Test ResourcesAccess
     */
    private ResourcesAccess resourcesAccess;

    /**
     * Test id
     */
    private final Long id = 0L;

    /**
     * Test description
     */
    private final String description = "description";

    /**
     * Test microservice
     */
    private final String microservice = "microservice";

    /**
     * Test resource
     */
    private final String resource = "resource";

    /**
     * Test verb
     */
    private final RequestMethod verb = RequestMethod.HEAD;

    private final String controller = "controller";

    @Before
    public void setUp() {
        resourcesAccess = new ResourcesAccess(id, description, microservice, resource, controller, verb,
                DefaultRole.ADMIN);
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess#ResourcesAccess()}.
     */
    @Test
    public void testResourcesAccess() {
        final ResourcesAccess testResources = new ResourcesAccess();

        Assert.assertNull(testResources.getId());
        Assert.assertNull(testResources.getDescription());
        Assert.assertNull(testResources.getMicroservice());
        Assert.assertNull(testResources.getResource());
        Assert.assertEquals(RequestMethod.GET, testResources.getVerb());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess#ResourcesAccess(java.lang.Long)}.
     */
    @Test
    public void testResourcesAccessId() {
        final ResourcesAccess testResources = new ResourcesAccess(id);

        Assert.assertEquals(id, testResources.getId());
        Assert.assertNull(testResources.getDescription());
        Assert.assertNull(testResources.getMicroservice());
        Assert.assertNull(testResources.getResource());
        Assert.assertEquals(RequestMethod.GET, testResources.getVerb());
    }

    /**
     * Test ResourcesAccess constructor
     */
    @Test
    public void testResourcesAccessWithEverything() {
        final ResourcesAccess testResources = new ResourcesAccess(id, description, microservice, resource, controller,
                verb, DefaultRole.ADMIN);

        Assert.assertEquals(id, testResources.getId());
        Assert.assertEquals(description, testResources.getDescription());
        Assert.assertEquals(microservice, testResources.getMicroservice());
        Assert.assertEquals(resource, testResources.getResource());
        Assert.assertEquals(verb, testResources.getVerb());
    }

    /**
     * Test ResourcesAccess constructor
     */
    @Test
    public void testResourcesAccessWithoutID() {
        final ResourcesAccess testResources = new ResourcesAccess(description, microservice, resource, controller, verb,
                DefaultRole.ADMIN);

        Assert.assertNull(testResources.getId());
        Assert.assertEquals(description, testResources.getDescription());
        Assert.assertEquals(microservice, testResources.getMicroservice());
        Assert.assertEquals(resource, testResources.getResource());
        Assert.assertEquals(verb, testResources.getVerb());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess#getId()}.
     */
    @Test
    public void testGetId() {
        Assert.assertEquals(id, resourcesAccess.getId());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess#setId(java.lang.Long)}.
     */
    @Test
    public void testSetId() {
        final Long newId = 4L;
        resourcesAccess.setId(newId);
        Assert.assertEquals(newId, resourcesAccess.getId());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess#getDescription()}.
     */
    @Test
    public void testGetDescription() {
        Assert.assertEquals(description, resourcesAccess.getDescription());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess#setDescription(java.lang.String)}.
     */
    @Test
    public void testSetDescription() {
        final String newDescription = "newDescription";
        resourcesAccess.setDescription(newDescription);
        Assert.assertEquals(newDescription, resourcesAccess.getDescription());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess#getMicroservice()}.
     */
    @Test
    public void testGetMicroservice() {
        Assert.assertEquals(microservice, resourcesAccess.getMicroservice());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess#setMicroservice(java.lang.String)}.
     */
    @Test
    public void testSetMicroservice() {
        final String newMicroservice = "newMicroservice";
        resourcesAccess.setMicroservice(newMicroservice);
        Assert.assertEquals(newMicroservice, resourcesAccess.getMicroservice());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess#getResource()}.
     */
    @Test
    public void testGetResource() {
        Assert.assertEquals(resource, resourcesAccess.getResource());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess#setResource(java.lang.String)}.
     */
    @Test
    public void testSetResource() {
        final String newResource = "newResource";
        resourcesAccess.setResource(newResource);
        Assert.assertEquals(newResource, resourcesAccess.getResource());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess#getVerb()}.
     */
    @Test
    public void testGetVerb() {
        Assert.assertEquals(verb, resourcesAccess.getVerb());
    }

    @Test
    public void testSetVerb() {
        final RequestMethod newVerb = RequestMethod.DELETE;
        resourcesAccess.setVerb(newVerb);
        Assert.assertEquals(newVerb, resourcesAccess.getVerb());
    }

}
