package fr.cnes.regards.modules.processing.service;

import fr.cnes.regards.modules.processing.domain.PUserAuth;
import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.dto.PBatchRequest;
import reactor.core.publisher.Mono;

public interface IBatchService {

    Mono<PBatch> checkAndCreateBatch(PUserAuth auth, PBatchRequest data);

}
