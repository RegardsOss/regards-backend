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
package fr.cnes.regards.framework.utils.plugins.inheritance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;

/**
 * @author Marc Sordi
 *
 */
public abstract class AbstractPlugin implements IBasicPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPlugin.class);

    public static final String INHERITED_FIELD_NAME_STRING = "inheritedField";

    @PluginParameter(name = INHERITED_FIELD_NAME_STRING, label = "Inherited string")
    protected String inheritedField;

    @PluginInit
    protected void init() {
        LOGGER.info("Init the plugin in an inherited class"); // Just for manual test!
    }
}
