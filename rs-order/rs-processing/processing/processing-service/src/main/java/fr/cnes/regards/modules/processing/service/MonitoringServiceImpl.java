/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

package fr.cnes.regards.modules.processing.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.dto.ExecutionMonitoringDTO;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import fr.cnes.regards.modules.processing.domain.repository.IPExecutionRepository;
import fr.cnes.regards.modules.processing.domain.repository.IPProcessRepository;
import fr.cnes.regards.modules.processing.domain.service.IMonitoringService;
import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * This class is the implementation for the {@link IMonitoringService} interface.
 *
 * @author gandrieu
 */
@Service
public class MonitoringServiceImpl implements IMonitoringService {

    // @formatter:off

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitoringServiceImpl.class);

    private final IPExecutionRepository execRepo;

    private final IPProcessRepository processRepository;

    static Cache<UUID, String> cache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();


    @Autowired
    public MonitoringServiceImpl(IPExecutionRepository execRepo, IPProcessRepository processRepository) {
        this.execRepo = execRepo;
        this.processRepository = processRepository;
    }

    private Mono<Page<PExecution>> getPExecutionsPageForCriteria(String tenant, List<ExecutionStatus> status, String processBid,
            @Nullable String userEmail, OffsetDateTime from, OffsetDateTime to, PageRequest paged) {
        return execRepo.countAllForMonitoringSearch(tenant, processBid, userEmail, status, from, to)
            .flatMap(total ->
                execRepo.findAllForMonitoringSearch(tenant, processBid, userEmail, status, from, to, paged)
                    .collectList()
                    .map(content -> {
                        Page<PExecution> p = new PageImpl<>(content, paged, total);
                        return p;
                    })
            )
            .switchIfEmpty(Mono.just(new PageImpl<>(new ArrayList<>(), paged, 0)))
            .doOnError(t -> LOGGER.error(t.getMessage(), t));
    }

    @Override
    public Mono<Page<ExecutionMonitoringDTO>> getExecutionsPageForCriteria(
            String tenant,
            List<ExecutionStatus> status,
            String processBid,
            @Nullable String userEmail,
            OffsetDateTime from,
            OffsetDateTime to,
            PageRequest paged
    ) {

        return getPExecutionsPageForCriteria(tenant, status, processBid, userEmail, from, to, paged)
            .flatMap(page ->
                Flux.fromIterable(page.getContent())
                    .flatMap(exec -> Option.of(cache.asMap().get(exec.getProcessBusinessId()))
                        .map(name -> Mono.just(new ExecutionMonitoringDTO(exec, name)))
                        .getOrElse(() -> processRepository
                            .findByTenantAndProcessBusinessID(exec.getTenant(), exec.getProcessBusinessId())
                            .doOnNext(process -> cache.put(process.getProcessId(), process.getProcessName()))
                            .map(process -> new ExecutionMonitoringDTO(exec, process.getProcessName()))
                        )
                    )
                    .collectList()
                    .map(dtos -> new PageImpl<>(dtos, page.getPageable(), page.getTotalElements())));
    }

    // @formatter:on
}
