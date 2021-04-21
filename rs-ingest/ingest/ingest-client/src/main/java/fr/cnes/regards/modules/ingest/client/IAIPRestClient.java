package fr.cnes.regards.modules.ingest.client;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;
import fr.cnes.regards.modules.ingest.dto.request.OAISDeletionPayloadDto;

/**
 * Client interface for requesting the AIP service
 *
 * @author Simon MILHAU
 */
@RestClient(name = "rs-ingest", contextId = "rs-ingest.rest.client")
@RequestMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
public interface IAIPRestClient {

    static final String DELETE_BY_SESSION_PATH = "/aips/delete";

    @RequestMapping(method = RequestMethod.POST, path = "/aips")
    ResponseEntity<PagedModel<EntityModel<AIPEntity>>> searchAIPs(@RequestBody SearchAIPsParameters filters,
            @RequestParam("page") int page, @RequestParam("size") int size);

    @RequestMapping(value = IAIPRestClient.DELETE_BY_SESSION_PATH, method = RequestMethod.POST)
    void delete(@RequestBody OAISDeletionPayloadDto deletionRequest);
}
