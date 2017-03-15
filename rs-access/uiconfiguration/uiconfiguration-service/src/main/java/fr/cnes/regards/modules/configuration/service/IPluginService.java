/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.configuration.domain.Plugin;

public interface IPluginService {

    /**
     *
     * Retreive a Plugin by is id.
     *
     * @param pPluginId
     * @return {@link Plugin}
     * @since 1.0-SNAPSHOT
     */
    Plugin retrievePlugin(Long pPluginId) throws EntityNotFoundException;

    /**
     *
     * Retrieve all plugins
     *
     * @return Paged list of {@link Plugin}
     * @since 1.0-SNAPSHOT
     */
    Page<Plugin> retrievePlugins(Pageable pPageable);

    /**
     *
     * Save a new plugin
     *
     * @param pTheme
     *            {@link Plugin} to save
     * @return saved {@link Plugin}
     * @since 1.0-SNAPSHOT
     */
    Plugin savePlugin(Plugin pPlugin) throws EntityInvalidException;

    /**
     *
     * Update a plugin
     *
     * @param pTheme
     *            {@link Plugin} to update
     * @return updated {@link Plugin}
     * @since 1.0-SNAPSHOT
     */
    Plugin updatePlugin(Plugin pPlugin) throws EntityNotFoundException, EntityInvalidException;

    /**
     *
     * Delete a plugin
     *
     * @param pPluginId
     *            {@link Plugin} id to delete
     *
     * @since 1.0-SNAPSHOT
     */
    void deletePlugin(Long pPluginId) throws EntityNotFoundException;

}
