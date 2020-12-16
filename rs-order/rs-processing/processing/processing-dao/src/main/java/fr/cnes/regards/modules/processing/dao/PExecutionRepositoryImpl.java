/* Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
*/
package fr.cnes.regards.modules.processing.dao;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import fr.cnes.regards.modules.processing.domain.repository.IPExecutionRepository;
import fr.cnes.regards.modules.processing.entity.ExecutionEntity;
import fr.cnes.regards.modules.processing.entity.mapping.DomainEntityMapper;
import fr.cnes.regards.modules.processing.exceptions.ProcessingException;
import fr.cnes.regards.modules.processing.exceptions.ProcessingExceptionType;
import io.vavr.collection.Seq;
import io.vavr.control.Option;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * This class is a bridge between execution domain entities and database entities.
 *
 * @author gandrieu
 */
@Component
public class PExecutionRepositoryImpl implements IPExecutionRepository {

    // @formatter:off

    private static Cache<UUID, PExecution> cache = Caffeine
        .newBuilder()
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .maximumSize(10000).build();

    private final IExecutionEntityRepository entityExecRepo;

    private final DomainEntityMapper.Execution mapper;

    private final DatabaseClient databaseClient;

    @Autowired
    public PExecutionRepositoryImpl(
            IExecutionEntityRepository entityExecRepo,
            DomainEntityMapper.Execution mapper,
            DatabaseClient databaseClient
    ) {
        this.entityExecRepo = entityExecRepo;
        this.mapper = mapper;
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<PExecution> create(PExecution exec) {
        return entityExecRepo
            .save(mapper.toEntity(exec))
            .map(ExecutionEntity::persisted)
            .map(mapper::toDomain)
            .doOnNext(e -> cache.put(e.getId(), e));
    }

    @Override
    public Mono<Integer> countByProcessBusinessIdAndStatusIn(UUID processBusinessId,
            Seq<ExecutionStatus> nonFinalStatusList) {
        return entityExecRepo.countByProcessBusinessIdAndCurrentStatusIn(
            processBusinessId,
            nonFinalStatusList.toJavaList()
        );
    }

    @Override
    public Mono<PExecution> update(PExecution exec) {
        return entityExecRepo
            .save(mapper.toEntity(exec))
            .map(mapper::toDomain)
            .doOnNext(e -> cache.put(e.getId(), e));
    }

    @Override
    public Mono<PExecution> findById(UUID id) {
        return Option.of(cache.getIfPresent(id))
            .map(Mono::just)
            .getOrElse(() -> entityExecRepo
                .findById(id)
                .map(mapper::toDomain)
                .doOnNext(e -> cache.put(e.getId(), e))
            )
            .switchIfEmpty(Mono.defer(() -> Mono.error(new ExecutionNotFoundException(id))));
    }

    @Override
    public Flux<PExecution> getTimedOutExecutions() {
        return entityExecRepo.getTimedOutExecutions().map(mapper::toDomain);
    }

    @Override
    public Flux<PExecution> findAllForMonitoringSearch(
            String tenant,
            String processBid,
            String userEmail,
            List<ExecutionStatus> status,
            OffsetDateTime from,
            OffsetDateTime to,
            Pageable page
    ) {
        DatabaseClient.GenericExecuteSpec execute = databaseClient.execute(
                " SELECT E.* " +
                " FROM t_execution AS E " +
                " WHERE (:ignoreTenant OR E.tenant = :tenant) " +
                "   AND (:ignoreProcessBid OR E.process_business_id = :processBid) " +
                "   AND (:ignoreUserEmail OR E.user_email = :userEmail) " +
                "   AND  E.current_status IN (:status) " +
                "   AND  E.last_updated >= :lastUpdatedFrom " +
                "   AND  E.last_updated <= :lastUpdatedTo " +
                " LIMIT :limit OFFSET :offset"
        );

        execute = execute.bind("ignoreTenant", tenant == null);
        execute = tenant == null ? execute.bindNull("tenant", String.class) : execute.bind("tenant", tenant);
        execute = execute.bind("ignoreProcessBid", processBid == null);
        execute = processBid == null ? execute.bindNull("processBid", UUID.class) : execute.bind("processBid", UUID.fromString(processBid));
        execute = execute.bind("ignoreUserEmail", userEmail == null);
        execute = userEmail == null ? execute.bindNull("userEmail", String.class) : execute.bind("userEmail", userEmail);
        execute = execute.bind("status", status);
        execute = execute.bind("lastUpdatedFrom", from);
        execute = execute.bind("lastUpdatedTo", to);
        execute = execute.bind("limit", page.getPageSize());
        execute = execute.bind("offset", page.getOffset());

        return execute
                .as(ExecutionEntity.class)
                .fetch()
                .all()
                .map(mapper::toDomain)
                .doOnNext(exec -> cache.put(exec.getId(), exec));
    }

    @Override
    public Mono<Integer> countAllForMonitoringSearch(
            String tenant,
            String processBid,
            String userEmail,
            List<ExecutionStatus> status,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        DatabaseClient.GenericExecuteSpec execute = databaseClient.execute(
                " SELECT COUNT(*) " +
                " FROM t_execution AS E " +
                " WHERE (:ignoreTenant OR E.tenant = :tenant) " +
                "   AND (:ignoreProcessBid OR E.process_business_id = :processBid) " +
                "   AND (:ignoreUserEmail OR E.user_email = :userEmail) " +
                "   AND  E.current_status IN (:status) " +
                "   AND  E.last_updated >= :lastUpdatedFrom " +
                "   AND  E.last_updated <= :lastUpdatedTo "
        );

        execute = execute.bind("ignoreTenant", tenant == null);
        execute = tenant == null ? execute.bindNull("tenant", String.class) : execute.bind("tenant", tenant);
        execute = execute.bind("ignoreProcessBid", processBid == null);
        execute = processBid == null ? execute.bindNull("processBid", UUID.class) : execute.bind("processBid", UUID.fromString(processBid));
        execute = execute.bind("ignoreUserEmail", userEmail == null);
        execute = userEmail == null ? execute.bindNull("userEmail", String.class) : execute.bind("userEmail", userEmail);
        execute = execute.bind("status", status);
        execute = execute.bind("lastUpdatedFrom", from);
        execute = execute.bind("lastUpdatedTo", to);

        return execute
                .as(Integer.class)
                .fetch()
                .one();
    }

    public static final class ExecutionNotFoundException extends ProcessingException {

        private static final long serialVersionUID = 1L;

        public ExecutionNotFoundException(UUID execId) {
            super(ProcessingExceptionType.EXECUTION_NOT_FOUND_EXCEPTION,
                  String.format("Execution uuid not found: %s", execId));
        }

        @Override
        public String getMessage() {
            return desc;
        }
    }

    // @formatter:on
}
