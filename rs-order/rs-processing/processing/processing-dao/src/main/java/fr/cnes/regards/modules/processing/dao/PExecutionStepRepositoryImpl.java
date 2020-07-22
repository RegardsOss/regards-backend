package fr.cnes.regards.modules.processing.dao;

import fr.cnes.regards.modules.processing.domain.PExecutionStep;
import fr.cnes.regards.modules.processing.domain.PExecutionStepSequence;
import fr.cnes.regards.modules.processing.entities.mapping.DomainEntityMapper;
import fr.cnes.regards.modules.processing.repository.IPExecutionStepRepository;
import io.vavr.collection.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class PExecutionStepRepositoryImpl implements IPExecutionStepRepository {

    private final IExecutionStepEntityRepository entityRepo;
    private final DomainEntityMapper mapper;

    @Autowired
    public PExecutionStepRepositoryImpl(IExecutionStepEntityRepository entityRepo, DomainEntityMapper mapper) {
        this.entityRepo = entityRepo;
        this.mapper = mapper;
    }

    @Override public Mono<PExecutionStep> save(PExecutionStep step) {
        return entityRepo
            .save(mapper.toEntity(step))
            .flatMap(mapper::toDomain);
    }

    @Override public Mono<PExecutionStepSequence> findAllForExecution(UUID execId) {
        return entityRepo
            .findAllByExecutionId(execId)
            .flatMap(mapper::toDomain)
            .collect(List.collector())
            .map(PExecutionStepSequence::new);
    }
}
