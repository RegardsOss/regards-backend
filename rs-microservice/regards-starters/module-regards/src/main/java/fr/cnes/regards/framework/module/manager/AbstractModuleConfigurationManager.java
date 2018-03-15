/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.module.manager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;

/**
 * Common module configuration manager
 *
 * @author Marc Sordi
 */
public abstract class AbstractModuleConfigurationManager implements IModuleConfigurationManager {

    protected ModuleInformation info;

    @Autowired
    private Validator validator;

    @PostConstruct
    protected void init() throws ModuleException {
        info = loadInformation();
        validate(info);
    }

    /**
     * Load {@link ModuleInformation} from property file. Property file
     * {@link IModuleConfigurationManager#PROPERTY_FILE} is loaded from the same package as the manager implementation.
     */
    ModuleInformation loadInformation() throws ModuleException {
        ModuleInformation info = new ModuleInformation();
        Properties properties = new Properties();
        try {
            properties.load(this.getClass().getResourceAsStream(PROPERTY_FILE));
            info.setId(properties.getProperty("module.id"));
            info.setName(properties.getProperty("module.name"));
            info.setDescription(properties.getProperty("module.description"));
            info.setVersion(properties.getProperty("module.version"));
            info.setAuthor(properties.getProperty("module.author"));
            info.setLegalOwner(properties.getProperty("module.legalOwner"));
            info.setDocumentation(properties.getProperty("module.documentation"));
            return info;
        } catch (IOException e) {
            throw new ModuleException("Cannot load module properties", e);
        }
    }

    protected void validate(ModuleInformation info) throws EntityInvalidException {
        Errors errors = new MapBindingResult(new HashMap<>(), "moduleprops");
        validator.validate(info, errors);
        if (errors.hasErrors()) {
            List<String> messages = new ArrayList<>();
            errors.getAllErrors().forEach(e -> messages.add(e.getDefaultMessage()));
            throw new EntityInvalidException(messages);
        }
    }

    /**
     * Import is applicable if module info id and version are equivalent.
     */
    @Override
    public boolean isApplicable(ModuleConfiguration configuration) {
        return info.getId().equals(configuration.getModule().getId())
                && info.getVersion().equals(configuration.getModule().getVersion());
    }

    @Override
    public ModuleInformation getModuleInformation() {
        return info;
    }
}
