package fr.cnes.regards.modules.processing.dao;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.entities.mapping.DomainEntityMapper;
import fr.cnes.regards.modules.processing.repository.IPExecutionRepository;
import io.vavr.control.Option;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class PExecutionRepositoryImpl implements IPExecutionRepository {

    static Cache<UUID, PExecution> cache = Caffeine.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();

    private final IExecutionStepEntityRepository stepRepo;

    private final IExecutionEntityRepository entityExecRepo;

    private final DomainEntityMapper mapper;

    @Autowired
    public PExecutionRepositoryImpl(IExecutionStepEntityRepository stepRepo, IExecutionEntityRepository entityExecRepo,
            DomainEntityMapper mapper) {
        this.stepRepo = stepRepo;
        this.entityExecRepo = entityExecRepo;
        this.mapper = mapper;
    }

    @Override public Mono<PExecution> save(PExecution exec) {
        return entityExecRepo
            .save(mapper.toEntity(exec))
            .flatMap(mapper::toDomain)
            .doOnNext(e -> cache.put(e.getId(), e));
    }

    @Override public Mono<PExecution> findById(UUID id) {
        return Option.of(cache.getIfPresent(id))
            .map(Mono::just)
            .getOrElse(() -> entityExecRepo.findById(id).flatMap(mapper::toDomain));
    }

    @Override public Flux<PExecution> getTimedOutExecutions() {
        return entityExecRepo.getTimedOutExecutions().flatMap(mapper::toDomain);
    }

}
