package fr.cnes.regards.modules.processing.service;

import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.constraints.ExecutionConstraintViolation;
import io.vavr.collection.Seq;


public interface IBatchExecutionChecker {

    Seq<ExecutionConstraintViolation> check(PBatch batch);

}
