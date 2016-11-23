/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.templates.rest;

import java.util.List;

import javax.validation.Valid;

import org.springframework.hateoas.Resource;
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
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.templates.domain.Template;

@RestController
@ModuleInfo(name = "templates", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
@RequestMapping("/templates")
public class TemplatesController implements IResourceController<Template> {

    /**
     * Resource service to manage visible hateoas links
     */
    private final IResourceService resourceService;

    /**
     * @param pResourceService
     */
    public TemplatesController(final IResourceService pResourceService) {
        super();
        resourceService = pResourceService;
    }

    /**
     * @return the list of templates
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    ResponseEntity<List<Resource<Template>>> findAll() {
        return null;
    }

    /**
     * Creates a template
     *
     * @param pTemplate
     *            the template
     * @return the created template
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.POST)
    ResponseEntity<Resource<Template>> create(@Valid @RequestBody final Template pTemplate) {
        return null;
    }

    /**
     * @param pId
     *            the retrieved template id
     * @throws EntityNotFoundException
     *             if no template with passed id could be found
     * @return the template of passed id
     */
    @ResponseBody
    @RequestMapping(value = "/{template_id}", method = RequestMethod.GET)
    ResponseEntity<Resource<Template>> findById(@PathVariable("template_id") final Long pId)
            throws EntityNotFoundException {
        return null;
    }

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
    @ResponseBody
    @RequestMapping(value = "/{template_id}", method = RequestMethod.PUT)
    ResponseEntity<Void> update(@PathVariable("template_id") final Long pId,
            @Valid @RequestBody final Template pTemplate) throws EntityException {
        return null;
    }

    /**
     * Deletes the template of passed id
     *
     * @param pId
     *            the updated template id
     * @return void
     * @throws EntityNotFoundException
     *             if no template with passed id could be found
     */
    @ResponseBody
    @RequestMapping(value = "/{template_id}", method = RequestMethod.DELETE)
    ResponseEntity<Void> delete(@PathVariable("template_id") final Long pId) throws EntityNotFoundException {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.framework.hateoas.IResourceController#toResource(java.lang.Object, java.lang.Object[])
     */
    @Override
    public Resource<Template> toResource(final Template pElement, final Object... pExtras) {
        final Resource<Template> resource = resourceService.toResource(pElement);
        resourceService.addLink(resource, getClass(), "findById", LinkRels.SELF,
                                MethodParamFactory.build(Long.class, pElement.getId()));
        resourceService.addLink(resource, getClass(), "delete", LinkRels.DELETE,
                                MethodParamFactory.build(Long.class, pElement.getId()));
        resourceService.addLink(resource, getClass(), "update", LinkRels.UPDATE,
                                MethodParamFactory.build(Long.class, pElement.getId()),
                                MethodParamFactory.build(Template.class, pElement));
        resourceService.addLink(resource, getClass(), "create", LinkRels.CREATE,
                                MethodParamFactory.build(Template.class, pElement));
        return resource;
    }

}
