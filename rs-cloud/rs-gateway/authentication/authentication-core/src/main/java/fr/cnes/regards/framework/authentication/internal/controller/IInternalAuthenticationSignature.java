/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.authentication.internal.controller;

import java.util.List;

import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;

/**
 *
 * Class IInternalAuthenticationSignature
 *
 * Internal authentication plugins management controller interface
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@RequestMapping("/authentication/idps")
public interface IInternalAuthenticationSignature {

    /**
     *
     * Retrieve all configured Identity Provider plugins to handle REGARDS internal authentication
     *
     * @return List fo PluginConfiguration (Hateoas formated)
     * @since 1.0-SNAPSHOT
     */
    @ResourceAccess(
            description = "Retrieve all configured Identity Provider plugins to handle REGARDS internal authentication",
            role = DefaultRole.PROJECT_ADMIN)
    @RequestMapping(method = RequestMethod.GET, produces = "application/json")
    ResponseEntity<List<Resource<PluginConfiguration>>> retrieveIdentityProviderPlugins();

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
    ResponseEntity<Resource<PluginConfiguration>> createIdentityProviderPlugin(
            @RequestBody PluginConfiguration pPluginConfigurationToCreate);

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
    ResponseEntity<Resource<PluginConfiguration>> retrieveIdentityProviderPlugin(
            @PathVariable("idp_id") Long pPluginConfigurationId);

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
    ResponseEntity<Resource<PluginConfiguration>> updateIdentityProviderPlugin(
            @PathVariable("idp_id") Long pPluginConfigurationId,
            @RequestBody PluginConfiguration pPluginConfigurationToUpdate);

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
    ResponseEntity<Void> deleteIdentityProviderPlugin(@PathVariable("idp_id") Long pPluginConfigurationId);

}
