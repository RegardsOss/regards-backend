package fr.cnes.regards.modules.processing.service;

import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PProcess;
import fr.cnes.regards.modules.processing.domain.constraints.Violation;
import io.vavr.collection.Seq;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class BatchExecutionCheckerService implements IBatchExecutionChecker {

    @Override public Mono<Seq<Violation>> check(PProcess process, PBatch batch) {
        return process.getBatchChecker().validate(batch);
    }

}
