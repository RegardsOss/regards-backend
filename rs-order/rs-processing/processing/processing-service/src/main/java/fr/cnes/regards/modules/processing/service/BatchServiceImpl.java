package fr.cnes.regards.modules.processing.service;

import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PProcess;
import fr.cnes.regards.modules.processing.domain.constraints.ExecutionConstraintViolation;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionStringParameterValue;
import fr.cnes.regards.modules.processing.repository.IPBatchRepository;
import fr.cnes.regards.modules.processing.repository.IPProcessRepository;
import fr.cnes.regards.modules.processing.dto.PBatchRequest;
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
    private final IBatchExecutionChecker checkerService;

    @Autowired
    public BatchServiceImpl(IPProcessRepository processRepo, IPBatchRepository batchRepo,
            IBatchExecutionChecker checkerService) {
        this.processRepo = processRepo;
        this.batchRepo = batchRepo;
        this.checkerService = checkerService;
    }

    @Override public Mono<PBatch> checkAndCreateBatch(PBatchRequest data) {
        return processRepo.findByName(data.getProcessName())
                .flatMap(p -> createBatch(p, data))
                .flatMap(b -> checkBatch(b));
    }

    private Mono<PBatch> createBatch(PProcess process, PBatchRequest data) {
        return Mono.just(new PBatch(
                data.getCorrelationId(),
                UUID.randomUUID(),
                data.getProcessName(),
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

    private Mono<? extends PBatch> checkBatch(PBatch batch) {
        Seq<ExecutionConstraintViolation> violations = checkerService.check(batch);
        return violations.isEmpty()
                ? Mono.just(batch)
                : Mono.error(new ProcessConstraintViolationsException(violations));
    }

}
