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
import fr.cnes.regards.modules.configuration.dao.IUIPluginDefinitionRepository;
import fr.cnes.regards.modules.configuration.domain.UIPluginDefinition;
import fr.cnes.regards.modules.configuration.domain.UIPluginTypesEnum;

@Service(value = "pluginService")
public class UIPluginDefinitionService implements IUIPluginDefinitionService {

    @Autowired
    private IUIPluginDefinitionRepository repository;

    @Override
    public UIPluginDefinition retrievePlugin(final Long pPluginId) throws EntityNotFoundException {
        if (!repository.exists(pPluginId)) {
            throw new EntityNotFoundException(pPluginId, UIPluginDefinition.class);
        }
        return repository.findOne(pPluginId);
    }

    @Override
    public Page<UIPluginDefinition> retrievePlugins(final Pageable pPageable) {
        return repository.findAll(pPageable);
    }

    @Override
    public Page<UIPluginDefinition> retrievePlugins(final UIPluginTypesEnum pType, final Pageable pPageable) {
        return repository.findByType(pType, pPageable);
    }

    @Override
    public UIPluginDefinition savePlugin(final UIPluginDefinition pPlugin) throws EntityInvalidException {
        return repository.save(pPlugin);
    }

    @Override
    public UIPluginDefinition updatePlugin(final UIPluginDefinition pPlugin) throws EntityNotFoundException, EntityInvalidException {
        if (!repository.exists(pPlugin.getId())) {
            throw new EntityNotFoundException(pPlugin.getId(), UIPluginDefinition.class);
        }
        return repository.save(pPlugin);
    }

    @Override
    public void deletePlugin(final Long pPluginId) throws EntityNotFoundException {
        if (!repository.exists(pPluginId)) {
            throw new EntityNotFoundException(pPluginId, UIPluginDefinition.class);
        }
        repository.delete(pPluginId);
    }

}
