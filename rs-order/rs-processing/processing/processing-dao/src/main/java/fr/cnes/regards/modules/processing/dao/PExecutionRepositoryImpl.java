package fr.cnes.regards.modules.processing.dao;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import fr.cnes.regards.modules.processing.entity.ExecutionEntity;
import fr.cnes.regards.modules.processing.entity.mapping.DomainEntityMapper;
import fr.cnes.regards.modules.processing.repository.IPExecutionRepository;
import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class PExecutionRepositoryImpl implements IPExecutionRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(PExecutionRepositoryImpl.class);

    private static Cache<UUID, PExecution> cache = Caffeine.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();

    private final IExecutionEntityRepository entityExecRepo;

    private final DomainEntityMapper mapper;

    @Autowired
    public PExecutionRepositoryImpl(IExecutionEntityRepository entityExecRepo,
            DomainEntityMapper mapper) {
        this.entityExecRepo = entityExecRepo;
        this.mapper = mapper;
    }

    @Override public Mono<PExecution> create(PExecution exec) {
        return entityExecRepo
            .save(mapper.toEntity(exec.withAuditDates()))
            .map(ExecutionEntity::persisted)
            .flatMap(mapper::toDomain)
            .doOnNext(e -> cache.put(e.getId(), e));
    }


    @Override public Mono<PExecution> update(PExecution exec) {
        return entityExecRepo
                .save(mapper.toEntity(exec))
                .flatMap(mapper::toDomain)
                .doOnNext(e -> cache.put(e.getId(), e));
    }

    @Override public Mono<PExecution> findById(UUID id) {
        return Option.of(cache.getIfPresent(id))
            .map(Mono::just)
            .getOrElse(() -> entityExecRepo.findById(id)
                .flatMap(mapper::toDomain)
                .doOnNext(e -> cache.put(e.getId(), e)));
    }

    @Override public Flux<PExecution> getTimedOutExecutions() {
        return entityExecRepo.getTimedOutExecutions().flatMap(mapper::toDomain);
    }

    @Override
    public Flux<PExecution> findByTenantAndCurrentStatusInAndLastUpdatedAfterAndLastUpdatedBefore(
            String tenant,
            List<ExecutionStatus> status,
            OffsetDateTime from,
            OffsetDateTime to,
            Pageable page
    ) {
        return entityExecRepo.findByTenantAndCurrentStatusInAndLastUpdatedAfterAndLastUpdatedBefore(
                tenant, status, from, to, page
        ).flatMap(mapper::toDomain);
    }

    @Override public Flux<PExecution> findByTenantAndUserNameAndCurrentStatusInAndLastUpdatedAfterAndLastUpdatedBefore(
            String tenant,
            String userEmail,
            List<ExecutionStatus> status,
            OffsetDateTime from,
            OffsetDateTime to,
            Pageable page
    ) {
        return entityExecRepo.findByTenantAndUserNameAndCurrentStatusInAndLastUpdatedAfterAndLastUpdatedBefore(
                tenant, userEmail, status, from, to, page
        ).flatMap(mapper::toDomain);
    }

}
