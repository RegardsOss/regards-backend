/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.rest;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.configuration.domain.Plugin;
import fr.cnes.regards.modules.configuration.service.IPluginService;

/**
 * REST controller for the microservice Access
 *
 * @author Sébastien Binda
 *
 */
@RestController
@ModuleInfo(name = "Plugin", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
@RequestMapping("/plugins")
public class PluginController implements IResourceController<Plugin> {

    @Autowired
    private IPluginService service;

    @Autowired
    private IResourceService resourceService;

    /**
     * Entry point to retrieve a plugins {@link Plugin}.
     *
     * @return {@link Plugin}
     * @throws EntityNotFoundException
     */
    @RequestMapping(value = "/{pluginId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve an IHM plugin", role = DefaultRole.PUBLIC)
    public HttpEntity<Resource<Plugin>> retrievePlugin(@PathVariable("pluginId") final Long pPluginId)
            throws EntityNotFoundException {
        final Plugin plugin = service.retrievePlugin(pPluginId);
        final Resource<Plugin> resource = toResource(plugin);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Entry point to retrieve all plugins
     *
     * @return {@link Plugin}
     * @throws EntityNotFoundException
     */
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve IHM plugins", role = DefaultRole.PUBLIC)
    public HttpEntity<PagedResources<Resource<Plugin>>> retrievePlugins(final Pageable pPageable,
            final PagedResourcesAssembler<Plugin> pAssembler) {
        final Page<Plugin> plugins = service.retrievePlugins(pPageable);
        final PagedResources<Resource<Plugin>> resources = toPagedResources(plugins, pAssembler);
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    /**
     * Entry point to save a new plugin
     *
     * @return {@link Plugin}
     * @throws EntityInvalidException
     * @throws EntityNotFoundException
     */
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to save a new IHM plugin", role = DefaultRole.PROJECT_ADMIN)
    public HttpEntity<Resource<Plugin>> savePlugin(@Valid @RequestBody final Plugin pPlugin)
            throws EntityInvalidException {
        final Plugin plugin = service.savePlugin(pPlugin);
        final Resource<Plugin> resource = toResource(plugin);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Entry point to save a new ihm plugin.
     *
     * @return {@link Plugin}
     * @throws EntityInvalidException
     * @throws EntityNotFoundException
     */
    @RequestMapping(value = "/{pluginId}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to update an IHM plugin", role = DefaultRole.PROJECT_ADMIN)
    public HttpEntity<Resource<Plugin>> updatePlugin(@PathVariable("pluginId") final Long pPluginId,
            @Valid @RequestBody final Plugin pPlugin) throws EntityException {

        if (!pPlugin.getId().equals(pPluginId)) {
            throw new EntityInvalidException("Invalide application identifier for plugin");
        }
        final Plugin plugin = service.updatePlugin(pPlugin);
        final Resource<Plugin> resource = toResource(plugin);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Entry point to delete an ihm plugin.
     *
     * @return {@link Plugin}
     * @throws EntityInvalidException
     * @throws EntityNotFoundException
     */
    @RequestMapping(value = "/{pluginId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to delete a plugin", role = DefaultRole.PROJECT_ADMIN)
    public HttpEntity<Resource<Void>> deletePlugin(@PathVariable("pluginId") final Long pPluginId)
            throws EntityNotFoundException {
        service.deletePlugin(pPluginId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public Resource<Plugin> toResource(final Plugin pElement, final Object... pExtras) {
        final Resource<Plugin> resource = resourceService.toResource(pElement);
        resourceService.addLink(resource, this.getClass(), "retrievePlugin", LinkRels.SELF,
                                MethodParamFactory.build(Long.class, pElement.getId()));
        resourceService.addLink(resource, this.getClass(), "updatePlugin", LinkRels.UPDATE,
                                MethodParamFactory.build(Long.class, pElement.getId()),
                                MethodParamFactory.build(Plugin.class));
        resourceService.addLink(resource, this.getClass(), "deletePlugin", LinkRels.DELETE,
                                MethodParamFactory.build(Long.class, pElement.getId()));
        return resource;
    }

}
