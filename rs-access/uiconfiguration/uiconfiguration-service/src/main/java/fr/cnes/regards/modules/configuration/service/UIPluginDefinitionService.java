/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.service;

import java.util.List;

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
public class UIPluginDefinitionService extends AbstractUiConfigurationService implements IUIPluginDefinitionService {

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
    public UIPluginDefinition updatePlugin(final UIPluginDefinition pPlugin)
            throws EntityNotFoundException, EntityInvalidException {
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

    @Override
    protected void initProjectUI(final String pTenant) {

        // Create default plugins if no plugin defined
        final List<UIPluginDefinition> plugins = repository.findAll();
        if (plugins.isEmpty()) {
            // Create string plugin
            UIPluginDefinition plugin = new UIPluginDefinition();
            plugin.setName("string-criteria");
            plugin.setSourcePath("/plugins/criterion/string/plugin.js");
            plugin.setType(UIPluginTypesEnum.CRITERIA);
            repository.save(plugin);

            plugin = new UIPluginDefinition();
            plugin.setName("full-text-criteria");
            plugin.setSourcePath("/plugins/criterion/full-text/plugin.js");
            plugin.setType(UIPluginTypesEnum.CRITERIA);
            repository.save(plugin);

            plugin = new UIPluginDefinition();
            plugin.setName("numerical-criteria");
            plugin.setSourcePath("/plugins/criterion/numerical/plugin.js");
            plugin.setType(UIPluginTypesEnum.CRITERIA);
            repository.save(plugin);

            plugin = new UIPluginDefinition();
            plugin.setName("two-numerical-criteria");
            plugin.setSourcePath("/plugins/criterion/two-numerical/plugin.js");
            plugin.setType(UIPluginTypesEnum.CRITERIA);
            repository.save(plugin);

            plugin = new UIPluginDefinition();
            plugin.setName("temporal-criteria");
            plugin.setSourcePath("/plugins/criterion/temporal/plugin.js");
            plugin.setType(UIPluginTypesEnum.CRITERIA);
            repository.save(plugin);

            plugin = new UIPluginDefinition();
            plugin.setName("two-temporal-criteria");
            plugin.setSourcePath("/plugins/criterion/two-temporal/plugin.js");
            plugin.setType(UIPluginTypesEnum.CRITERIA);
            repository.save(plugin);
        }

    }

    @Override
    protected void initInstanceUI() {
        // Nothing to do.
    }

}
