/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.templates.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mail.SimpleMailMessage;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.templates.dao.ITemplateRepository;
import fr.cnes.regards.modules.templates.domain.Template;
import fr.cnes.regards.modules.templates.test.TemplateTestConstants;

/**
 * Test suite for {@link TemplateService}.
 *
 * @author Xavier-Alexandre Brochard
 */
public class TemplateServiceTest {

    /**
     * A template
     */
    private static Template template;

    /**
     * A template id
     */
    private static final Long ID = 0L;

    /**
     * Tested service
     */
    private ITemplateService templateService;

    /**
     * Mocked CRUD repository managing {@link Template}s
     */
    private ITemplateRepository templateRepository;

    @Before
    public void setUp() throws IOException {
        template = new Template(TemplateTestConstants.CODE, TemplateTestConstants.CONTENT, TemplateTestConstants.DATA,
                TemplateTestConstants.SUBJECT);
        templateRepository = Mockito.mock(ITemplateRepository.class);

        templateService = new TemplateService(templateRepository);
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.templates.service.TemplateService#findAll()}.
     */
    @Test
    @Purpose("Check that the system allows to retrieve the list of all templates.")
    @Requirement("REGARDS_DSL_SYS_ERG_310")
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_460")
    public final void testFindAll() {
        // Define expected
        final List<Template> expected = Arrays.asList(template, new Template(), new Template());

        // Mock
        Mockito.when(templateRepository.findAll()).thenReturn(expected);

        // Define actual
        final List<Template> actual = templateService.findAll();

        // Check
        Assert.assertThat(actual, CoreMatchers.is(CoreMatchers.equalTo(expected)));
    }

    /**
     * Test method for {@link TemplateService#create(Template)}.
     */
    @Test
    @Purpose("Check that the system allows to create templates.")
    @Requirement("REGARDS_DSL_SYS_ERG_310")
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_460")
    public final void testCreate() {
        // Mock
        Mockito.when(templateRepository.findOne(ID)).thenReturn(null);
        Mockito.when(templateRepository.exists(ID)).thenReturn(false);

        // Call tested method
        templateService.create(template);

        // Check
        Mockito.verify(templateRepository).save(Mockito.refEq(template, "id"));
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.templates.service.TemplateService#findById(Long)}.
     *
     * @throws EntityNotFoundException
     *             if no template with passed id could be found
     */
    @Test
    @Purpose("Check that the system allows to retrieve a single template.")
    @Requirement("REGARDS_DSL_SYS_ERG_310")
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_460")
    public final void testFindById() throws EntityNotFoundException {
        // Mock
        Mockito.when(templateRepository.findOne(ID)).thenReturn(template);
        Mockito.when(templateRepository.exists(ID)).thenReturn(true);

        // Trigger expected exception
        final Template actual = templateService.findById(ID);

        // Check
        Assert.assertThat(actual, CoreMatchers.is(CoreMatchers.equalTo(template)));
        Mockito.verify(templateRepository).findOne(ID);
    }

    /**
     * Test method for {@link TemplateService#findById(java.lang.Long)}.
     *
     * @throws EntityNotFoundException
     *             if no template with passed id could be found
     */
    @Test(expected = EntityNotFoundException.class)
    @Purpose("Check that the system handles the case where trying to retrieve a template of unknown id.")
    @Requirement("REGARDS_DSL_SYS_ERG_310")
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_460")
    public final void testFindByIdNotFound() throws EntityNotFoundException {
        // Mock
        Mockito.when(templateRepository.findOne(ID)).thenReturn(null);
        Mockito.when(templateRepository.exists(ID)).thenReturn(false);

        // Trigger expected exception
        templateService.findById(ID);
    }

    /**
     * Test method for {@link TemplateService#update(Long, Template)}.
     *
     * @throws EntityException
     *             <br>
     *             {@link EntityNotFoundException} if no template with passed id could be found<br>
     *             {@link EntityInconsistentIdentifierException} if the path id differs from the template id<br>
     */
    @Test
    @Purpose("Check that the system allows to update a template.")
    @Requirement("REGARDS_DSL_SYS_ERG_310")
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_460")
    public final void testUpdate() throws EntityException {
        // Prepare the case
        template.setId(ID);
        template.setDescription("Updated description");

        // Mock
        Mockito.when(templateRepository.findOne(ID)).thenReturn(template);
        Mockito.when(templateRepository.exists(ID)).thenReturn(true);

        // Call tested method
        templateService.update(ID, template);

        // Check
        Mockito.verify(templateRepository).save(Mockito.refEq(template));
    }

    /**
     * Test method for {@link TemplateService#update(Long, Template)}.
     *
     * @throws EntityException
     *             <br>
     *             {@link EntityNotFoundException} if no template with passed id could be found<br>
     *             {@link EntityInconsistentIdentifierException} if the path id differs from the template id<br>
     */
    @Test(expected = EntityNotFoundException.class)
    @Purpose("Check that the system handles the case of updating an not existing template.")
    @Requirement("REGARDS_DSL_SYS_ERG_310")
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_460")
    public final void testUpdateNotFound() throws EntityException {
        // Prepare the case
        template.setId(ID);

        // Mock
        Mockito.when(templateRepository.findOne(ID)).thenReturn(null);
        Mockito.when(templateRepository.exists(ID)).thenReturn(false);

        // Trigger expected exception
        templateService.update(ID, template);
    }

    /**
     * Test method for {@link TemplateService#update(Long, Template)}.
     *
     * @throws EntityException
     *             <br>
     *             {@link EntityNotFoundException} if no template with passed id could be found<br>
     *             {@link EntityInconsistentIdentifierException} if the path id differs from the template id<br>
     */
    @Test(expected = EntityInconsistentIdentifierException.class)
    @Purpose("Check that the system allows the case of inconsistency of ids in the request.")
    @Requirement("REGARDS_DSL_SYS_ERG_310")
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_460")
    public final void testUpdateInconsistentIdentifier() throws EntityException {
        // Prepare the case
        template.setId(ID);

        // Mock
        Mockito.when(templateRepository.findOne(ID)).thenReturn(template);
        Mockito.when(templateRepository.exists(ID)).thenReturn(true);

        // Trigger expected exception
        templateService.update(1L, template);
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.templates.service.TemplateService#delete(java.lang.Long)}.
     *
     * @throws EntityNotFoundException
     *             if no template with passed id could be found
     */
    @Test
    @Purpose("Check that the system allows to delete a single template.")
    @Requirement("REGARDS_DSL_SYS_ERG_310")
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_460")
    public final void testDelete() throws EntityNotFoundException {
        // Mock
        Mockito.when(templateRepository.exists(ID)).thenReturn(true);

        // Call tested method
        templateService.delete(ID);

        // Check
        Mockito.verify(templateRepository).delete(ID);
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.templates.service.TemplateService#delete(java.lang.Long)}.
     *
     * @throws EntityNotFoundException
     *             if no template with passed id could be found
     */
    @Test(expected = EntityNotFoundException.class)
    @Purpose("Check that the system handles the case of deleting an inexistent template.")
    @Requirement("REGARDS_DSL_SYS_ERG_310")
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_460")
    public final void testDeleteNotFound() throws EntityNotFoundException {
        // Mock
        Mockito.when(templateRepository.exists(ID)).thenReturn(false);

        // Trigger expected exception
        templateService.delete(ID);
    }

    /**
     * Test method for {@link SimpleMailMessageTemplateWriter#writeToEmail(Template, Map, String[])}.
     *
     * @throws TemplateWriterException
     *             todo
     */
    @Test
    @Purpose("Check that the system uses templates to send emails.")
    @Requirement("REGARDS_DSL_SYS_ERG_310")
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_460")
    public final void testWrite() throws TemplateWriterException {
        // Mock
        Mockito.when(templateRepository.findOneByCode(TemplateTestConstants.CODE))
                .thenReturn(Optional.ofNullable(template));

        // Define expected
        final String expectedSubject = TemplateTestConstants.SUBJECT;
        final String expectedText = "Hello Defaultname. You are 26 years old and 1.79 m tall.";

        // Define actual
        final SimpleMailMessage message = templateService
                .writeToEmail(TemplateTestConstants.CODE, TemplateTestConstants.DATA, TemplateTestConstants.RECIPIENTS);

        // Check
        Assert.assertEquals(expectedSubject, message.getSubject());
        Assert.assertEquals(expectedText, message.getText());
        Assert.assertArrayEquals(TemplateTestConstants.RECIPIENTS, message.getTo());
    }

}
