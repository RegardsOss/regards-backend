package fr.cnes.regards.modules.processing.rest;

import fr.cnes.regards.modules.processing.dto.PBatchRequest;
import fr.cnes.regards.modules.processing.dto.PBatchResponse;
import fr.cnes.regards.modules.processing.service.IBatchService;
import fr.cnes.regards.modules.processing.domain.factory.IPUserAuthFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.BATCH_PATH;

@RestController
@RequestMapping(
    produces = MediaType.APPLICATION_JSON_VALUE,
    consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE }
)
public class PBatchController {

    private final IBatchService batchService;
    private final IPUserAuthFactory authFactory;

    @Autowired
    public PBatchController(IBatchService batchService, IPUserAuthFactory authFactory) {
        this.batchService = batchService;
        this.authFactory = authFactory;
    }

    @PostMapping(path = BATCH_PATH)
    public Mono<PBatchResponse> createBatch(@RequestBody PBatchRequest data) {
        return ReactiveSecurityContextHolder.getContext()
            .flatMap(ctx -> batchService.checkAndCreateBatch(authFactory.fromContext(ctx), data)
                .map(b -> new PBatchResponse(b.getId(), b.getCorrelationId())));
    }

}
