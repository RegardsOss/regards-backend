package fr.cnes.regards.modules.processing.rest;

import fr.cnes.regards.modules.processing.dto.PBatchRequest;
import fr.cnes.regards.modules.processing.dto.PBatchResponse;
import fr.cnes.regards.modules.processing.service.IBatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.BATCH_PATH;

@RestController
@RequestMapping(BATCH_PATH)
public class PBatchController {

    private final IBatchService batchService;

    @Autowired
    public PBatchController(IBatchService batchService) {
        this.batchService = batchService;
    }

    @RequestMapping(method = RequestMethod.POST)
    public Mono<PBatchResponse> createBatch(@RequestBody PBatchRequest data) {
        return batchService.checkAndCreateBatch(data)
                .map(b -> new PBatchResponse(b.getId(), b.getCorrelationId()));
    }

}
