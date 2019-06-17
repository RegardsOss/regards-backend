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
package fr.cnes.regards.framework.utils.plugins.plugintypes;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;

/**
 * ParameterPlugin
 * @author Christophe Mertz
 */
@Plugin(description = "Parameter plugin test", id = "aParameterPlugin", version = "0.0.1", author = "REGARDS Team",
        contact = "regards@c-s.fr", license = "GPLv3", owner = "CSSI", url = "https://github.com/RegardsOss")
public class AParameterPluginImplementation implements IComplexInterfacePlugin {

    // Field name
    public static final String FIELD_NAME = "ll";

    /**
     * A {@link Integer} parameter
     */
    @PluginParameter(description = "long parameter", label = "Long parameter")
    private Long ll;

    @Override
    public int mult(final int pFirst, final int pSecond) {

        return ll.intValue() * (pFirst * pSecond);
    }

}
