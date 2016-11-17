/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.authentication.external.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.cloud.gateway.authentication.plugins.domain.ExternalAuthenticationInformations;
import fr.cnes.regards.framework.authentication.external.service.IExternalAuthenticationPluginsService;
import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.ModuleEntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;

/**
 *
 * Class InternalAuthenicationController
 *
 * REST Controller to manage internal authentication Service providers
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@RestController
public class ExternalAuthenticationController
        implements IExternalAuthenticationSignature, IResourceController<PluginConfiguration> {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ExternalAuthenticationController.class);

    /**
     * Service to manage PluginConfiguration for internal authentication Service providers
     */
    private final IExternalAuthenticationPluginsService service;

    /**
     * Resource service to manage visibles hateoas links
     */
    private final IResourceService resourceService;

    public ExternalAuthenticationController(final IExternalAuthenticationPluginsService pService,
            final IResourceService pResourceService) {
        super();
        service = pService;
        resourceService = pResourceService;
    }

    @Override
    public ResponseEntity<List<Resource<PluginConfiguration>>> retrieveServiceProviderPlugins() {
        final List<PluginConfiguration> plugins = service.retrieveServiceProviderPlugins();
        return new ResponseEntity<>(toResources(plugins), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Resource<PluginConfiguration>> createServiceProviderPlugin(
            @RequestBody final PluginConfiguration pPluginConfigurationToCreate) {
        ResponseEntity<Resource<PluginConfiguration>> response;
        try {
            final PluginConfiguration plugin = service.createServiceProviderPlugin(pPluginConfigurationToCreate);
            response = new ResponseEntity<>(toResource(plugin), HttpStatus.OK);
        } catch (final ModuleException e) {
            LOG.error(e.getMessage(), e);
            response = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return response;
    }

    @Override
    public ResponseEntity<Resource<PluginConfiguration>> retrieveServiceProviderPlugin(
            @PathVariable("sp_id") final Long pPluginConfigurationId) {
        ResponseEntity<Resource<PluginConfiguration>> response;
        try {
            final PluginConfiguration plugin = service.retrieveServiceProviderPlugin(pPluginConfigurationId);
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
    public ResponseEntity<Resource<PluginConfiguration>> updateServiceProviderPlugin(
            @PathVariable("sp_id") final Long pPluginConfigurationId,
            @RequestBody final PluginConfiguration pPluginConfigurationToUpdate) {
        ResponseEntity<Resource<PluginConfiguration>> response;
        if (pPluginConfigurationId.equals(pPluginConfigurationToUpdate.getId())) {
            try {
                final PluginConfiguration plugin = service.updateServiceProviderPlugin(pPluginConfigurationToUpdate);
                if (plugin != null) {
                    response = new ResponseEntity<>(toResource(plugin), HttpStatus.OK);
                } else {
                    response = new ResponseEntity<>(HttpStatus.NOT_FOUND);
                }
            } catch (final ModuleEntityNotFoundException e) {
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

    @Override
    public ResponseEntity<Void> deleteServiceProviderPlugin(@PathVariable("sp_id") final Long pPluginConfigurationId) {
        ResponseEntity<Void> response;
        try {
            service.deleteServiceProviderPlugin(pPluginConfigurationId);
            response = new ResponseEntity<>(HttpStatus.OK);
        } catch (final ModuleEntityNotFoundException e) {
            LOG.error(e.getMessage(), e);
            response = new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return response;

    }

    @Override
    public ResponseEntity<OAuth2AccessToken> authenticate(@PathVariable("sp_id") final Long pPluginConfigurationId,
            @RequestBody final ExternalAuthenticationInformations pAuthInformations) {
        try {
            return new ResponseEntity<>(service.authenticate(pPluginConfigurationId, pAuthInformations), HttpStatus.OK);
        } catch (final ModuleEntityNotFoundException e) {
            LOG.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public Resource<PluginConfiguration> toResource(final PluginConfiguration pElement, final Object... pExtras) {
        final Resource<PluginConfiguration> resource = resourceService.toResource(pElement);
        resourceService.addLink(resource, this.getClass(), "retrieveServiceProviderPlugin", LinkRels.SELF,
                                MethodParamFactory.build(Long.class, pElement.getId()));
        resourceService.addLink(resource, this.getClass(), "updateServiceProviderPlugin", LinkRels.UPDATE,
                                MethodParamFactory.build(Long.class, pElement.getId()),
                                MethodParamFactory.build(PluginConfiguration.class, pElement));
        resourceService.addLink(resource, this.getClass(), "createServiceProviderPlugin", LinkRels.CREATE,
                                MethodParamFactory.build(PluginConfiguration.class, pElement));
        resourceService.addLink(resource, this.getClass(), "deleteServiceProviderPlugin", LinkRels.DELETE,
                                MethodParamFactory.build(Long.class, pElement.getId()));
        return resource;

    }

}
