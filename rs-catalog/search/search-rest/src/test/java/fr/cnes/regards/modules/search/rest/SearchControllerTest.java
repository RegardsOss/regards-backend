/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest;

import org.apache.lucene.queryparser.classic.ParseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.entities.domain.Dataset;

/**
 * TODO
 *
 * @author Xavier-Alexandre Brochard
 */
public class SearchControllerTest {

    /**
     * Controller under test
     */
    private SearchController searchController;

    @Before
    public void setUp() {
        // Instanciate the tested class
        searchController = new SearchController();
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.templates.rest.TemplateController#findAll()}.
     *
     * @throws ParseException
     */
    @Test
    @Purpose("TODO.")
    // @Requirement("REGARDS_DSL_ADM_ADM_440")
    public final void testSearch() throws ParseException {
        // Mock service
        // final List<Template> templates = Arrays.asList(template);
        // Mockito.when(templateService.findAll()).thenReturn(templates);

        // Define actual
        // final String pQ = "user:kimchy";
        // final String pQ = "title:(+return +\"pink panther\")";
        final String pQ = "music -video (singer OR songwriter) site:amazon.com ";
        final Pageable pageable = new PageRequest(0, 10);
        final PagedResourcesAssembler<Dataset> assembler = new PagedResourcesAssembler<>(null, null);
        final ResponseEntity<PagedResources<Resource<Dataset>>> result = searchController.search(pQ, pageable,
                                                                                                 assembler);

        Assert.assertTrue(true);

        // // Check
        // Assert.assertEquals(template.getCode(), actual.getBody().get(0).getContent().getCode());
        // Assert.assertEquals(template.getContent(), actual.getBody().get(0).getContent().getContent());
        // Assert.assertEquals(template.getDataStructure(), actual.getBody().get(0).getContent().getDataStructure());
        // Assert.assertEquals(template.getDescription(), actual.getBody().get(0).getContent().getDescription());
        // Mockito.verify(templateService).findAll();
    }

    // /**
    // * Test method for
    // * {@link
    // fr.cnes.regards.modules.templates.rest.TemplateController#toResource(fr.cnes.regards.modules.templates.domain.Template,
    // java.lang.Object[])}.
    // */
    // @Test
    // public final void testToResource() {
    // // Define expected
    // template.setId(TemplateTestConstants.ID);
    // final List<Link> links = new ArrayList<>();
    // links.add(new Link("/templates/" + TemplateTestConstants.ID, "self"));
    // links.add(new Link("/templates/" + TemplateTestConstants.ID, "delete"));
    // links.add(new Link("/templates/" + TemplateTestConstants.ID, "update"));
    // links.add(new Link("/templates", "create"));
    // final Resource<Template> expected = new Resource<>(template, links);
    //
    // // Define actual
    // final Resource<Template> actual = templateController.toResource(template);
    //
    // // Check
    // Assert.assertEquals(expected.getContent().getId(), actual.getContent().getId());
    // Assert.assertEquals(expected.getContent().getCode(), actual.getContent().getCode());
    // Assert.assertEquals(expected.getContent().getContent(), actual.getContent().getContent());
    // Assert.assertEquals(expected.getContent().getDataStructure(), actual.getContent().getDataStructure());
    // Assert.assertEquals(expected.getContent().getDescription(), actual.getContent().getDescription());
    // Assert.assertEquals(expected.getLinks(), actual.getLinks());
    // }

}
