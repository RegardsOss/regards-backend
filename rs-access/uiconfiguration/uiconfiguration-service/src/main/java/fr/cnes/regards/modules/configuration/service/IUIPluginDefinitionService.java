/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.configuration.domain.UIPluginDefinition;
import fr.cnes.regards.modules.configuration.domain.UIPluginTypesEnum;

@RegardsTransactional
public interface IUIPluginDefinitionService {

    /**
     *
     * Retreive a Plugin by is id.
     *
     * @param pPluginId
     * @return {@link UIPluginDefinition}
     * @since 1.0-SNAPSHOT
     */
    UIPluginDefinition retrievePlugin(Long pPluginId) throws EntityNotFoundException;

    /**
     *
     * Retrieve all plugins
     *
     * @return Paged list of {@link UIPluginDefinition}
     * @since 1.0-SNAPSHOT
     */
    Page<UIPluginDefinition> retrievePlugins(Pageable pPageable);

    /**
     *
     * Retrieve all plugins for the given type
     *
     * @return Paged list of {@link UIPluginDefinition}
     * @since 1.0-SNAPSHOT
     */
    Page<UIPluginDefinition> retrievePlugins(UIPluginTypesEnum pType, Pageable pPageable);

    /**
     *
     * Save a new plugin
     *
     * @param pTheme
     *            {@link UIPluginDefinition} to save
     * @return saved {@link UIPluginDefinition}
     * @since 1.0-SNAPSHOT
     */
    UIPluginDefinition savePlugin(UIPluginDefinition pPlugin) throws EntityInvalidException;

    /**
     *
     * Update a plugin
     *
     * @param pTheme
     *            {@link UIPluginDefinition} to update
     * @return updated {@link UIPluginDefinition}
     * @since 1.0-SNAPSHOT
     */
    UIPluginDefinition updatePlugin(UIPluginDefinition pPlugin) throws EntityNotFoundException, EntityInvalidException;

    /**
     *
     * Delete a plugin
     *
     * @param pPluginId
     *            {@link UIPluginDefinition} id to delete
     *
     * @since 1.0-SNAPSHOT
     */
    void deletePlugin(Long pPluginId) throws EntityNotFoundException;

}
