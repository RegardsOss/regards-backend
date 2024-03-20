/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.PluginOverridenParamType;
import fr.cnes.regards.framework.utils.cycle.detection.SomeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Set;

/**
 * SamplePlugin
 *
 * @author Christophe Mertz
 */
@Plugin(description = "Sample plugin test",
        markdown = "suffix.md",
        userMarkdown = "suffixUser.md",
        id = "aSamplePlugin",
        version = "0.0.1",
        author = "REGARDS Team",
        contact = "regards@c-s.fr",
        license = "GPLv3",
        owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class SamplePlugin implements ISamplePlugin {

    public static final String FIELD_NAME_SUFFIX = "suffix";

    public static final String FIELD_NAME_SUFFIX_USER = "suffixUser";

    public static final String FIELD_NAME_COEF = "coef";

    public static final String FIELD_NAME_ACTIVE = "isActive";

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SamplePlugin.class);

    /**
     * A {@link String} parameter
     */
    @PluginParameter(description = "short description, see markdown for detailed description",
                     markdown = "suffix.md",
                     label = "Suffix")
    private String suffix;

    /**
     * A {@link Integer} parameter
     */
    @PluginParameter(description = "int parameter", label = "Coeff", defaultValue = "-100")
    private Integer coef;

    /**
     * A {@link Boolean} parameter
     */
    @PluginParameter(description = "boolean parameter", label = "Enabled")
    private Boolean isActive;

    @PluginParameter(description = "a list of enumerated values",
                     label = "An enumerated inside a list",
                     optional = true)
    private Set<SomeEnum> someEnums;

    /**
     * Map of primitive parameter
     */
    @PluginParameter(keylabel = "mapKey",
                     description = "a list of enumerated values",
                     label = "An enumerated inside a list",
                     optional = true)
    private HashMap<String, Boolean> mapParam;

    /**
     * A Model parameter
     */
    @PluginParameter(description = "model parameter",
                     label = "model",
                     type = PluginOverridenParamType.REGARDS_ENTITY_MODEL,
                     optional = true)
    private String model;

    /**
     * A Model collection parameter
     */
    @PluginParameter(description = "model collection parameter",
                     label = "models",
                     type = PluginOverridenParamType.REGARDS_ENTITY_MODEL,
                     optional = true)
    private Set<String> models;

    /**
     * A Model Map parameter
     */
    @PluginParameter(keylabel = "modelsMapKey",
                     description = "model map parameter",
                     label = "Map Models",
                     type = PluginOverridenParamType.REGARDS_ENTITY_MODEL,
                     optional = true)
    private HashMap<Integer, String> mapModels;

    /**
     * A Plugin parameter
     */
    @PluginParameter(description = "plugin parameter", label = "Plugin", optional = true)
    private ISamplePlugin plugin;

    /**
     * A plugin collection parameter
     */
    @PluginParameter(description = "plugin collection parameter", label = "Set Plugins", optional = true)
    private Set<ISamplePlugin> plugins;

    /**
     * A plugin map parameter
     */
    @PluginParameter(keylabel = "pluginsMapKey",
                     description = "plugin map parameter",
                     label = "Map Plugins",
                     optional = true)
    private HashMap<String, ISamplePlugin> mapPlugins;

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
        LOGGER.info("Init method call : "
                    + this.getClass().getName()
                    + "suffixe:"
                    + suffix
                    + "|active:"
                    + isActive
                    + "|coeff:"
                    + coef);
    }

}
