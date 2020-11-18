package fr.cnes.regards.modules.processing.entity.mapping;

import fr.cnes.regards.modules.processing.domain.PStep;
import fr.cnes.regards.modules.processing.entity.StepEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Component
public class StepMapper implements DomainEntityMapper.Step {

    public StepEntity toEntity(PStep step) {
        return new StepEntity(
                step.getStatus(),
                step.getTime().toEpochSecond() * 1000L,
                step.getMessage()
        );
    }

    public PStep toDomain(StepEntity entity) {
        return PStep.from(
                entity.getStatus(),
                OffsetDateTime.ofInstant(Instant.ofEpochMilli(entity.getEpochTs()), ZoneId.of("UTC")),
                entity.getMessage()
        );
    }
}
