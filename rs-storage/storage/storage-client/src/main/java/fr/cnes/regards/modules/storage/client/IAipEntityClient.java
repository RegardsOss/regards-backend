package fr.cnes.regards.modules.storage.client;

import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.storage.domain.database.AIPEntity;

/**
 * Feign client to interact with {@link AIPEntity}
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@RestClient(name = "rs-storage", contextId = "rs-storage.aip-entity.client")
@RequestMapping(value = IAipEntityClient.BASE_PATH, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface IAipEntityClient {

    /**
     * Client path path
     */
    String BASE_PATH = "sips/{sip_id}/aips";

    /**
     * Retrieve a page of AIPEntities from a given sip id
     * @param sipId
     * @param page page number
     * @param size page size
     * @return the page of AIPEntities
     */
    @RequestMapping(method = RequestMethod.GET)
    ResponseEntity<PagedResources<Resource<AIPEntity>>> retrieveAIPEntities(@PathVariable("sip_id") String sipId,
            @RequestParam("page") int page, @RequestParam("size") int size);
}
