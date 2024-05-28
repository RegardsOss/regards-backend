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
package fr.cnes.regards.modules.backendforfrontend.rest;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.catalog.services.domain.ServicePluginParameters;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.catalog.services.domain.annotations.CatalogServicePlugin;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * SampleServicePlugin
 *
 * @author Christophe Mertz
 */
@Plugin(description = "Sample plugin test",
        id = "aSampleServicePlugin",
        version = "0.0.1",
        author = "REGARDS Dream Team",
        contact = "regards@c-s.fr",
        license = "GPLv3",
        owner = "CSSI",
        url = "https://github.com/RegardsOss")
@CatalogServicePlugin(applicationModes = { ServiceScope.ONE, ServiceScope.MANY }, entityTypes = { EntityType.DATA })
public class SampleServicePlugin implements ISampleServicePlugin {

    /**
     * Name of the response type parameter
     */
    public static final String RESPONSE_TYPE_PARAMETER = "responseType";

    /**
     * Available value for the responseType parameter
     */
    public static final String RESPONSE_TYPE_JSON = "json";

    /**
     * Available value for the responseType parameter
     */
    public static final String RESPONSE_TYPE_XML = "xml";

    /**
     * Available value for the responseType parameter
     */
    public static final String RESPONSE_TYPE_IMG = "image";

    /**
     * Available value for the responseType parameter
     */
    public static final String RESPONSE_TYPE_OTHER = "other";

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SampleServicePlugin.class);

    /**
     * A {@link String} parameter
     */
    @PluginParameter(description = "Response type",
                     name = RESPONSE_TYPE_PARAMETER,
                     defaultValue = RESPONSE_TYPE_JSON,
                     optional = false,
                     label = "Response type")
    private String responseType;

    /**
     * Init method
     */
    @PluginInit
    private void aInit() {
        LOGGER.info("Init method call : " + this.getClass().getName() + RESPONSE_TYPE_PARAMETER + ":" + responseType);
    }

    @Override
    public ResponseEntity<StreamingResponseBody> apply(ServicePluginParameters pParameters,
                                                       HttpServletResponse response) {
        return null;
    }

}
