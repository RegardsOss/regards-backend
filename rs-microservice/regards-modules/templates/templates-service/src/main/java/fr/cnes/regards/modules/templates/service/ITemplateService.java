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
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEvent;
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

    /**
     * @return the list of templates
     */
    List<Template> findAll();

    /**
     * @param id the retrieved template id
     * @return the template of given id
     * @throws EntityNotFoundException if no template with given id could be found
     */
    Template findById(final Long id) throws EntityNotFoundException;

    /**
     * Update the template of given id
     * @param id the updated template id
     * @param template the updated template
     * @throws EntityException <br>
     *                         {@link EntityNotFoundException} if no template with given id could be found<br>
     *                         {@link EntityInconsistentIdentifierException} if the path id differs from the template id<br>
     */
    Template update(final Long id, final Template template) throws EntityException;

    /**
     * Render the template found by name, from latest value found in database
     * @param templateName template name
     * @param dataModel data model used as dynamic values for rendering
     * @return rendered template
     * @throws EntityNotFoundException
     */
    String render(String templateName, Map<String, ?> dataModel) throws EntityNotFoundException;

}
