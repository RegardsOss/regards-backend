package fr.cnes.regards.modules.processing.service;

import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.dto.PBatchRequest;
import fr.cnes.regards.modules.processing.dto.PBatchResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;

public interface IBatchService {

    Mono<PBatch> checkAndCreateBatch(PBatchRequest data);

}
