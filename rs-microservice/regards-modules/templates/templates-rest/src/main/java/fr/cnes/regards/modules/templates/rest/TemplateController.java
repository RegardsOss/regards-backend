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
package fr.cnes.regards.modules.templates.rest;

import java.util.List;

import javax.validation.Valid;

import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.templates.domain.Template;
import fr.cnes.regards.modules.templates.service.ITemplateService;

/**
 * Rest controller defining endpoint for managing {@link Template}s.
 * @author Xavier-Alexandre Brochard
 */
@RestController
@RequestMapping("/templates")
public class TemplateController implements IResourceController<Template> {

    /**
     * Template service
     */
    private final ITemplateService templateService;

    /**
     * Resource service to manage visible hateoas links
     */
    private final IResourceService resourceService;

    /**
     * Constructor
     * @param pTemplateService the template service
     * @param pResourceService the resource service
     */
    public TemplateController(final ITemplateService pTemplateService, final IResourceService pResourceService) {
        super();
        templateService = pTemplateService;
        resourceService = pResourceService;
    }

    /**
     * @return the list of templates
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieves all email templates of the current project")
    public ResponseEntity<List<Resource<Template>>> findAll() {
        final List<Template> templates = templateService.findAll();
        return new ResponseEntity<>(toResources(templates), HttpStatus.OK);
    }

    /**
     * @param pId the retrieved template id
     * @return the template of passed id
     * @throws EntityNotFoundException if no template with passed id could be found
     */
    @ResponseBody
    @RequestMapping(value = "/{template_id}", method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieves the email template of given id")
    public ResponseEntity<Resource<Template>> findById(@PathVariable("template_id") final Long pId)
            throws EntityNotFoundException {
        final Template template = templateService.findById(pId);
        return new ResponseEntity<>(toResource(template), HttpStatus.OK);
    }

    /**
     * Updates the template of passed id
     * @param id the updated template id
     * @param template the updated template
     * @return void
     * @throws EntityException <br>
     *                         {@link EntityNotFoundException} if no template with passed id could be found<br>
     *                         {@link EntityInconsistentIdentifierException} if the path id differs from the template id<br>
     */
    @ResponseBody
    @RequestMapping(value = "/{template_id}", method = RequestMethod.PUT)
    @ResourceAccess(description = "Update the email template with given id with given values")
    public ResponseEntity<Void> update(@PathVariable("template_id") final Long id,
            @Valid @RequestBody final Template template) throws EntityException {
        templateService.update(id, template);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.framework.hateoas.IResourceController#toResource(java.lang.Object, java.lang.Object[])
     */
    @Override
    public Resource<Template> toResource(final Template element, final Object... extras) {
        final Resource<Template> resource = resourceService.toResource(element);
        resourceService.addLink(resource, getClass(), "findById", LinkRels.SELF,
                                MethodParamFactory.build(Long.class, element.getId()));
        resourceService.addLink(resource, getClass(), "update", LinkRels.UPDATE,
                                MethodParamFactory.build(Long.class, element.getId()),
                                MethodParamFactory.build(Template.class, element));
        return resource;
    }

}
