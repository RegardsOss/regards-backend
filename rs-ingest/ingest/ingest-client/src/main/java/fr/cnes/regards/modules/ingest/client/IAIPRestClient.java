package fr.cnes.regards.modules.ingest.client;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.ingest.dto.AIPEntityLightDto;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;
import fr.cnes.regards.modules.ingest.dto.request.OAISDeletionPayloadDto;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.SlicedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Client interface for requesting the AIP service
 *
 * @author Simon MILHAU
 */
@RestClient(name = "rs-ingest", contextId = "rs-ingest.rest.client")
public interface IAIPRestClient {

    String ROOT_PATH = "/aips";

    String SLICE_PATH = "/slice";

    String DELETE_BY_SESSION_PATH = "/delete";

    /**
     * Retrieve a page of AIPs based on the provided filters, page number, size, and sort order.
     */
    default ResponseEntity<PagedModel<EntityModel<AIPEntityLightDto>>> searchAIPs(SearchAIPsParameters filters,
                                                                                  int page,
                                                                                  int size,
                                                                                  Sort sort) {
        return searchAIPs(PageRequest.of(page, size, sort), filters);
    }

    /**
     * Retrieve a slice of AIPs based on the provided filters, page number, size, and sort order.
     *
     * @see Slice
     * @see SlicedModel
     */
    default ResponseEntity<SlicedModel<EntityModel<AIPEntityLightDto>>> searchAIPsSlice(SearchAIPsParameters filters,
                                                                                        int page,
                                                                                        int size,
                                                                                        Sort sort) {
        return searchAIPsSlice(PageRequest.of(page, size, sort), filters);
    }

    /**
     * You better use {@link #searchAIPs(SearchAIPsParameters, int, int, Sort)} which explicitly asks for sort parameter
     */
    @PostMapping(path = ROOT_PATH,
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PagedModel<EntityModel<AIPEntityLightDto>>> searchAIPs(@SpringQueryMap Pageable pageable,
                                                                          @RequestBody SearchAIPsParameters filters);

    /**
     * You better use {@link #searchAIPsSlice(SearchAIPsParameters, int, int, Sort)} (SearchAIPsParameters, int, int, Sort)} which explicitly asks for sort parameter
     */
    @PostMapping(path = ROOT_PATH + SLICE_PATH,
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<SlicedModel<EntityModel<AIPEntityLightDto>>> searchAIPsSlice(@SpringQueryMap Pageable pageable,
                                                                                @RequestBody
                                                                                SearchAIPsParameters filters);

    @PostMapping(value = ROOT_PATH + DELETE_BY_SESSION_PATH,
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    void delete(@RequestBody OAISDeletionPayloadDto deletionRequest);
}
