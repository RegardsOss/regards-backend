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
package fr.cnes.regards.modules.processing.domain.repository;

import com.google.common.annotations.VisibleForTesting;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.SearchExecutionEntityParameters;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import io.vavr.collection.Seq;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * This interface defines a repository contract for {@link PExecution} entities.
 *
 * @author gandrieu
 */
public interface IPExecutionRepository {

    Mono<PExecution> create(PExecution execution);

    Mono<PExecution> update(PExecution execution);

    Mono<PExecution> findById(UUID id);

    Flux<PExecution> getTimedOutExecutions();

    Flux<PExecution> findAllForMonitoringSearch(String tenant, SearchExecutionEntityParameters filters, Pageable page);

    Mono<Integer> countAllForMonitoringSearch(String tenant, SearchExecutionEntityParameters filters);

    Mono<Integer> countByProcessBusinessIdAndStatusIn(UUID processBusinessId, Seq<ExecutionStatus> nonFinalStatusList);

    @VisibleForTesting
    Mono<Void> deleteAll();
}
