package fr.cnes.regards.modules.processing.domain.service;

import fr.cnes.regards.modules.processing.domain.PUserAuth;
import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.dto.PBatchRequest;
import reactor.core.publisher.Mono;

public interface IBatchService {

    Mono<PBatch> checkAndCreateBatch(PUserAuth auth, PBatchRequest data);

}
