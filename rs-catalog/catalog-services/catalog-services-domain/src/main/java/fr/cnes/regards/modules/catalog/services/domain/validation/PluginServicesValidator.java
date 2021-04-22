/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.catalog.services.domain.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.catalog.services.domain.annotations.PluginServices;
import fr.cnes.regards.modules.catalog.services.domain.plugins.IService;

/**
 * Validator enforcing {@link PluginServices} constraints
 *
 * @author Sylvain Vissiere-Guerinet
 */
public class PluginServicesValidator implements ConstraintValidator<PluginServices, PluginConfiguration> {

    @Override
    public void initialize(PluginServices pConstraintAnnotation) {
        // nothing to do
    }

    @Override
    public boolean isValid(PluginConfiguration pValue, ConstraintValidatorContext pContext) {
        return pValue != null && pValue.getInterfaceNames().contains(IService.class.getName());
    }
}
