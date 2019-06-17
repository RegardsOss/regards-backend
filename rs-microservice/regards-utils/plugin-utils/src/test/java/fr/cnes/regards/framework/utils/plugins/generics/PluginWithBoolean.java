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
package fr.cnes.regards.framework.utils.plugins.generics;

import org.junit.Assert;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;

/**
 * Test with primitive
 * @author Marc Sordi
 */
@Plugin(author = "REGARDS Team", description = "Plugin with boolean as primitive and object", id = "PluginWithBoolean",
        version = "1.0.0", contact = "regards@c-s.fr", license = "GPLv3", owner = "CNES",
        url = "https://regardsoss.github.io/")
public class PluginWithBoolean implements IPluginWithGenerics {

    // Attribute names
    public static final String FIELD_NAME_OBJECT = "ofield";

    public static final String FIELD_NAME_PRIMITIVE = "pfield";

    public static final String FIELD_NAME_STRING = "sfield";

    @PluginParameter(name = FIELD_NAME_OBJECT, label = "Object")
    private Boolean ofield;

    @PluginParameter(name = FIELD_NAME_PRIMITIVE, label = "Primitive")
    private boolean pfield;

    @PluginParameter(name = FIELD_NAME_STRING, label = "String")
    private String sfield;

    @Override
    public void doIt() {
        Assert.assertNotNull(ofield);
        Assert.assertNotNull(pfield);
        Assert.assertNotNull(sfield);
        Assert.assertEquals(6, sfield.length());
    }
}
