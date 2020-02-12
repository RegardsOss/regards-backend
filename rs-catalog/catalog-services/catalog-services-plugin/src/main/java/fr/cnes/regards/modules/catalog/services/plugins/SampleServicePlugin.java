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
package fr.cnes.regards.modules.catalog.services.plugins;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.catalog.services.domain.annotations.CatalogServicePlugin;
import fr.cnes.regards.modules.catalog.services.helper.CatalogPluginResponseFactory;
import fr.cnes.regards.modules.catalog.services.helper.CatalogPluginResponseFactory.CatalogPluginResponseType;

/**
 * SampleServicePlugin
 *
 * @author Christophe Mertz
 */
@Plugin(description = "Sample plugin test", id = SampleServicePlugin.PLUGIN_ID, version = "0.0.1",
        author = "REGARDS Dream Team", contact = "regards@c-s.fr", license = "GPLv3", owner = "CSSI",
        url = "https://github.com/RegardsOss")
@CatalogServicePlugin(applicationModes = { ServiceScope.ONE, ServiceScope.MANY }, entityTypes = { EntityType.DATA })
public class SampleServicePlugin extends AbstractCatalogServicePlugin implements ISampleServicePlugin {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SampleServicePlugin.class);

    public static final String PLUGIN_ID = "aSampleServicePlugin";

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
     * A {@link String} parameter
     */
    @PluginParameter(description = "Response type", name = RESPONSE_TYPE_PARAMETER, defaultValue = RESPONSE_TYPE_JSON,
            optional = false, label = "Response type")
    private String responseType;

    /**
     * Init method
     */
    @PluginInit
    private void aInit() {
        LOGGER.info("Init method call : " + this.getClass().getName() + RESPONSE_TYPE_PARAMETER + ":" + responseType);
    }

    @Override
    public ResponseEntity<StreamingResponseBody> applyOnEntities(List<String> pEntitiesId,
            HttpServletResponse response) {
        if ((pEntitiesId == null) || pEntitiesId.isEmpty()) {
            return apply("no entities", response);
        }
        return apply(String.format("Number of entities %d", pEntitiesId.size()), response);
    }

    @Override
    public ResponseEntity<StreamingResponseBody> applyOnQuery(String pOpenSearchQuery, EntityType pEntityType,
            HttpServletResponse response) {
        return apply(pOpenSearchQuery, response);
    }

    @Override
    public ResponseEntity<StreamingResponseBody> applyOnEntity(String pEntityId, HttpServletResponse response) {
        return apply(pEntityId, response);
    }

    /**
     * Sample method to return a result for the current service.
     *
     * @param pResultValue String to return in JSON or XML format
     * @param response HttpResponse
     * @return {@link ResponseEntity}
     */
    private ResponseEntity<StreamingResponseBody> apply(String pResultValue, HttpServletResponse response) {
        ResponseEntity<StreamingResponseBody> streamResponse = null;
        ResponseObject resp = new ResponseObject(pResultValue);
        switch (responseType) {
            case RESPONSE_TYPE_JSON:
                LOGGER.info("[Sample plugin] Applying for JSON format response");
                streamResponse = CatalogPluginResponseFactory
                        .createSuccessResponse(response, CatalogPluginResponseType.JSON, resp);
                break;
            case RESPONSE_TYPE_XML:
                LOGGER.info("[Sample plugin] Applying for XML format response");
                streamResponse = CatalogPluginResponseFactory
                        .createSuccessResponse(response, CatalogPluginResponseType.XML, resp);
                break;
            case RESPONSE_TYPE_IMG:
                LOGGER.info("[Sample plugin] Applying for IMG format response");
                InputStreamResource resource = new InputStreamResource(
                        this.getClass().getClassLoader().getResourceAsStream("LogoCnes.png"));
                try {
                    streamResponse = CatalogPluginResponseFactory
                            .createSuccessResponseFromInputStream(response, CatalogPluginResponseType.FILE_IMG_PNG,
                                                                  resource.getInputStream(), "LogoCnes.png");
                } catch (IOException e) {
                    LOGGER.error("Error sending file", e);
                    streamResponse = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                }
                break;
            case RESPONSE_TYPE_OTHER:
                LOGGER.info("[Sample plugin] Applying for Other format response");
                InputStreamResource resourceDownload = new InputStreamResource(
                        this.getClass().getClassLoader().getResourceAsStream("result.other"));
                try {
                    streamResponse = CatalogPluginResponseFactory
                            .createSuccessResponseFromInputStream(response, CatalogPluginResponseType.FILE_DOWNLOAD,
                                                                  resourceDownload.getInputStream(), "result.other");
                } catch (IOException e) {
                    LOGGER.error("Error sending file", e);
                    streamResponse = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                }
                break;
            default:
                streamResponse = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                break;
        }
        LOGGER.info("[Sample plugin] Response {}", streamResponse.getStatusCodeValue());
        return streamResponse;
    }
}
