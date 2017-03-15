/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.configuration.dao.IPluginRepository;
import fr.cnes.regards.modules.configuration.domain.Plugin;

@Service(value = "pluginService")
public class PluginService implements IPluginService {

    @Autowired
    private IPluginRepository repository;

    @Override
    public Plugin retrievePlugin(final Long pPluginId) throws EntityNotFoundException {
        if (!repository.exists(pPluginId)) {
            throw new EntityNotFoundException(pPluginId, Plugin.class);
        }
        return repository.findOne(pPluginId);
    }

    @Override
    public Page<Plugin> retrievePlugins(final Pageable pPageable) {
        return repository.findAll(pPageable);
    }

    @Override
    public Plugin savePlugin(final Plugin pPlugin) throws EntityInvalidException {
        return repository.save(pPlugin);
    }

    @Override
    public Plugin updatePlugin(final Plugin pPlugin) throws EntityNotFoundException, EntityInvalidException {
        if (!repository.exists(pPlugin.getId())) {
            throw new EntityNotFoundException(pPlugin.getId(), Plugin.class);
        }
        return repository.save(pPlugin);
    }

    @Override
    public void deletePlugin(final Long pPluginId) throws EntityNotFoundException {
        if (!repository.exists(pPluginId)) {
            throw new EntityNotFoundException(pPluginId, Plugin.class);
        }
        repository.delete(pPluginId);
    }

}
