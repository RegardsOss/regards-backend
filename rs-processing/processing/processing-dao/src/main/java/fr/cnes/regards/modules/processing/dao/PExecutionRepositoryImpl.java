/* Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.framework.jpa.restriction.ValuesRestriction;
import fr.cnes.regards.framework.jpa.restriction.ValuesRestrictionMatchMode;
import fr.cnes.regards.framework.jpa.restriction.ValuesRestrictionMode;
import fr.cnes.regards.framework.jpa.utils.AbstractSpecificationsBuilder;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.SearchExecutionEntityParameters;
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
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.r2dbc.convert.MappingR2dbcConverter;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * This class is a bridge between execution domain entities and database entities.
 *
 * @author gandrieu
 */
@Component
public class PExecutionRepositoryImpl implements IPExecutionRepository {

    private static final String TENANT_COLUMN = "tenant";

    private static final String PROCESS_BID_COLUMN = "processBid";

    private static final String USER_EMAIL_COLUMN = "userEmail";

    private static Cache<UUID, PExecution> cache = Caffeine.newBuilder()
                                                           .expireAfterAccess(30, TimeUnit.MINUTES)
                                                           .maximumSize(10000)
                                                           .build();

    private final IExecutionEntityRepository entityExecRepo;

    private final DomainEntityMapper.Execution mapper;

    private final DatabaseClient databaseClient;

    private final MappingR2dbcConverter converter;

    @Autowired
    public PExecutionRepositoryImpl(IExecutionEntityRepository entityExecRepo,
                                    DomainEntityMapper.Execution mapper,
                                    DatabaseClient databaseClient,
                                    MappingR2dbcConverter converter) {
        this.entityExecRepo = entityExecRepo;
        this.mapper = mapper;
        this.databaseClient = databaseClient;
        this.converter = converter;
    }

    @Override
    public Mono<PExecution> create(PExecution exec) {
        return entityExecRepo.save(mapper.toEntity(exec))
                             .map(ExecutionEntity::persisted)
                             .map(mapper::toDomain)
                             .doOnNext(e -> cache.put(e.getId(), e));
    }

    @Override
    public Mono<Integer> countByProcessBusinessIdAndStatusIn(UUID processBusinessId,
                                                             Seq<ExecutionStatus> nonFinalStatusList) {
        return entityExecRepo.countByProcessBusinessIdAndCurrentStatusIn(processBusinessId,
                                                                         nonFinalStatusList.toJavaList());
    }

    @Override
    public Mono<Void> deleteAll() {
        return entityExecRepo.deleteAll().doOnTerminate(() -> {
            cache.invalidateAll();
            cache.cleanUp();
        });
    }

    @Override
    public Mono<PExecution> update(PExecution exec) {
        return entityExecRepo.save(mapper.toEntity(exec))
                             .map(ExecutionEntity::persisted)
                             .map(mapper::toDomain)
                             .doOnNext(e -> cache.put(e.getId(), e));
    }

    @Override
    public Mono<PExecution> findById(UUID id) {
        return Option.of(cache.getIfPresent(id))
                     .map(Mono::just)
                     .getOrElse(() -> entityExecRepo.findById(id)
                                                    .map(ExecutionEntity::persisted)
                                                    .map(mapper::toDomain)
                                                    .doOnNext(e -> cache.put(e.getId(), e)))
                     .switchIfEmpty(Mono.defer(() -> Mono.error(new ExecutionNotFoundException(id))));
    }

    @Override
    public Flux<PExecution> getTimedOutExecutions() {
        return entityExecRepo.getTimedOutExecutions().map(mapper::toDomain);
    }

    @Override
    public Flux<PExecution> findAllForMonitoringSearch(String tenant,
                                                       SearchExecutionEntityParameters filters,
                                                       Pageable page) {
        String orderBy = "";
        if ((page.getSort() != null) && !page.getSort().isEmpty()) {
            StringJoiner sj = new StringJoiner(",", "ORDER BY ", "");
            int count = 0;
            for (Order o : page.getSort().toList()) {
                count++;
                sj.add(o.getProperty() + " " + o.getDirection());
            }
            orderBy = sj.toString();
        }
        DatabaseClient.GenericExecuteSpec execute = databaseClient.sql(String.format(""" 
                                                                                         SELECT E.*
                                                                                         FROM t_execution AS E
                                                                                         WHERE (:ignoreTenant OR E.tenant = :tenant)
                                                                                         AND (:ignoreProcessBid OR E.process_business_id = :processBid)
                                                                                         AND %s
                                                                                         AND  E.current_status IN (:status)
                                                                                         AND  E.last_updated >= :lastUpdatedFrom
                                                                                         AND  E.last_updated <= :lastUpdatedTo
                                                                                         %s
                                                                                         LIMIT :limit OFFSET :offset;""",
                                                                                     getUserExpression(filters),
                                                                                     orderBy));

        execute = bindParametersInWhere(execute, tenant, filters);

        execute = execute.bind("limit", page.getPageSize());
        execute = execute.bind("offset", page.getOffset());

        return execute.map((row, metadata) -> converter.read(ExecutionEntity.class, row, metadata))
                      .all()
                      .map(ExecutionEntity::persisted)
                      .map(mapper::toDomain)
                      .doOnNext(exec -> cache.put(exec.getId(), exec));
    }

