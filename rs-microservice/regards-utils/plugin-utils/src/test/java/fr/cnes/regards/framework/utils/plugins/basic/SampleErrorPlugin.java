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
package fr.cnes.regards.framework.utils.plugins.basic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;

/**
 * SampleErrorPlugin
 * @author Christophe Mertz
 */
@Plugin(description = "Sample plugin test", id = "aSampleErrorPlugin", version = "0.0.1", author = "REGARDS Team",
        contact = "regards@c-s.fr", license = "GPLv3", owner = "CSSI", url = "https://github.com/RegardsOss")
public class SampleErrorPlugin implements ISamplePlugin {

    public static final String FIELD_NAME_SUFFIX = "suffix";

    public static final String FIELD_NAME_COEF = "coef";

    public static final String FIELD_NAME_ACTIVE = "isActive";

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SampleErrorPlugin.class);

    /**
     * A {@link String} parameter
     */
    @PluginParameter(description = "string parameter", label = "Suffix")
    private String suffix;

    /**
     * A {@link Integer} parameter
     */
    @PluginParameter(description = "int parameter", label = "Coeff")
    private Integer coef;

    /**
     * A {@link Boolean} parameter
     */
    @PluginParameter(description = "boolean parameter", label = "Enabled")
    private Boolean isActive;

    @Override
    public String echo(final String pMessage) {
        final StringBuilder str = new StringBuilder();
        if (isActive) {
            str.append(this.getClass().getName()).append(" -> ").append(pMessage).append(" - ").append(suffix);
        } else {

            str.append(this.getClass().getName()).append(":is not active");
        }
        return str.toString();
    }

    @Override
    public int add(final int pFist, final int pSecond) {
        final int res = coef * (pFist + pSecond);
        LOGGER.info("add result : " + res);
        return res;
    }

    /**
     * Init method
     */
    @PluginInit
    private void aInit() {
        LOGGER.info("Init method call : " + this.getClass().getName() + "suffixe:" + suffix + "|active:" + isActive
                + "|coeff:" + coef);
        throw new PluginUtilsRuntimeException("error");
    }

}
