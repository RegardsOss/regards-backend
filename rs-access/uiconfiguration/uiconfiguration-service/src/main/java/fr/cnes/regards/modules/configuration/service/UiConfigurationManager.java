package fr.cnes.regards.modules.configuration.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.module.manager.AbstractModuleConfigurationManager;
import fr.cnes.regards.framework.module.manager.ModuleConfiguration;
import fr.cnes.regards.framework.module.manager.ModuleConfigurationItem;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.configuration.domain.Theme;

/**
 * Describe how to export and import configuration for uiconfiguration module.
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Component
public class UiConfigurationManager extends AbstractModuleConfigurationManager {

    public static final String ALREADY_EXISTS = "Skipping import because a theme with same name(%s) already exists";

    @Autowired
    private IThemeService themeService;

    @Override
    protected Set<String> importConfiguration(ModuleConfiguration moduleConfiguration) {
        Set<String> importErrors = new HashSet<>();
        for (ModuleConfigurationItem<?> item : moduleConfiguration.getConfiguration()) {
            if (Theme.class.isAssignableFrom(item.getKey())) {
                Theme toImport = item.getTypedValue();
                if (themeService.retrieveByName(toImport.getName()).isPresent()) {
                    importErrors.add(String.format(ALREADY_EXISTS, toImport.getName()));
                } else {
                    themeService.saveTheme(toImport);
                }
            }
        }
        return importErrors;
    }

    @Override
    public ModuleConfiguration exportConfiguration() throws ModuleException {
        List<ModuleConfigurationItem<?>> configurations = new ArrayList<>();
        for (Theme theme : themeService.retrieveAllThemes()) {
            configurations.add(ModuleConfigurationItem.build(theme));
        }
        return ModuleConfiguration.build(info, configurations);
    }
}
