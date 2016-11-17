/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.authentication.external.controller;

import java.util.List;

import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.cloud.gateway.authentication.plugins.IServiceProviderPlugin;
import fr.cnes.regards.cloud.gateway.authentication.plugins.domain.ExternalAuthenticationInformations;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;

/**
 *
 * Class IExternalAuthenticationController
 *
 * External authentication plugins management controller interface
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@RequestMapping("/authentication/sps")
public interface IExternalAuthenticationSignature {

    /**
     *
     * Retrieve all configured Service Provider plugins to handle REGARDS internal authentication
     *
     * @return List fo PluginConfiguration (Hateoas formated)
     * @since 1.0-SNAPSHOT
     */
    @ResourceAccess(
            description = "Retrieve all configured Service Provider plugins to handle REGARDS internal authentication")
    @RequestMapping(method = RequestMethod.GET, produces = "application/json")
    ResponseEntity<List<Resource<PluginConfiguration>>> retrieveServiceProviderPlugins();

    /**
     *
     * Create a new Service Provider plugin
     *
     * @param pPluginConfigurationToCreate
     *            PluginConfiguration to create
     * @return Created PluginConfiguration (hateoas formated)
     * @since 1.0-SNAPSHOT
     */
    @ResourceAccess(description = "Create a new Service Provider plugin")
    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    ResponseEntity<Resource<PluginConfiguration>> createServiceProviderPlugin(
            @RequestBody PluginConfiguration pPluginConfigurationToCreate);

    /**
     *
     * Retrieve a configured Service Provider plugin
     *
     * @param pPluginConfigurationId
     *            PluginConfiguration identifier to retrieve
     * @return PluginConfiguration (hateoas formated)
     * @since 1.0-SNAPSHOT
     */
    @ResourceAccess(description = "Retrieve a configured Service Provider plugin")
    @RequestMapping(path = "/{sp_id}", method = RequestMethod.GET, produces = "application/json")
    ResponseEntity<Resource<PluginConfiguration>> retrieveServiceProviderPlugin(
            @PathVariable("sp_id") Long pPluginConfigurationId);

    /**
     *
     * Update an Service Provider plugin
     *
     * @param pPluginConfigurationId
     *            PluginConfiguration identifier to update
     * @param pPluginConfigurationToUpdate
     *            PluginConfiguration to update
     * @return updated PluginConfiguration (hateoas formated)
     * @since 1.0-SNAPSHOT
     */
    @ResourceAccess(description = "Update a Service Provider plugin")
    @RequestMapping(path = "/{sp_id}", method = RequestMethod.PUT, produces = "application/json")
    ResponseEntity<Resource<PluginConfiguration>> updateServiceProviderPlugin(
            @PathVariable("sp_id") Long pPluginConfigurationId,
            @RequestBody PluginConfiguration pPluginConfigurationToUpdate);

    /**
     *
     * Delete an Service Provider plugin
     *
     * @param pPluginConfigurationId
     *            PluginConfiguration identifier to delete
     * @return Void
     * @since 1.0-SNAPSHOT
     */
    @ResourceAccess(description = "Delete a Service Provider plugin")
    @RequestMapping(path = "/{sp_id}", method = RequestMethod.DELETE)
    ResponseEntity<Void> deleteServiceProviderPlugin(@PathVariable("sp_id") Long pPluginConfigurationId);

    /**
     *
     * Authenticate with the given Service Provider plugin
     *
     * @param pPluginConfigurationId
     *            PluginConfiguration identifier to delete
     * @return Void
     * @since 1.0-SNAPSHOT
     */
    @ResourceAccess(description = "Authenticate with the given Service Provider plugin",
            plugin = IServiceProviderPlugin.class)
    @RequestMapping(path = "/{sp_id}/authenticate", method = RequestMethod.POST)
    ResponseEntity<OAuth2AccessToken> authenticate(@PathVariable("sp_id") Long pPluginConfigurationId,
            @RequestBody ExternalAuthenticationInformations pAuthInformations);

}
