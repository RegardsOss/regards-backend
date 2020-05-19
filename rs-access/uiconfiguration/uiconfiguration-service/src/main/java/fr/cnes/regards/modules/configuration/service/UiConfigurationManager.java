package fr.cnes.regards.modules.configuration.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.module.manager.AbstractModuleManager;
import fr.cnes.regards.framework.module.manager.ModuleConfiguration;
import fr.cnes.regards.framework.module.manager.ModuleConfigurationItem;
import fr.cnes.regards.framework.module.manager.ModuleReadinessReport;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.configuration.domain.Module;
import fr.cnes.regards.modules.configuration.domain.Theme;

/**
 * Describe how to export and import configuration for uiconfiguration module.
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Component
public class UiConfigurationManager extends AbstractModuleManager<Void> {

    @Autowired
    private IThemeService themeService;

    @Autowired
    private IModuleService moduleService;

    @Override
    public Set<String> resetConfiguration() {
        Set<String> errors = Sets.newHashSet();
        for (Theme t : themeService.retrieveAllThemes()) {
            try {
                themeService.deleteTheme(t.getId());
            } catch (EntityNotFoundException e) {
                errors.add(e.getMessage());
            }
        }
        for (Module module : moduleService.retrieveModules(PageRequest.of(0, 1_000))) {
            try {
                moduleService.deleteModule(module.getId());
            } catch (EntityNotFoundException e) {
                errors.add(e.getMessage());
            }
        }
        return errors;
    }

    @Override
    protected Set<String> importConfiguration(ModuleConfiguration moduleConfiguration) {
        Set<String> importErrors = new HashSet<>();
        for (ModuleConfigurationItem<?> item : moduleConfiguration.getConfiguration()) {
            if (Theme.class.isAssignableFrom(item.getKey())) {
                Theme toImport = item.getTypedValue();
                Optional<Theme> existingOne = themeService.retrieveByName(toImport.getName());
                if (existingOne.isPresent()) {
                    Theme t = existingOne.get();
                    t.setConfiguration(toImport.getConfiguration());
                    try {
                        themeService.updateTheme(t);
                    } catch (EntityException e) {
                        importErrors.add(e.getMessage());
                    }
                } else {
                    themeService.saveTheme(toImport);
                }
            } else if (Module.class.isAssignableFrom(item.getKey())) {
                Module toImport = item.getTypedValue();
                try {
                    moduleService.saveModule(toImport);
                } catch (EntityInvalidException e) {
                    importErrors.add(e.getMessage());
                }
            }
        }
        return importErrors;
    }

    @Override
    public ModuleConfiguration exportConfiguration() throws ModuleException {
        List<ModuleConfigurationItem<?>> configurations = new ArrayList<>();
        for (Theme theme : themeService.retrieveAllThemes()) {
            theme.setId(null);
            configurations.add(ModuleConfigurationItem.build(theme));
        }
        for (Module module : moduleService.retrieveModules(PageRequest.of(0, 1_000))) {
            module.setId(null);
            configurations.add(ModuleConfigurationItem.build(module));
        }
        return ModuleConfiguration.build(info, true, configurations);
    }

    @Override
    public ModuleReadinessReport<Void> isReady() {
        return new ModuleReadinessReport<Void>(true, null, null);
    }
}
