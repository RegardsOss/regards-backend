/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.authentication.internal.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.authentication.internal.service.IInternalAuthenticationPluginsService;
import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.modules.plugins.service.PluginService;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;

/**
 *
 * Class InternalAuthenicationController
 *
 * REST Controller to manage internal authentication Identity providers
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@RestController
@RequestMapping("/authentication/idps")
public class InternalAuthenticationController implements IResourceController<PluginConfiguration> {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(InternalAuthenticationController.class);

    /**
     * Service to manage PluginConfiguration for internal authentication Identity providers
     */
    private final IInternalAuthenticationPluginsService service;

    /**
     * Resource service to manage visibles hateoas links
     */
    @Autowired
    private IResourceService resourceService;

    /**
     * Constructor to specify a particular {@link IPluginService}.
     * 
     * @param pService
     *            The {@link PluginService} used
     */
    public InternalAuthenticationController(final IInternalAuthenticationPluginsService pService) {
        super();
        service = pService;
    }

    /**
     *
     * Retrieve all configured Identity Provider plugins to handle REGARDS internal authentication
     *
     * @return List for PluginConfiguration (Hateoas formated)
     * @since 1.0-SNAPSHOT
     */
    @ResourceAccess(
            description = "Retrieve all configured Identity Provider plugins to handle REGARDS internal authentication",
            role = DefaultRole.PROJECT_ADMIN)
    @RequestMapping(method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<List<Resource<PluginConfiguration>>> retrieveIdentityProviderPlugins() {
        final List<PluginConfiguration> plugins = service.retrieveIdentityProviderPlugins();
        return new ResponseEntity<>(toResources(plugins), HttpStatus.OK);
    }

    /**
     *
     * Create a new Identity Provider plugin
     *
     * @param pPluginConfigurationToCreate
     *            PluginConfiguration to create
     * @return Created PluginConfiguration (hateoas formated)
     * @since 1.0-SNAPSHOT
     */
    @ResourceAccess(description = "Create a new Identity Provider plugin", role = DefaultRole.PROJECT_ADMIN)
    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<Resource<PluginConfiguration>> createIdentityProviderPlugin(
            @RequestBody final PluginConfiguration pPluginConfigurationToCreate) {
        ResponseEntity<Resource<PluginConfiguration>> response;
        try {
            final PluginConfiguration plugin = service.createIdentityProviderPlugin(pPluginConfigurationToCreate);
            response = new ResponseEntity<>(toResource(plugin), HttpStatus.OK);
        } catch (final ModuleException e) {
            LOG.error(e.getMessage(), e);
            response = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return response;
    }

    /**
     *
     * Retrieve a configured Identity Provider plugin
     *
     * @param pPluginConfigurationId
     *            PluginConfiguration identifier to retrieve
     * @return PluginConfiguration (hateoas formated)
     * @since 1.0-SNAPSHOT
     */
    @ResourceAccess(description = "Retrieve a configured Identity Provider plugin", role = DefaultRole.PROJECT_ADMIN)
    @RequestMapping(path = "/{idp_id}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<Resource<PluginConfiguration>> retrieveIdentityProviderPlugin(
            @PathVariable("idp_id") final Long pPluginConfigurationId) {
        ResponseEntity<Resource<PluginConfiguration>> response;
        try {
            final PluginConfiguration plugin = service.retrieveIdentityProviderPlugin(pPluginConfigurationId);
            if (plugin != null) {
                response = new ResponseEntity<>(toResource(plugin), HttpStatus.OK);
            } else {
                response = new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (final ModuleException e) {
            LOG.error(e.getMessage(), e);
            response = new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return response;
    }

    /**
     *
     * Update an Identity Provider plugin
     *
     * @param pPluginConfigurationId
     *            PluginConfiguration identifier to update
     * @param pPluginConfigurationToUpdate
     *            PluginConfiguration to update
     * @return updated PluginConfiguration (hateoas formated)
     * @since 1.0-SNAPSHOT
     */
    @ResourceAccess(description = "Update an Identity Provider plugin", role = DefaultRole.PROJECT_ADMIN)
    @RequestMapping(path = "/{idp_id}", method = RequestMethod.PUT, produces = "application/json")
    public ResponseEntity<Resource<PluginConfiguration>> updateIdentityProviderPlugin(
            @PathVariable("idp_id") final Long pPluginConfigurationId,
            @RequestBody final PluginConfiguration pPluginConfigurationToUpdate) {
        ResponseEntity<Resource<PluginConfiguration>> response;
        if (pPluginConfigurationId.equals(pPluginConfigurationToUpdate.getId())) {
            try {
                final PluginConfiguration plugin = service.updateIdentityProviderPlugin(pPluginConfigurationToUpdate);
                if (plugin != null) {
                    response = new ResponseEntity<>(toResource(plugin), HttpStatus.OK);
                } else {
                    response = new ResponseEntity<>(HttpStatus.NOT_FOUND);
                }
            } catch (final EntityNotFoundException e) {
                LOG.error(e.getMessage(), e);
                response = new ResponseEntity<>(HttpStatus.NOT_FOUND);
            } catch (final ModuleException e) {
                LOG.error(e.getMessage(), e);
                response = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

        } else {
            LOG.error(String.format("The given plugin id %s does not match the given plugin to update id %s",
                                    pPluginConfigurationId, pPluginConfigurationToUpdate.getId()));
            response = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return response;
    }

    /**
     *
     * Delete an Identity Provider plugin
     *
     * @param pPluginConfigurationId
     *            PluginConfiguration identifier to delete
     * @return Void
     * @since 1.0-SNAPSHOT
     */
    @ResourceAccess(description = "Delete an Identity Provider plugin", role = DefaultRole.PROJECT_ADMIN)
    @RequestMapping(path = "/{idp_id}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteIdentityProviderPlugin(
            @PathVariable("idp_id") final Long pPluginConfigurationId) {
        ResponseEntity<Void> response;
        try {
            service.deleteIdentityProviderPlugin(pPluginConfigurationId);
            response = new ResponseEntity<>(HttpStatus.OK);
        } catch (final EntityNotFoundException e) {
            LOG.error(e.getMessage(), e);
            response = new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (final ModuleException e) {
            LOG.error(e.getMessage(), e);
            response = new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return response;

    }

    @Override
    public Resource<PluginConfiguration> toResource(final PluginConfiguration pElement, final Object... pExtras) {
        final Resource<PluginConfiguration> resource = resourceService.toResource(pElement);
        resourceService.addLink(resource, this.getClass(), "retrieveIdentityProviderPlugin", LinkRels.SELF,
                                MethodParamFactory.build(Long.class, pElement.getId()));
        resourceService.addLink(resource, this.getClass(), "updateIdentityProviderPlugin", LinkRels.UPDATE,
                                MethodParamFactory.build(Long.class, pElement.getId()),
                                MethodParamFactory.build(PluginConfiguration.class, pElement));
        resourceService.addLink(resource, this.getClass(), "createIdentityProviderPlugin", LinkRels.CREATE,
                                MethodParamFactory.build(PluginConfiguration.class, pElement));
        resourceService.addLink(resource, this.getClass(), "deleteIdentityProviderPlugin", LinkRels.DELETE,
                                MethodParamFactory.build(Long.class, pElement.getId()));
        return resource;

    }

}
