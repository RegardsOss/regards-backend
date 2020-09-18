package fr.cnes.regards.modules.processing.storage;

import fr.cnes.regards.modules.processing.domain.POutputFile;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import io.vavr.collection.Seq;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * This service allows to share execution results by moving them to a shared
 * folder, accessible from the caller.
 */
public interface ISharedStorageService {

    Mono<Seq<POutputFile>> storeResult(ExecutionContext ctx, ExecutionLocalWorkdir workdir);

    Mono<POutputFile> delete(POutputFile outFile);
}
