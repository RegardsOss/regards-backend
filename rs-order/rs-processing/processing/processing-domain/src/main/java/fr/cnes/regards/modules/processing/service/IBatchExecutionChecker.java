package fr.cnes.regards.modules.processing.service;

import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PProcess;
import fr.cnes.regards.modules.processing.domain.constraints.Violation;
import io.vavr.collection.Seq;
import reactor.core.publisher.Mono;

public interface IBatchExecutionChecker {

    Mono<Seq<Violation>> check(PProcess process, PBatch batch);

}
