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
package fr.cnes.regards.framework.utils.plugins.plugintypes;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.utils.plugins.basic.ISamplePlugin;

/**
 * ISamplePlugin
 * @author Christophe Mertz
 */
@Plugin(description = "Complex plugin test", id = "aComplexErrorPlugin", version = "0.0.1", author = "REGARDS Team",
        contact = "regards@c-s.fr", license = "GPLv3", owner = "CSSI", url = "https://github.com/RegardsOss")
public class ComplexErrorPlugin implements ISamplePlugin {

    // Field name
    public static final String FIELD_NAME_COEF = "coef";

    // Field name
    public static final String FIELD_NAME_PLUGIN = "interfacePlugin";

    /**
     * A {@link Integer} parameter
     */
    @PluginParameter(description = "int parameter", label = "Coefficient")
    private final Integer coef = 0;

    /**
     * A {@link String} parameter
     */
    @PluginParameter(description = "plugin parameter", label = "Embedded plugin")
    private INotInterfacePlugin interfacePlugin;

    @Override
    public String echo(String pMessage) {
        return this.getClass().getName() + "-" + pMessage + interfacePlugin.toString();
    }

    @Override
    public int add(int pFist, int pSecond) {
        return 0;
    }

}
