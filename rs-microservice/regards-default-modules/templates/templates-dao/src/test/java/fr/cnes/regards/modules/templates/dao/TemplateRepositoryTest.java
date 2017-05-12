/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.templates.dao;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTransactionalTest;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.templates.domain.Template;
import fr.cnes.regards.modules.templates.test.TemplateTestConstants;

/**
 * Test class for {@link Template} DAO module
 *
 * @author Xavier-Alexandre Brochard
 */
@TestPropertySource(locations = "classpath:application-test.properties")
public class TemplateRepositoryTest extends AbstractDaoTransactionalTest {

    /**
     * A template with some values
     */
    private final Template template = new Template(TemplateTestConstants.CODE, TemplateTestConstants.CONTENT,
            TemplateTestConstants.DATA, TemplateTestConstants.SUBJECT);

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

    @Test
    @Requirement("REGARDS_DSL_SYS_ERG_310")
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_460")
    @Purpose("Check the data map is properly stored and recovered.")
    public final void testDataMapStorage() {
        // Create the template
        templateRepository.save(template);

        // Check
        Assert.assertEquals(template.getDataStructure().get(TemplateTestConstants.DATA_KEY_0),
                            TemplateTestConstants.DATA_VALUE_0);
        Assert.assertEquals(template.getDataStructure().get(TemplateTestConstants.DATA_KEY_1),
                            TemplateTestConstants.DATA_VALUE_1);
        Assert.assertEquals(template.getDataStructure().get(TemplateTestConstants.DATA_KEY_2),
                            TemplateTestConstants.DATA_VALUE_2);
    }

}
