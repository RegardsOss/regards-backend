/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.authentication.internal.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.authentication.internal.service.IInternalAuthenticationPluginsService;
import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.InvalidEntityException;
import fr.cnes.regards.framework.module.rest.exception.ModuleEntityNotFoundException;
import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.plugins.utils.PluginUtilsException;

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
public class InternalAuthenticationController
        implements IInternalAuthenticationSignature, IResourceController<PluginConfiguration> {

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
    private final IResourceService resourceService;

    public InternalAuthenticationController(final IInternalAuthenticationPluginsService pService,
            final IResourceService pResourceService) {
        super();
        service = pService;
        resourceService = pResourceService;
    }

    @Override
    public ResponseEntity<List<Resource<PluginConfiguration>>> retrieveIdentityProviderPlugins() {
        final List<PluginConfiguration> plugins = service.retrieveIdentityProviderPlugins();
        return new ResponseEntity<>(toResources(plugins), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Resource<PluginConfiguration>> createIdentityProviderPlugin(
            @RequestBody final PluginConfiguration pPluginConfigurationToCreate) {
        ResponseEntity<Resource<PluginConfiguration>> response;
        try {
            final PluginConfiguration plugin = service.createIdentityProviderPlugin(pPluginConfigurationToCreate);
            response = new ResponseEntity<>(toResource(plugin), HttpStatus.OK);
        } catch (final InvalidEntityException e) {
            LOG.error(e.getMessage(), e);
            response = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return response;
    }

    @Override
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
        } catch (final ModuleEntityNotFoundException e) {
            LOG.error(e.getMessage(), e);
            response = new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return response;
    }

    @Override
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
            } catch (final InvalidEntityException e) {
                LOG.error(e.getMessage(), e);
                response = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            } catch (final ModuleEntityNotFoundException e) {
                LOG.error(e.getMessage(), e);
                response = new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

        } else {
            LOG.error(String.format("The given plugin id %s does not match the given plugin to update id %s",
                                    pPluginConfigurationId, pPluginConfigurationToUpdate.getId()));
            response = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return response;
    }

    @Override
    public ResponseEntity<Void> deleteIdentityProviderPlugin(
            @PathVariable("idp_id") final Long pPluginConfigurationId) {
        ResponseEntity<Void> response;
        try {
            service.deleteIdentityProviderPlugin(pPluginConfigurationId);
            response = new ResponseEntity<>(HttpStatus.OK);
        } catch (final ModuleEntityNotFoundException e) {
            LOG.error(e.getMessage(), e);
            response = new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (final PluginUtilsException e) {
            LOG.error(e.getMessage(), e);
            response = new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return response;

    }

    @Override
    public Resource<PluginConfiguration> toResource(final PluginConfiguration pElement) {
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
