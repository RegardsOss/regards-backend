package fr.cnes.regards.modules.processing.entity.mapping;

import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.PStep;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import fr.cnes.regards.modules.processing.entity.ExecutionEntity;
import fr.cnes.regards.modules.processing.entity.FileParameters;
import fr.cnes.regards.modules.processing.entity.Steps;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;

import static fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus.REGISTERED;

@Component
public class ExecutionMapper implements DomainEntityMapper.Execution {

    private final StepMapper stepMapper;

    @Autowired
    public ExecutionMapper(StepMapper stepMapper) {
        this.stepMapper = stepMapper;
    }

    @Override
    public ExecutionEntity toEntity(PExecution exec) {
        return new ExecutionEntity(
                exec.getId(),
                exec.getBatchId(),
                new FileParameters(exec.getInputFiles().toJavaList()),
                exec.getExpectedDuration().toMillis(),
                toEntity(exec.getSteps()),
                exec.getSteps().lastOption().map(PStep::getStatus).getOrElse(REGISTERED),
                exec.getTenant(),
                exec.getUserName(),
                exec.getProcessBusinessId(),
                exec.getProcessName(),
                exec.getExecutionCorrelationId(),
                exec.getBatchCorrelationId(),
                exec.getCreated(),
                exec.getLastUpdated(),
                exec.getVersion(),
                exec.isPersisted()
        );
    }

    @Override
    public PExecution toDomain(ExecutionEntity entity) {
        return new PExecution(
                entity.getId(),
                entity.getCorrelationId(),
                entity.getBatchId(),
                entity.getBatchCorrelationId(),
                Duration.ofMillis(entity.getTimeoutAfterMillis()),
                List.ofAll(entity.getFileParameters().getValues()),
                toDomain(entity.getSteps()),
                entity.getTenant(),
                entity.getUserEmail(),
                entity.getProcessBusinessId(),
                entity.getProcessName(),
                entity.getCreated(),
                entity.getLastUpdated(),
                entity.getVersion(),
                entity.isPersisted()
        );
    }

    private Steps toEntity(Seq<PStep> domain) {
        return new Steps(domain.map(stepMapper::toEntity).toJavaList());
    }

    private List<PStep> toDomain(Steps entity) {
        return List.ofAll(entity.getValues()).map(stepMapper::toDomain);
    }

}
