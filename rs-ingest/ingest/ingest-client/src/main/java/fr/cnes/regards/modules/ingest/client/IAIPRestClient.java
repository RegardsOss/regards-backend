package fr.cnes.regards.modules.ingest.client;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;
import fr.cnes.regards.modules.ingest.dto.request.OAISDeletionPayloadDto;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Client interface for requesting the AIP service
 *
 * @author Simon MILHAU
 */
@RestClient(name = "rs-ingest", contextId = "rs-ingest.rest.client")
public interface IAIPRestClient {

    String ROOT_PATH = "/aips";

    String DELETE_BY_SESSION_PATH = "/delete";

    @PostMapping(path = ROOT_PATH, consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PagedModel<EntityModel<AIPEntity>>> searchAIPs(@RequestBody SearchAIPsParameters filters,
                                                                  @RequestParam("page") int page,
                                                                  @RequestParam("size") int size);

    @PostMapping(value = ROOT_PATH + DELETE_BY_SESSION_PATH, consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    void delete(@RequestBody OAISDeletionPayloadDto deletionRequest);
}
