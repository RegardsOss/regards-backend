/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.modules.configuration.domain.UIPluginDefinition;
import fr.cnes.regards.modules.configuration.domain.UIPluginConfiguration;

public interface IUIPluginConfigurationService {

    /**
     *
     * Retrieve all plugin configurations
     *
     * @param pIsActive
     *            Return only the active plugins ?. Pass null value to not filter.
     * @param pIsLinkedToAllEntities
     *            Return only the plugins linked to all entities?. Pass null value to not filter.
     * @return Page of {@link UIPluginConfiguration}
     * @since 1.0-SNAPSHOT
     */
    Page<UIPluginConfiguration> retrievePluginConfigurations(Boolean pIsActive, Boolean pIsLinkedToAllEntities,
            Pageable pPageable);

    /**
     *
     * Retrieve all plugin configurations for the given plugin
     *
     * @param {@link
     *            UIPluginDefinition}'s to search configurations.
     * @param pIsActive
     *            Return only the active plugins ?. Pass null value to not filter.
     * @param pIsLinkedToAllEntities
     *            Return only the plugins linked to all entities?. Pass null value to not filter.
     * @return Page of {@link UIPluginConfiguration}
     * @throws EntityException
     *             throw exception if pPlugin is not defined or not exists.
     * @since 1.0-SNAPSHOT
     */
    Page<UIPluginConfiguration> retrievePluginConfigurations(UIPluginDefinition pPlugin, Boolean pIsActive,
            Boolean pIsLinkedToAllEntities, Pageable pPageable) throws EntityException;

    /**
     *
     * Retrieve one plugin configuration.
     *
     * @param pPluginConfigurationId
     *            PluginConfiguration id to retreive
     * @return {@link UIPluginConfiguration}
     * @throws EntityInvalidException
     *             if pPluginConfigurationId is null
     * @since 1.0-SNAPSHOT
     */
    UIPluginConfiguration retrievePluginconfiguration(Long pPluginConfigurationId) throws EntityInvalidException;

    /**
     *
     * Update a plugin configuration.
     *
     * @param UIPluginConfiguration
     *            {@link UIPluginConfiguration} to update
     * @return {@link UIPluginConfiguration}
     * @throws EntityException
     *             if given configuration is null or does not exists
     * @since 1.0-SNAPSHOT
     */
    UIPluginConfiguration updatePluginconfiguration(UIPluginConfiguration pPluginConfiguration) throws EntityException;

    /**
     *
     * Create a plugin configuration.
     *
     * @param UIPluginConfiguration
     *            {@link UIPluginConfiguration} to update
     * @return {@link UIPluginConfiguration}
     * @throws throws
     *             EntityException if pPluginConfiguration already exists
     * @since 1.0-SNAPSHOT
     */
    UIPluginConfiguration createPluginconfiguration(UIPluginConfiguration pPluginConfiguration) throws EntityException;

    /**
     *
     * Delete a plugin configuration.
     *
     * @param UIPluginConfiguration
     *            {@link UIPluginConfiguration} to update
     * @return {@link UIPluginConfiguration}
     * @throws EntityException
     *             if pPluginConfiguration is invalid or does not exists
     * @since 1.0-SNAPSHOT
     */
    void deletePluginconfiguration(UIPluginConfiguration pPluginConfiguration) throws EntityException;

}
