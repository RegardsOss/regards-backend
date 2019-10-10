package fr.cnes.regards.modules.ingest.client;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Set;

/**
 * Client interface for requesting the AIP service
 *
 * Client requests are done asynchronously.
 * To listen to the feedback messages, you have to implement your own {@link IAIPClientListener}.
 */
@RestClient(name = "rs-ingest")
@RequestMapping("/aips")
public interface IAIPRestClient {

    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "Return a page of AIPs")
    ResponseEntity<PagedResources<Resource<AIPEntity>>> searchAIPs(
        AIPState aipState,
        Set<String> subsettingTags,
        OffsetDateTime date,
        @RequestParam("page") int page,
        @RequestParam("size") int size
    );
}
