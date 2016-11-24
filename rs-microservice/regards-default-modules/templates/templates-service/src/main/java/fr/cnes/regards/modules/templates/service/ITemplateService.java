/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.templates.service;

import java.util.List;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.templates.domain.Template;

/**
 * Define the base interface for any implementation of a Template Service.
 *
 * @author Xavier-Alexandre Brochard
 */
public interface ITemplateService {

    /**
     * @return the list of templates
     */
    List<Template> findAll();

    /**
     * Creates a template
     *
     * @param pTemplate
     *            the template
     * @return the created template
     */
    Template create(final Template pTemplate);

    /**
     * @param pId
     *            the retrieved template id
     * @throws EntityNotFoundException
     *             if no template with passed id could be found
     * @return the template of passed id
     */
    Template findById(final Long pId) throws EntityNotFoundException;

    /**
     * Updates the template of passed id
     *
     * @param pId
     *            the updated template id
     * @param pTemplate
     *            the updated template
     * @throws EntityException
     *             <br>
     *             {@link EntityNotFoundException} if no template with passed id could be found<br>
     *             {@link EntityInconsistentIdentifierException} if the path id differs from the template id<br>
     * @return void
     */
    void update(final Long pId, final Template pTemplate) throws EntityException;

    /**
     * Deletes the template of passed id
     *
     * @param pId
     *            the updated template id
     * @throws EntityNotFoundException
     *             if no template with passed id could be found
     * @return void
     */
    void delete(final Long pId) throws EntityNotFoundException;
}
