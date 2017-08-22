/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.client;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.gson.JsonObject;

import fr.cnes.regards.framework.feign.annotation.RestClient;

/**
 * Feign client for calling rs-catalog's CatalogController#searchAll.
 * <p>
 * We can't gather all Feign clients of the module in a single one because of of the type-level {@link RequestMapping} annotation
 * which should be empty (or ""). This is not supported by Feign.
 *
 * @author Xavier-Alexandre Brochard
 */
@RestClient(name = "rs-catalog")
@RequestMapping(value = "/searchwithfacets", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@FunctionalInterface
public interface ISearchAllWithFacetsClient {

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<JsonObject> searchAll(@RequestParam final Map<String, String> allParams,
            @RequestParam(value = "facets", required = false) final String[] pFacets);
}