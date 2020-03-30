/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.templates.dao;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTransactionalTest;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.templates.domain.Template;

/**
 * Test class for {@link Template} DAO module
 * @author Xavier-Alexandre Brochard
 */
@TestPropertySource(locations = "classpath:application-test.properties")
public class TemplateRepositoryTest extends AbstractDaoTransactionalTest {

    /**
     * A template with some values
     */
    private final Template template = new Template(TemplateTestConstants.CODE, TemplateTestConstants.CONTENT);

    /**
     * The template repository
     */
    @Autowired
    private ITemplateRepository templateRepository;

    @Test
    @Requirement("REGARDS_DSL_SYS_ERG_310")
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_460")
    @Purpose("Quick JPA check. If no error is thrown, the persistance is likely to be correct.")
    public final void testSaveNew() {
        templateRepository.save(template);
    }

}
