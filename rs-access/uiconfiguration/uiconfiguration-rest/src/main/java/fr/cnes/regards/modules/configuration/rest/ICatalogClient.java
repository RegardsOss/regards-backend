/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.rest;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.gson.JsonObject;

import fr.cnes.regards.framework.feign.annotation.RestClient;

/**
 * Feign client for calling rs-catalog's {@link CatalogController#searchDataobjects}
 *
 * @author Xavier-Alexandre Brochard
 */
@RestClient(name = "rs-catalog")
@RequestMapping(value = "/dataobjects/search", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface ICatalogClient {

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<JsonObject> searchDataobjects(
            @RequestParam(required = false) final Map<String, String> allParams);
}