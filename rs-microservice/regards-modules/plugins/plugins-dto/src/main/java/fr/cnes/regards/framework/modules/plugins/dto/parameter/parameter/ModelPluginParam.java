/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter;

/**
 * Model plugin parameter
 * REGARDS_ENTITY_MODEL is not a java native type.
 * This type is used to indicates that the parameter must match a REGARDS Model name.
 * Models are entity description defined in data-management microservice.
 *
 * @author mnguyen0
 */
public class ModelPluginParam extends AbstractPluginParam<String> {

    public ModelPluginParam() {
        super(PluginParamType.REGARDS_ENTITY_MODEL);
    }

    @Override
    public boolean supportsDefaultValue() {
        return true;
    }

    @Override
    public void applyDefaultValue(String value) {
        if (!hasValue()) {
            this.value = value;
        }
    }
}
