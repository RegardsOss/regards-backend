package fr.cnes.regards.modules.processing.dao;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import fr.cnes.regards.modules.processing.entity.ExecutionEntity;
import fr.cnes.regards.modules.processing.entity.mapping.DomainEntityMapper;
import fr.cnes.regards.modules.processing.domain.repository.IPExecutionRepository;
import fr.cnes.regards.modules.processing.exceptions.ProcessingException;
import fr.cnes.regards.modules.processing.exceptions.ProcessingExceptionType;
import io.vavr.collection.Seq;
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

    private final DomainEntityMapper.Execution mapper;

    @Autowired
    public PExecutionRepositoryImpl(
            IExecutionEntityRepository entityExecRepo,
            DomainEntityMapper.Execution mapper
    ) {
        this.entityExecRepo = entityExecRepo;
        this.mapper = mapper;
    }

    @Override public Mono<PExecution> create(PExecution exec) {
        return entityExecRepo
            .save(mapper.toEntity(exec))
            .map(ExecutionEntity::persisted)
            .map(mapper::toDomain)
            .doOnNext(e -> cache.put(e.getId(), e));
    }

    @Override public Flux<PExecution> findByProcessBusinessIdAndStatusIn(
            UUID processBusinessId,
            Seq<ExecutionStatus> nonFinalStatusList
    ) {
        return entityExecRepo.findByProcessBusinessIdAndCurrentStatusIn(processBusinessId, nonFinalStatusList.toJavaList())
                .map(mapper::toDomain);
    }

    @Override public Mono<PExecution> update(PExecution exec) {
        return entityExecRepo
                .save(mapper.toEntity(exec))
                .map(mapper::toDomain)
                .doOnNext(e -> cache.put(e.getId(), e));
    }

    @Override public Mono<PExecution> findById(UUID id) {
        return Option.of(cache.getIfPresent(id))
            .map(Mono::just)
            .getOrElse(() -> entityExecRepo.findById(id)
                .map(mapper::toDomain)
                .doOnNext(e -> cache.put(e.getId(), e)))
            .switchIfEmpty(Mono.defer(() -> Mono.error(new ExecutionNotFoundException(id))));
    }

    @Override public Flux<PExecution> getTimedOutExecutions() {
        return entityExecRepo.getTimedOutExecutions().map(mapper::toDomain);
    }

    @Override public Flux<PExecution> findByTenantAndCurrentStatusInAndLastUpdatedAfterAndLastUpdatedBefore(
            String tenant,
            List<ExecutionStatus> status,
            OffsetDateTime from,
            OffsetDateTime to,
            Pageable page
    ) {
        return entityExecRepo.findByTenantAndCurrentStatusInAndLastUpdatedAfterAndLastUpdatedBefore(
                tenant, status, from, to, page
        ).map(mapper::toDomain);
    }

    @Override public Flux<PExecution> findByTenantAndUserEmailAndCurrentStatusInAndLastUpdatedAfterAndLastUpdatedBefore(
            String tenant,
            String userEmail,
            List<ExecutionStatus> status,
            OffsetDateTime from,
            OffsetDateTime to,
            Pageable page
    ) {
        return entityExecRepo.findByTenantAndUserEmailAndCurrentStatusInAndLastUpdatedAfterAndLastUpdatedBefore(
                tenant, userEmail, status, from, to, page
        ).map(mapper::toDomain);
    }


    public static final class ExecutionNotFoundException extends ProcessingException {
        public ExecutionNotFoundException(UUID execId) {
            super(ProcessingExceptionType.EXECUTION_NOT_FOUND_EXCEPTION, String.format("Execution uuid not found: %s", execId));
        }
        @Override public String getMessage() {
            return desc;
        }
    }

}
