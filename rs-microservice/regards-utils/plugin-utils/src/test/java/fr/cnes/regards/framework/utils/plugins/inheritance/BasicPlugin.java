/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.junit.Assert;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;

/**
 * @author Marc Sordi
 */
@Plugin(author = "REGARDS Team", description = "Plugin for inheritance testing", id = "BasicPlugin", version = "1.0.0",
        contact = "regards@c-s.fr", license = "GPLv3", owner = "CNES", url = "https://regardsoss.github.io/")
public class BasicPlugin extends AbstractPlugin {

    public static final String FIELD_NAME_STRING = "field";

    @PluginParameter(name = FIELD_NAME_STRING, label = "String")
    private String field;

    @Override
    public void doIt() {
        Assert.assertNotNull(field);
        Assert.assertNotNull(inheritedField);
    }
}
