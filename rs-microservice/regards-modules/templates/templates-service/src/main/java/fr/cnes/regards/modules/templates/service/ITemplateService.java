/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.templates.service;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.mail.SimpleMailMessage;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.templates.domain.Template;

/**
 * Define the base interface for any implementation of a Template Service.
 * @author Xavier-Alexandre Brochard
 */
public interface ITemplateService {

    void init(ApplicationReadyEvent event);

    /**
     * @return the list of templates
     */
    List<Template> findAll();

    /**
     * Create a template
     * @param pTemplate the template
     * @return the created template
     */
    Template create(final Template pTemplate);

    /**
     * @param pId the retrieved template id
     * @return the template of given id
     * @throws EntityNotFoundException if no template with given id could be found
     */
    Template findById(final Long pId) throws EntityNotFoundException;

    /**
     * Update the template of given id
     * @param pId the updated template id
     * @param pTemplate the updated template
     * @throws EntityException <br>
     *                         {@link EntityNotFoundException} if no template with given id could be found<br>
     *                         {@link EntityInconsistentIdentifierException} if the path id differs from the template id<br>
     */
    void update(final Long pId, final Template pTemplate) throws EntityException;

    /**
     * Delete the template of given id
     * @param pId the updated template id
     * @throws EntityNotFoundException if no template with given id could be found
     */
    void delete(final Long pId) throws EntityNotFoundException;

    /**
     * Delete all templates
     */
    void deleteAll();

    /**
     * @param templateCode the code of the template
     * @param subject email subject (can be null, in this case template one is used)
     * @param dataModel the data to bind into to template
     * @param recipients the array of recipients
     * @return the mail
     * @throws EntityNotFoundException when a {@link Template} of given <code>code</code> could not be found
     */
    SimpleMailMessage writeToEmail(String templateCode, String subject, Map<String, ?> dataModel,
            String... recipients) throws EntityNotFoundException;

    /**
     * Write email with default subject
     * @param templateCode the code of the template
     * @param dataModel the data to bind into to template
     * @param recipients the array of recipients
     * @return the mail
     * @throws EntityNotFoundException when a {@link Template} of given <code>code</code> could not be found
     */
    default SimpleMailMessage writeToEmail(String templateCode, Map<String, ?> dataModel,
            String... recipients) throws EntityNotFoundException {
        return writeToEmail(templateCode, null, dataModel, recipients);
    }

}
