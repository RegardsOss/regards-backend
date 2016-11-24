/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.templates.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.modules.templates.domain.Template;
import fr.cnes.regards.modules.templates.service.ITemplateService;
import fr.cnes.regards.modules.templates.test.TemplateTestConstants;

/**
 * Templates controller unit test
 *
 * @author Xavier-Alexandre Brochard
 */
public class TemplateControllerTest {

    /**
     * A template with some values
     */
    private Template template;

    /**
     * Controller under test
     */
    private TemplateController templateController;

    /**
     * Mocked template service
     */
    private ITemplateService templateService;

    /**
     * Mocked resource service
     */
    private IResourceService resourceService;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Init a template
        template = new Template(TemplateTestConstants.CODE, TemplateTestConstants.CONTENT, TemplateTestConstants.DATA);
        template.setId(TemplateTestConstants.ID);

        // Mock stuff
        templateService = Mockito.mock(ITemplateService.class);
        final MethodAuthorizationService authService = Mockito.mock(MethodAuthorizationService.class);
        Mockito.when(authService.hasAccess(Mockito.any(), Mockito.any())).thenReturn(true);
        resourceService = new MockDefaultResourceService(authService);

        // Instanciate the tested class
        templateController = new TemplateController(templateService, resourceService);
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.templates.rest.TemplateController#findAll()}.
     */
    @Test
    public final void testFindAll() {
        // Mock service
        final List<Template> templates = Arrays.asList(template);
        Mockito.when(templateService.findAll()).thenReturn(templates);

        // Define actual
        final ResponseEntity<List<Resource<Template>>> actual = templateController.findAll();

        // Check
        Assert.assertEquals(template.getCode(), actual.getBody().get(0).getContent().getCode());
        Assert.assertEquals(template.getContent(), actual.getBody().get(0).getContent().getContent());
        Assert.assertEquals(template.getData(), actual.getBody().get(0).getContent().getData());
        Assert.assertEquals(template.getDescription(), actual.getBody().get(0).getContent().getDescription());
        Mockito.verify(templateService).findAll();
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.templates.rest.TemplateController#create(fr.cnes.regards.modules.templates.domain.Template)}.
     */
    @Test
    public final void testCreate() {
        // Mock service
        Mockito.when(templateService.create(Mockito.any())).thenReturn(template);

        // Define actual
        final ResponseEntity<Resource<Template>> actual = templateController.create(template);

        // Check
        Mockito.verify(templateService).create(Mockito.refEq(template, "id"));
        Assert.assertEquals(template.getCode(), actual.getBody().getContent().getCode());
        Assert.assertEquals(template.getContent(), actual.getBody().getContent().getContent());
        Assert.assertEquals(template.getData(), actual.getBody().getContent().getData());
        Assert.assertEquals(template.getDescription(), actual.getBody().getContent().getDescription());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.templates.rest.TemplateController#findById(java.lang.Long)}.
     */
    @Test
    public final void testFindById() throws EntityNotFoundException {
        // Mock service
        Mockito.when(templateService.findById(TemplateTestConstants.ID)).thenReturn(template);

        // Define actual
        final ResponseEntity<Resource<Template>> actual = templateController.findById(TemplateTestConstants.ID);

        // Check
        Assert.assertEquals(template.getCode(), actual.getBody().getContent().getCode());
        Assert.assertEquals(template.getDescription(), actual.getBody().getContent().getDescription());
        Assert.assertEquals(template.getData(), actual.getBody().getContent().getData());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.templates.rest.TemplateController#findById(java.lang.Long)}.
     */
    @SuppressWarnings("unchecked")
    @Test(expected = EntityNotFoundException.class)
    public final void testFindByIdNotFound() throws EntityNotFoundException {
        // Mock service
        Mockito.when(templateService.findById(TemplateTestConstants.ID)).thenThrow(EntityNotFoundException.class);

        // Trigger exception
        templateController.findById(TemplateTestConstants.ID);
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.templates.rest.TemplateController#update(java.lang.Long, fr.cnes.regards.modules.templates.domain.Template)}.
     *
     * @throws EntityException
     *             <br>
     *             {@link EntityNotFoundException} if no template with passed id could be found<br>
     *             {@link EntityInconsistentIdentifierException} if the path id differs from the template id<br>
     */
    @Test
    public final void testUpdate() throws EntityException {
        // Call tested method
        templateController.update(TemplateTestConstants.ID, template);

        // Check that the repository's method was called with right arguments
        Mockito.verify(templateService).update(Mockito.anyLong(), Mockito.refEq(template));

    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.templates.rest.TemplateController#update(java.lang.Long, fr.cnes.regards.modules.templates.domain.Template)}.
     *
     * @throws EntityException
     *             <br>
     *             {@link EntityNotFoundException} if no template with passed id could be found<br>
     *             {@link EntityInconsistentIdentifierException} if the path id differs from the template id<br>
     */
    @Test(expected = EntityNotFoundException.class)
    public final void testUpdateNotFound() throws EntityException {
        // Mock
        Mockito.doThrow(EntityNotFoundException.class).when(templateService).update(TemplateTestConstants.ID, template);

        // Trigger exception
        templateController.update(TemplateTestConstants.ID, template);
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.templates.rest.TemplateController#update(java.lang.Long, fr.cnes.regards.modules.templates.domain.Template)}.
     *
     * @throws EntityException
     *             <br>
     *             {@link EntityNotFoundException} if no template with passed id could be found<br>
     *             {@link EntityInconsistentIdentifierException} if the path id differs from the template id<br>
     */
    @Test(expected = EntityInconsistentIdentifierException.class)
    public final void testUpdateInconsistentIdentifier() throws EntityException {
        // Mock
        Mockito.doThrow(EntityInconsistentIdentifierException.class).when(templateService)
                .update(TemplateTestConstants.ID, template);

        // Trigger exception
        templateController.update(TemplateTestConstants.ID, template);
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.templates.rest.TemplateController#delete(java.lang.Long)}.
     *
     * @throws EntityNotFoundException
     *             if no template with passed id could be found
     */
    @Test
    public final void testDelete() throws EntityNotFoundException {
        // Call tested method
        templateController.delete(TemplateTestConstants.ID);

        // Check
        Mockito.verify(templateService).delete(TemplateTestConstants.ID);
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.templates.rest.TemplateController#delete(java.lang.Long)}.
     *
     * @throws EntityNotFoundException
     *             if no template with passed id could be found
     */
    @Test(expected = EntityNotFoundException.class)
    public final void testDeleteNotFound() throws EntityNotFoundException {
        // Mock
        Mockito.doThrow(EntityNotFoundException.class).when(templateService).delete(TemplateTestConstants.ID);

        // Trigger exception
        templateController.delete(TemplateTestConstants.ID);
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.templates.rest.TemplateController#toResource(fr.cnes.regards.modules.templates.domain.Template, java.lang.Object[])}.
     */
    @Test
    public final void testToResource() {
        // Define expected
        template.setId(TemplateTestConstants.ID);
        final List<Link> links = new ArrayList<>();
        links.add(new Link("/templates/" + TemplateTestConstants.ID, "self"));
        links.add(new Link("/templates/" + TemplateTestConstants.ID, "delete"));
        links.add(new Link("/templates/" + TemplateTestConstants.ID, "update"));
        links.add(new Link("/templates", "create"));
        final Resource<Template> expected = new Resource<>(template, links);

        // Define actual
        final Resource<Template> actual = templateController.toResource(template);

        // Check
        Assert.assertEquals(expected.getContent().getId(), actual.getContent().getId());
        Assert.assertEquals(expected.getContent().getCode(), actual.getContent().getCode());
        Assert.assertEquals(expected.getContent().getContent(), actual.getContent().getContent());
        Assert.assertEquals(expected.getContent().getData(), actual.getContent().getData());
        Assert.assertEquals(expected.getContent().getDescription(), actual.getContent().getDescription());
        Assert.assertEquals(expected.getLinks(), actual.getLinks());
    }

}
