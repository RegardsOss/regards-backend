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
import fr.cnes.regards.modules.search.rest.CatalogController;

/**
 * Feign client for calling rs-catalog's {@link CatalogController#searchCollections}.
 * <p>
 * We can't gather all Feign clients of the module in a single one because of of the type-level {@link RequestMapping} annotation
 * which should be empty (or ""). This is not supported by Feign.
 *
 * @author Xavier-Alexandre Brochard
 */
@RestClient(name = "rs-catalog")
@RequestMapping(value = CatalogController.COLLECTIONS_SEARCH, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@FunctionalInterface
public interface ISearchCollectionsClient {

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<JsonObject> searchCollections(@RequestParam final Map<String, String> allParams);
}