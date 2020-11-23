package fr.cnes.regards.modules.processing.rest;

import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.BATCH_PATH;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.modules.processing.domain.dto.PBatchRequest;
import fr.cnes.regards.modules.processing.domain.dto.PBatchResponse;
import fr.cnes.regards.modules.processing.domain.service.IBatchService;
import fr.cnes.regards.modules.processing.domain.service.IPUserAuthService;
import reactor.core.publisher.Mono;

@RestController
@ConditionalOnProperty(name = "spring.main.web-application-type", havingValue = "reactive")
@RequestMapping(path = BATCH_PATH)
public class PBatchReactiveController {

    private final IBatchService batchService;

    private final IPUserAuthService authFactory;

    @Autowired
    public PBatchReactiveController(IBatchService batchService, IPUserAuthService authFactory) {
        this.batchService = batchService;
        this.authFactory = authFactory;
    }

    @PostMapping
    public Mono<PBatchResponse> createBatch(@RequestBody PBatchRequest data) {
        return ReactiveSecurityContextHolder.getContext()
                .flatMap(ctx -> batchService.checkAndCreateBatch(authFactory.fromContext(ctx), data)
                        .map(b -> new PBatchResponse(b.getId(), b.getCorrelationId())));
    }

}
