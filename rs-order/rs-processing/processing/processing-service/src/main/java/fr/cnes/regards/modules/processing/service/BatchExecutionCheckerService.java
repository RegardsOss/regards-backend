package fr.cnes.regards.modules.processing.service;

import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.constraints.ExecutionConstraintViolation;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import org.springframework.stereotype.Service;

@Service
public class BatchExecutionCheckerService implements IBatchExecutionChecker {

    @Override public Seq<ExecutionConstraintViolation> check(PBatch batch) {
        return checkQuotas(batch)
                .appendAll(checkRights(batch))
                .appendAll(checkParameters(batch));
    }

    private Seq<ExecutionConstraintViolation> checkQuotas(PBatch batch) {
        // TODO
        return List.empty();
    }

    private Seq<ExecutionConstraintViolation> checkRights(PBatch batch) {
        // TODO
        return List.empty();
    }

    private Seq<ExecutionConstraintViolation> checkParameters(PBatch batch) {
        // TODO
        return List.empty();
    }
}
