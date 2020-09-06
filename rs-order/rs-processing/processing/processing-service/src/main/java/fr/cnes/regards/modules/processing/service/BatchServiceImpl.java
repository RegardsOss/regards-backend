package fr.cnes.regards.modules.processing.service;

import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PProcess;
import fr.cnes.regards.modules.processing.domain.PUserAuth;
import fr.cnes.regards.modules.processing.domain.constraints.Violation;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionStringParameterValue;
import fr.cnes.regards.modules.processing.dto.PBatchRequest;
import fr.cnes.regards.modules.processing.repository.IPBatchRepository;
import fr.cnes.regards.modules.processing.repository.IPProcessRepository;
import fr.cnes.regards.modules.processing.service.exception.ProcessConstraintViolationsException;
import io.vavr.collection.Seq;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class BatchServiceImpl implements IBatchService {

    private final IPProcessRepository processRepo;
    private final IPBatchRepository batchRepo;

    @Autowired
    public BatchServiceImpl(
            IPProcessRepository processRepo,
            IPBatchRepository batchRepo
    ) {
        this.processRepo = processRepo;
        this.batchRepo = batchRepo;
    }

    @Override public Mono<PBatch> checkAndCreateBatch(PUserAuth auth, PBatchRequest data) {
        return processRepo.findByTenantAndProcessName(auth.getTenant(), data.getProcessName())
            .flatMap(p -> createBatch(p, data)
                .flatMap(b -> checkBatch(p, b)));
    }

    private Mono<Seq<Violation>> check(PProcess process, PBatch batch) {
        return process.getBatchChecker().validate(batch);
    }

    private Mono<PBatch> createBatch(PProcess process, PBatchRequest data) {
        return Mono.just(new PBatch(
            data.getCorrelationId(),
            UUID.randomUUID(),
            process.getBusinessId(),
            process.getProcessName(),
            data.getTenant(),
            data.getUser(),
            data.getUserRole(),
            paramValues(data),
            data.getFilesetsByDataset(),
            false)
        );
    }

    private Seq<ExecutionStringParameterValue> paramValues(PBatchRequest data) {
        return data.getParameters().toList()
            .map(entry -> new ExecutionStringParameterValue(entry._1, entry._2));
    }

    private Mono<? extends PBatch> checkBatch(PProcess process, PBatch batch) {
        return check(process, batch)
            .flatMap(vs -> {
                if (vs.isEmpty()) {
                    return Mono.just(batch);
                }
                else {
                    return Mono.error(new ProcessConstraintViolationsException(vs));
                }
            })
            .switchIfEmpty(Mono.just(batch));
    }

}
