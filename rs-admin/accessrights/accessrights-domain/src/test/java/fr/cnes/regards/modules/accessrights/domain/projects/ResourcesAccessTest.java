/**
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain.projects;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.regards.modules.accessrights.domain.HttpVerb;

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
    private final HttpVerb verb = HttpVerb.HEAD;

    @Before
    public void setUp() {
        resourcesAccess = new ResourcesAccess(id, description, microservice, resource, verb);
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess#ResourcesAccess()}.
     */
    @Test
    public void testResourcesAccess() {
        final ResourcesAccess testResources = new ResourcesAccess();

        Assert.assertEquals(null, testResources.getId());
        Assert.assertEquals(null, testResources.getDescription());
        Assert.assertEquals(null, testResources.getMicroservice());
        Assert.assertEquals(null, testResources.getResource());
        Assert.assertEquals(HttpVerb.GET, testResources.getVerb());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess#ResourcesAccess(java.lang.Long)}.
     */
    @Test
    public void testResourcesAccessId() {
        final ResourcesAccess testResources = new ResourcesAccess(id);

        Assert.assertEquals(id, testResources.getId());
        Assert.assertEquals(null, testResources.getDescription());
        Assert.assertEquals(null, testResources.getMicroservice());
        Assert.assertEquals(null, testResources.getResource());
        Assert.assertEquals(HttpVerb.GET, testResources.getVerb());
    }

    /**
     * Test method for {@link ResourcesAccess#ResourcesAccess(Long, String, String, String, HttpVerb)}.
     */
    @Test
    public void testResourcesAccessWithEverything() {
        final ResourcesAccess testResources = new ResourcesAccess(id, description, microservice, resource, verb);

        Assert.assertEquals(id, testResources.getId());
        Assert.assertEquals(description, testResources.getDescription());
        Assert.assertEquals(microservice, testResources.getMicroservice());
        Assert.assertEquals(resource, testResources.getResource());
        Assert.assertEquals(verb, testResources.getVerb());
    }

    /**
     * Test method for {@link ResourcesAccess#ResourcesAccess(String, String, String, HttpVerb)}.
     */
    @Test
    public void testResourcesAccessWithoutID() {
        final ResourcesAccess testResources = new ResourcesAccess(description, microservice, resource, verb);

        Assert.assertEquals(null, testResources.getId());
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

    /**
     * Test method for {@link ResourcesAccess#setVerb(fr.cnes.regards.modules.accessrights.domain.HttpVerb)}.
     */
    @Test
    public void testSetVerb() {
        final HttpVerb newVerb = HttpVerb.DELETE;
        resourcesAccess.setVerb(newVerb);
        Assert.assertEquals(newVerb, resourcesAccess.getVerb());
    }

}
