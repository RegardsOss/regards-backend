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
package fr.cnes.regards.modules.catalog.services.plugins;

import java.io.ByteArrayInputStream;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.google.gson.GsonBuilder;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
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
@CatalogServicePlugin(applicationModes = { ServiceScope.ONE, ServiceScope.MANY }, entityTypes = { EntityType.DATA })
public class SampleServicePlugin implements ISampleServicePlugin {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SampleServicePlugin.class);

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
            optional = false)
    private String responseType;

    /**
     * Init method
     */
    @PluginInit
    private void aInit() {
        LOGGER.info("Init method call : " + this.getClass().getName() + RESPONSE_TYPE_PARAMETER + ":" + responseType);
    }

    @Override
    public ResponseEntity<InputStreamResource> applyOnEntities(List<String> pEntitiesId, HttpServletResponse response) {
        if ((pEntitiesId == null) || pEntitiesId.isEmpty()) {
            return apply("no entities", response);
        }
        return apply(String.format("Number of entities %d", pEntitiesId.size()), response);
    }

    @Override
    public ResponseEntity<InputStreamResource> applyOnQuery(String pOpenSearchQuery, EntityType pEntityType,
            HttpServletResponse response) {
        return apply(pOpenSearchQuery, response);
    }

    @Override
    public ResponseEntity<InputStreamResource> applyOnEntity(String pEntityId, HttpServletResponse response) {
        return apply(pEntityId, response);
    }

    /**
     * Sample method to return a result for the current service.
     *
     * @param pResultValue String to return in JSON or XML format
     * @param response HttpResponse
     * @return {@link ResponseEntity}
     */
    private ResponseEntity<InputStreamResource> apply(String pResultValue, HttpServletResponse response) {
        ResponseObject resp = new ResponseObject(pResultValue);

        GsonBuilder builder = new GsonBuilder();

        InputStreamResource respin = new InputStreamResource(
                new ByteArrayInputStream(builder.create().toJson(resp).getBytes()));
        HttpHeaders headers = new HttpHeaders();
        switch (responseType) {
            case RESPONSE_TYPE_JSON:
                headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=sampleServiceResults.json");
                // Simulate return of a JSON Object
                headers.setContentType(MediaType.APPLICATION_JSON);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                return new ResponseEntity<>(respin, headers, HttpStatus.OK);
            case RESPONSE_TYPE_XML:
                headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=sampleServiceResults.xml");
                // Simulate return of a XML Object
                headers.setContentType(MediaType.APPLICATION_XML);
                response.setContentType(MediaType.APPLICATION_XML_VALUE);
                String xmlString = String.format("<id>%s</id>", pResultValue);
                return new ResponseEntity<>(new InputStreamResource(new ByteArrayInputStream(xmlString.getBytes())),
                        headers, HttpStatus.OK);
            case RESPONSE_TYPE_IMG:
                // Simulate return of an image through the image format
                return retrieveImage(response);
            case RESPONSE_TYPE_OTHER:
                // Simulate return of an image through the octet-stream format
                return retrieveOther(response);

            default:
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Retrieve an image as an InputStream
     * @param response {@link HttpServletResponse}
     * @return {@link ResponseEntity}
     */
    private ResponseEntity<InputStreamResource> retrieveImage(HttpServletResponse response) {
        HttpHeaders headers = new HttpHeaders();
        InputStreamResource resource = new InputStreamResource(
                this.getClass().getClassLoader().getResourceAsStream("LogoCnes.png"));
        headers.setContentType(MediaType.IMAGE_PNG);
        response.setContentType(MediaType.IMAGE_PNG_VALUE);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=sampleService.png");
        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }

    /**
     * Retrieve an unkown extension file as an InputStream
     * @param response {@link HttpServletResponse}
     * @return {@link ResponseEntity}
     */
    private ResponseEntity<InputStreamResource> retrieveOther(HttpServletResponse response) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=sampleService.other");
        InputStreamResource resource = new InputStreamResource(
                this.getClass().getClassLoader().getResourceAsStream("result.other"));
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }
}
