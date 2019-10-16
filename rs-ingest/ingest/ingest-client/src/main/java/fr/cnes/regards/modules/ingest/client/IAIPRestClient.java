package fr.cnes.regards.modules.ingest.client;

import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;

/**
 * Client interface for requesting the AIP service
 *
 * @author Simon MILHAU
 */
@RestClient(name = "rs-ingest", contextId = "rs-ingest.rest.client")
@RequestMapping(consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface IAIPRestClient {

    @RequestMapping(method = RequestMethod.GET, path = "/aips")
    @ResourceAccess(description = "Return a page of AIPs")
    ResponseEntity<PagedResources<Resource<AIPEntity>>> searchAIPs(@RequestParam SearchAIPsParameters filters,
            @RequestParam("page") int page, @RequestParam("size") int size);
}
