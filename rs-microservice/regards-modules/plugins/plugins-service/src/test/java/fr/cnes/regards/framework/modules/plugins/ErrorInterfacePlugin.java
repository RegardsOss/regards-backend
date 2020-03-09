/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;

/**
 * ISamplePlugin
 * @author Christophe Mertz
 */
@Plugin(description = "Sample plugin test", id = "anErrorPluginInterface", version = "0.0.1", author = "REGARDS Team",
        contact = "regards@c-s.fr", license = "GPLv3", owner = "CSSI", url = "https://github.com/RegardsOss")
public class ErrorInterfacePlugin implements IComplexInterfacePlugin {

    public static final String FIELD_NAME = "aLong";

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorInterfacePlugin.class);

    /**
     * A {@link Long} parameter
     */
    @PluginParameter(description = "long parameter", label = "LONG_PARAM")
    private Long aLong;

    /**
     * Init method
     */
    @PluginInit
    private void aInit() {
        LOGGER.info("Init method call : " + this.getClass().getName() + "|long:" + aLong);
    }

    @Override
    public int mult(final int pFirst, final int pSecond) {
        return 0;
    }

}
