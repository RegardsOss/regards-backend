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
package fr.cnes.regards.modules.catalog.services.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.catalog.services.domain.annotations.CatalogServicePlugin;
import fr.cnes.regards.modules.models.domain.EntityType;

/**
 * SampleServicePlugin
 *
 * @author Christophe Mertz
 */
@Plugin(description = "Sample plugin test", id = "aSampleServicePlugin", version = "0.0.1",
        author = "REGARDS Dream Team", contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI",
        url = "https://github.com/RegardsOss")
@CatalogServicePlugin(applicationModes = { ServiceScope.ONE, ServiceScope.QUERY }, entityTypes = { EntityType.DATASET })
public class SampleServicePlugin implements ISampleServicePlugin {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SampleServicePlugin.class);

    /**
     * A {@link String} parameter
     */
    @PluginParameter(description = "string parameter", name = SUFFIXE, defaultValue = "Hello", optional = true)
    private String suffix;

    /**
     * A {@link Integer} parameter
     */
    @PluginParameter(description = "int parameter", name = COEFF)
    private Integer coef;

    /**
     * A {@link Boolean} parameter
     */
    @PluginParameter(description = "boolean parameter", name = ACTIVE, defaultValue = "false")
    private Boolean isActive;

    @Override
    public String echo(final String pMessage) {
        StringBuilder str = new StringBuilder();
        if (isActive) {
            str.append(pMessage + " --> the suffix is '" + suffix + "'");
        } else {

            str.append(this.getClass().getName() + ":is not active");
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
        LOGGER.info("Init method call : " + this.getClass().getName() + SUFFIXE + ":" + suffix + "|" + ACTIVE + ":"
                + isActive + "|" + COEFF + ":" + coef);
    }

    @Override
    public ResponseEntity<?> apply() {
        if (!isActive) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        int n = add(10, 20);
        return ResponseEntity.ok(echo("res=" + n));
    }
}
