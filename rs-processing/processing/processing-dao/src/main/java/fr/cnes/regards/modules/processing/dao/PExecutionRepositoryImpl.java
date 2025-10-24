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
        return entityExecRepo.save(mapper.toEntity(exec)).map(mapper::toDomain).doOnNext(e -> cache.put(e.getId(), e));
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
        return entityExecRepo.save(mapper.toEntity(exec)).map(mapper::toDomain).doOnNext(e -> cache.put(e.getId(), e));
    }

    @Override
    public Mono<PExecution> findById(UUID id) {
        return Option.of(cache.getIfPresent(id))
                     .map(Mono::just)
                     .getOrElse(() -> entityExecRepo.findById(id)
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

        // build the "order by" sql expression from the given Pageable.
        String orderBy = "";
        if (!page.getSort().isEmpty()) {
            StringJoiner sj = new StringJoiner(",", "ORDER BY ", "");
            for (Order o : page.getSort().toList()) {
                sj.add(o.getProperty() + " " + o.getDirection());
            }
            orderBy = sj.toString();
        }

        // Build the sql expression to select all the executions of the given tenant and matching the given filters
        // - the tenant is either ignored or equal to the given one.
        // - processBid is either ignored or equal to the given one of the filters
        // - current status is in the one given by the filters
        // - the last updated is between the creation date before and after of the filters
        // - user email from the restriction of the filters
        // include the sorting "order by" and paging "limit" and "offset"
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

        // bind the parameters
        // - "ignoreTenant", "tenant" ,
        // - "ignoreProcessBid", "processBid",
        // - "status",
        // - "lastUpdatedFrom" "lastUpdatedTo"
        execute = bindParametersInWhere(execute, tenant, filters);

        // bind the page parameters "limit" and "offset" from the given Pageble
        execute = execute.bind("limit", page.getPageSize());
        execute = execute.bind("offset", page.getOffset());

        // Need to manually call persisted() on the ExecutionEntity because ExecutionEntityCallback is not
        // automically called when directly using the MappingR2dbcConverter. Check out ExecutionEntityCallback to
        // see why it's important to call persisted().
        return execute.map((row, metadata) -> converter.read(ExecutionEntity.class, row, metadata).persisted())
                      .all()
                      .map(mapper::toDomain)
                      .doOnNext(exec -> cache.put(exec.getId(), exec));
    }

    @Override
    public Mono<Integer> countAllForMonitoringSearch(String tenant, SearchExecutionEntityParameters filters) {
        // build the sql expression to count all the executions of the given tenant and matching the given filters
        // - the tenant is either ignored or equal to the given one.
        // - processBid is either ignored or equal to the given one of the filters
        // - current status is in the one given by the filters
        // - the last updated is between the creation date before and after of the filters
        // - user email from the restriction of the filters
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
        // bind the GenericExecuteSpec with the values of the filters.
        execute = bindParametersInWhere(execute, tenant, filters);
        // execute the sql query for getting the count.
        return execute.map((row, metadata) -> converter.read(Integer.class, row, metadata)).one();
    }

    /**
     * Bind the parameters "ignoreTenant", "tenant" from the given tenant.<br/>
     * Bind the parameters "ignoreProcessBid", "processBid", "status", "lastUpdatedFrom" and
     * "lastUpdatedTo" from the given filters.<br/>
     * Parameters are bound into the given {@link DatabaseClient.GenericExecuteSpec}.
     *
     * @param execute the GenericExecuteSpec to be bound with parameters
     * @param tenant  the tenant filter
     * @param filters the other filters for the other parameters.
     * @return {@link DatabaseClient.GenericExecuteSpec} with added bound parameters.
     */
    private DatabaseClient.GenericExecuteSpec bindParametersInWhere(DatabaseClient.GenericExecuteSpec execute,
                                                                    String tenant,
                                                                    SearchExecutionEntityParameters filters) {

        // bind "ignoreTenant" and "tenant" from given tenant
        execute = execute.bind("ignoreTenant", tenant == null);
        execute = tenant == null ? execute.bindNull(TENANT_COLUMN, String.class) : execute.bind(TENANT_COLUMN, tenant);

        // bind "ignoreProcessBid" and "processBid" from filters.getProcessBusinessId()
        execute = execute.bind("ignoreProcessBid", filters.getProcessBusinessId() == null);
        execute = filters.getProcessBusinessId() == null ?
            execute.bindNull(PROCESS_BID_COLUMN, UUID.class) :
            execute.bind(PROCESS_BID_COLUMN, UUID.fromString(filters.getProcessBusinessId()));

        // bind "status" from filters.getStatus()
        execute = (filters.getStatus() == null || filters.getStatus().getValues().isEmpty()) ?
            execute.bind("status", Stream.of(ExecutionStatus.values()).map(Enum::name).toList()) :
            execute.bind("status", filters.getStatus().getValues().stream().map(Enum::toString).toList());

        // bind "lastUpdatedFrom" from filters.getCreationDate().getAfter()
        execute = filters.getCreationDate().getAfter() == null ?
            // default OffsetDateTime is 2000-01-01
            execute.bind("lastUpdatedFrom", OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)) :
            execute.bind("lastUpdatedFrom", filters.getCreationDate().getAfter());

        // bind "lastUpdatedTo" from filters.getCreationDate().getBefore()
        execute = filters.getCreationDate().getBefore() == null ?
            // default OffsetDateTime is 2100-01-01
            execute.bind("lastUpdatedTo", OffsetDateTime.of(2100, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)) :
            execute.bind("lastUpdatedTo", filters.getCreationDate().getBefore());

        return execute;
    }

    /**
     * Build the user email expression from the restriction found in the given filters.
     *
     * @param filters the filtres providing the user email restrictions.
     * @return a String representing the expression on the user email or "true" if no restriction on the user email.
     * @see SearchExecutionEntityParameters#getUserEmail()
     */
    private String getUserExpression(SearchExecutionEntityParameters filters) {
        String userExpression;
        // any user email filter?
        boolean emptyUsers = filters.getUserEmail() == null || filters.getUserEmail().getValues().isEmpty();
        // no user email filter?
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
