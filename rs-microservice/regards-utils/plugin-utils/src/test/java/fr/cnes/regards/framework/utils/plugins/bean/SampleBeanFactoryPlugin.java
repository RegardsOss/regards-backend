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
package fr.cnes.regards.framework.utils.plugins.bean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;

/**
 * SampleBeanFactoryPlugin
 * @author Christophe Mertz
 */
@Plugin(description = "Sample plugin test", id = "SampleBeanFactoryPlugin", version = "0.0.1", author = "REGARDS Team",
        contact = "regards@c-s.fr", license = "GPLv3", owner = "CSSI", url = "https://github.com/RegardsOss")
public class SampleBeanFactoryPlugin implements ISamplePlugin {

    public static final String FIELD_NAME_SUFFIX = "suffix";

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SampleBeanFactoryPlugin.class);

    /**
     * A {@link String} parameter
     */
    @PluginParameter(description = "string parameter", label = "Suffix")
    private String suffix;

    /**
     * An Autowired field
     */
    @Autowired
    private ISampleBeanService sampleBeanService;

    @Override
    public String echo(final String pMessage) {
        sampleBeanService.setId("---> add string with PluginService");
        return this.getClass().getCanonicalName() + " -> " + pMessage + " - " + suffix + sampleBeanService.getId();
    }

    /**
     * Init method
     */
    @PluginInit
    private void aInit() {
        LOGGER.info("Init method call : " + " suffixe=" + suffix);
    }

}
