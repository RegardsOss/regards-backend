package fr.cnes.regards.modules.processing.plugins;

import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.constraints.ConstraintChecker;

public interface IProcessCheckerDefinition {

    default ConstraintChecker<PBatch> batchChecker() {
        return ConstraintChecker.noViolation();
    }

    default ConstraintChecker<PExecution> executionChecker() {
        return ConstraintChecker.noViolation();
    }

}
