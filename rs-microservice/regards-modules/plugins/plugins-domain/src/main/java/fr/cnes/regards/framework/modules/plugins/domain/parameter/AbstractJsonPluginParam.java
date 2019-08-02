/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.plugins.domain.parameter;

import javax.validation.constraints.NotBlank;

/**
 * Parameter associated to a plugin configuration.
 * This type of plugin parameter is only use for complex type (de)serialization.
 * @author Marc SORDI
 *
 * @param <T> parameter type
 */
public abstract class AbstractJsonPluginParam<T> extends AbstractPluginParam<T> {

    @NotBlank(message = "Fully qualified class name is required")
    protected String clazz;

    public AbstractJsonPluginParam(PluginParamType type) {
        super(type);
    }

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }
}
