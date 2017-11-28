/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.templates.rest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
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
@RegardsTransactional
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

    /**
     * Tenant resolver to access all configured tenant
     */
    private ITenantResolver tenantResolver;

    @Before
    public void setUp() {
        final Set<String> tenants = new HashSet<>();
        tenants.add("PROJECT");
        tenantResolver = Mockito.mock(ITenantResolver.class);
        Mockito.when(tenantResolver.getAllTenants()).thenReturn(tenants);

        template = new Template(TemplateTestConstants.CODE, TemplateTestConstants.CONTENT, TemplateTestConstants.DATA,
                TemplateTestConstants.SUBJECT);
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
        expectations.add(MockMvcResultMatchers.status().isOk());
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
        expectations.add(MockMvcResultMatchers.status().isOk());
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
        expectations.add(MockMvcResultMatchers.status().isNotFound());
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
        expectations.add(MockMvcResultMatchers.status().isBadRequest());
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
        expectations.add(MockMvcResultMatchers.status().isOk());
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
        expectations.add(MockMvcResultMatchers.status().isNotFound());
        performDefaultDelete(TemplateTestConstants.API_TEMPLATES_TEMPLATE_ID, expectations,
                             "Unable to update the template.", template.getId());
    }
}
