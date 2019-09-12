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

/**
 * Supported plugin parameter.<br/>
 * Nested plugin param keeps the reference to another plugin configuration. Business key has to be used.
 *
 * @author Marc SORDI
 *
 */
public class NestedPluginParam extends AbstractPluginParam<String> {

    public NestedPluginParam() {
        super(PluginParamType.PLUGIN);
    }
}