    @Override
    public Mono<Integer> countAllForMonitoringSearch(String tenant, SearchExecutionEntityParameters filters) {
        String sqlExpression = String.format("""
                                                 SELECT COUNT(*)
                                                 FROM t_execution AS E
                                                 WHERE (:ignoreTenant OR E.tenant = :tenant)
                                                 AND (:ignoreProcessBid OR E.process_business_id = :processBid)
                                                 AND %s
                                                 AND  E.current_status IN (:status)
                                                 AND  E.last_updated >= :lastUpdatedFrom
                                                 AND  E.last_updated <= :lastUpdatedTo;""", getUserExpression(filters));

        DatabaseClient.GenericExecuteSpec execute = databaseClient.sql(sqlExpression);
        execute = bindParametersInWhere(execute, tenant, filters);
        return execute.map((row, metadata) -> converter.read(Integer.class, row, metadata)).one();
    }

    private DatabaseClient.GenericExecuteSpec bindParametersInWhere(DatabaseClient.GenericExecuteSpec execute,
                                                                    String tenant,
                                                                    SearchExecutionEntityParameters filters) {

        execute = execute.bind("ignoreTenant", tenant == null);
        execute = tenant == null ? execute.bindNull(TENANT_COLUMN, String.class) : execute.bind(TENANT_COLUMN, tenant);

        execute = execute.bind("ignoreProcessBid", filters.getProcessBusinessId() == null);
        execute = filters.getProcessBusinessId() == null ?
            execute.bindNull(PROCESS_BID_COLUMN, UUID.class) :
            execute.bind(PROCESS_BID_COLUMN, UUID.fromString(filters.getProcessBusinessId()));

        execute = (filters.getStatus() == null || filters.getStatus().getValues().isEmpty()) ?
            execute.bind("status", Stream.of(ExecutionStatus.values()).map(Enum::name).toList()) :
            execute.bind("status", filters.getStatus().getValues().stream().map(Enum::toString).toList());

        execute = filters.getCreationDate().getAfter() == null ?
            execute.bind("lastUpdatedFrom", OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)) :
            execute.bind("lastUpdatedFrom", filters.getCreationDate().getAfter());

        execute = filters.getCreationDate().getBefore() == null ?
            execute.bind("lastUpdatedTo", OffsetDateTime.of(2100, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)) :
            execute.bind("lastUpdatedTo", filters.getCreationDate().getBefore());

        return execute;
    }

    private String getUserExpression(SearchExecutionEntityParameters filters) {
        String userExpression;
        boolean emptyUsers = filters.getUserEmail() == null || filters.getUserEmail().getValues().isEmpty();
        if (emptyUsers) {
            userExpression = "true";
        } else {
            userExpression = getValueRestrictionExpression(filters.getUserEmail(), "E.user_email");
        }
        return userExpression;
    }

    private static String getValueRestrictionExpression(ValuesRestriction<String> restriction, String paramName) {
        List<String> exprBuilder = new ArrayList<>();
        String column = restriction.isIgnoreCase() ? "LOWER(" + paramName + ")" : paramName;
        String operator;
        if (restriction.getMatchMode() == ValuesRestrictionMatchMode.STRICT) {
            if (restriction.getMode().equals(ValuesRestrictionMode.INCLUDE)) {
                operator = " = ";
            } else {
                operator = " != ";
            }
        } else {
            if (restriction.getMode().equals(ValuesRestrictionMode.INCLUDE)) {
                operator = " LIKE ";
            } else {
                operator = " NOT LIKE ";
            }
        }
        for (String value : restriction.getValues()) {
            if (restriction.getMatchMode() == ValuesRestrictionMatchMode.STRICT) {
                exprBuilder.add(column + operator + "'" + value + "'");
            } else {
                String likeExpr = AbstractSpecificationsBuilder.getLikeStringExpression(restriction.getMatchMode(),
                                                                                        value,
                                                                                        restriction.isIgnoreCase());
                exprBuilder.add(column + operator + "'" + likeExpr + "'");
            }
        }
        return String.join(" OR ", exprBuilder);
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

}
