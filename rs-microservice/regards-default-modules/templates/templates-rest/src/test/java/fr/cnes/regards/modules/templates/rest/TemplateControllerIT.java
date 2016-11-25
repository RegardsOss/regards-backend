/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.templates.rest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.Transactional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.templates.dao.ITemplateRepository;
import fr.cnes.regards.modules.templates.domain.Template;
import fr.cnes.regards.modules.templates.test.TemplateTestConstants;

/**
 * Templates integration test
 *
 * @author Xavier-Alexandre Brochard
 */
@Transactional
public class TemplateControllerIT extends AbstractRegardsTransactionalIT {

    /**
     * A template with some values
     */
    private Template template;

    /**
     * The template repository
     */
    @Autowired
    private ITemplateRepository templateRepository;

    @Before
    public void setUp() {
        template = new Template(TemplateTestConstants.CODE, TemplateTestConstants.CONTENT, TemplateTestConstants.DATA);
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.templates.rest.TemplateController#findAll()}.
     */
    @Test
    @Purpose("Check that the system allows to retrieve the list of all templates.")
    @Requirement("REGARDS_DSL_SYS_ERG_310")
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_460")
    public final void testFindAll() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isOk());
        performDefaultGet(TemplateTestConstants.API_TEMPLATES, expectations, "Unable to retrieve the template.");
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.templates.rest.TemplateController#create(fr.cnes.regards.modules.templates.domain.Template)}.
     */
    @Test
    @Purpose("Check that the system allows to create templates.")
    @Requirement("REGARDS_DSL_SYS_ERG_310")
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_460")
    public final void testCreate() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isCreated());
        performDefaultPost(TemplateTestConstants.API_TEMPLATES, template, expectations,
                           "Unable to create a new template.");
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.templates.rest.TemplateController#findById(java.lang.Long)}.
     */
    @Test
    @Purpose("Check that the system allows to retrieve a single template.")
    @Requirement("REGARDS_DSL_SYS_ERG_310")
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_460")
    public final void testFindById() {
        // Prepare
        templateRepository.save(template);

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultGet(TemplateTestConstants.API_TEMPLATES_TEMPLATE_ID, expectations,
                          "Unable to retrieve the template.", template.getId());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.templates.rest.TemplateController#findById(java.lang.Long)}.
     */
    @Test
    @Purpose("Check that the system handles the case where trying to retrieve a template of unknown id.")
    @Requirement("REGARDS_DSL_SYS_ERG_310")
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_460")
    public final void testFindByIdNotFound() {
        Assert.assertFalse(templateRepository.exists(TemplateTestConstants.WRONG_ID));

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isNotFound());
        performDefaultGet(TemplateTestConstants.API_TEMPLATES_TEMPLATE_ID, expectations,
                          "Unable to retrieve the template.", TemplateTestConstants.WRONG_ID);
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.templates.rest.TemplateController#update(java.lang.Long, fr.cnes.regards.modules.templates.domain.Template)}.
     */
    @Test
    @Purpose("Check that the system allows to update a template.")
    @Requirement("REGARDS_DSL_SYS_ERG_310")
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_460")
    public final void testUpdate() {
        // Prepare
        templateRepository.save(template);

        // Change stuff
        template.setDescription(TemplateTestConstants.DESCRIPTON);

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isOk());
        performDefaultPut(TemplateTestConstants.API_TEMPLATES_TEMPLATE_ID, template, expectations,
                          "Unable to update the template.", template.getId());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.templates.rest.TemplateController#update(java.lang.Long, fr.cnes.regards.modules.templates.domain.Template)}.
     */
    @Test
    @Purpose("Check that the system handles the case of updating an not existing template.")
    @Requirement("REGARDS_DSL_SYS_ERG_310")
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_460")
    public final void testUpdateNotFound() {
        // Set inexistent id
        template.setId(TemplateTestConstants.WRONG_ID);

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isNotFound());
        performDefaultPut(TemplateTestConstants.API_TEMPLATES_TEMPLATE_ID, template, expectations,
                          "Unable to update the template.", template.getId());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.templates.rest.TemplateController#update(java.lang.Long, fr.cnes.regards.modules.templates.domain.Template)}.
     */
    @Test
    @Purpose("Check that the system allows the case of inconsistency of ids in the request.")
    @Requirement("REGARDS_DSL_SYS_ERG_310")
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_460")
    public final void testUpdateInconsistentIdentifier() {
        // Set inexistent id
        template.setId(TemplateTestConstants.ID);

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isBadRequest());
        performDefaultPut(TemplateTestConstants.API_TEMPLATES_TEMPLATE_ID, template, expectations,
                          "Unable to update the template.", TemplateTestConstants.WRONG_ID);
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.templates.rest.TemplateController#delete(java.lang.Long)}.
     */
    @Test
    @Purpose("Check that the system allows to delete a single template.")
    @Requirement("REGARDS_DSL_SYS_ERG_310")
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_460")
    public final void testDelete() {
        // Prepare
        templateRepository.save(template);

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isOk());
        performDefaultDelete(TemplateTestConstants.API_TEMPLATES_TEMPLATE_ID, expectations,
                             "Unable to delete the template", template.getId());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.templates.rest.TemplateController#delete(java.lang.Long)}.
     */
    @Test
    @Purpose("Check that the system handles the case of deleting an inexistent template.")
    @Requirement("REGARDS_DSL_SYS_ERG_310")
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_460")
    public final void testDeleteNotFound() {
        // Set inexistent id
        template.setId(TemplateTestConstants.WRONG_ID);

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isNotFound());
        performDefaultDelete(TemplateTestConstants.API_TEMPLATES_TEMPLATE_ID, expectations,
                             "Unable to update the template.", template.getId());
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.framework.test.integration.AbstractRegardsIT#getLogger()
     */
    @Override
    protected Logger getLogger() {
        // TODO Auto-generated method stub
        return null;
    }

}
