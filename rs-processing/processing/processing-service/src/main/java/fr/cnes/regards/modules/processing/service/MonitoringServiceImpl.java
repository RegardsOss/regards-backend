/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.modules.processing.domain.SearchExecutionEntityParameters;
import fr.cnes.regards.modules.processing.domain.dto.ExecutionMonitoringDTO;
import fr.cnes.regards.modules.processing.domain.repository.IPExecutionRepository;
import fr.cnes.regards.modules.processing.domain.repository.IPProcessRepository;
import fr.cnes.regards.modules.processing.domain.service.IMonitoringService;
import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * This class is the implementation for the {@link IMonitoringService} interface.
 *
 * @author gandrieu
 */
@Service
public class MonitoringServiceImpl implements IMonitoringService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitoringServiceImpl.class);

    private final static Cache<UUID, String> CACHE = Caffeine.newBuilder()
                                                             .expireAfterWrite(5, TimeUnit.MINUTES)
                                                             .maximumSize(1000)
                                                             .build();

    private final IPExecutionRepository executionRepository;

    private final IPProcessRepository processRepository;

    @Autowired
    public MonitoringServiceImpl(IPExecutionRepository executionRepository, IPProcessRepository processRepository) {
        this.executionRepository = executionRepository;
        this.processRepository = processRepository;
    }

    private Mono<Page<PExecution>> getPExecutionsPageForCriteria(String tenant,
                                                                 SearchExecutionEntityParameters filters,
                                                                 Pageable paged) {
        return executionRepository.countAllForMonitoringSearch(tenant, filters)
                                  .flatMap(total -> executionRepository.findAllForMonitoringSearch(tenant,
                                                                                                   filters,
                                                                                                   paged)
                                                                       .collectList()
                                                                       .map(content -> (Page<PExecution>) new PageImpl<>(
                                                                           content,
                                                                           paged,
                                                                           total)))
                                  .switchIfEmpty(Mono.just(new PageImpl<>(new ArrayList<>(), paged, 0)))
                                  .doOnError(t -> LOGGER.error(t.getMessage(), t));
    }

    @Override
    public Mono<Page<ExecutionMonitoringDTO>> getExecutionsPageForCriteria(String tenant,
                                                                           SearchExecutionEntityParameters filters,
                                                                           Pageable page) {

        return getPExecutionsPageForCriteria(tenant, filters, page).flatMap(p -> Flux.fromIterable(p.getContent())
                                                                                     .flatMap(exec -> Option.of(CACHE.asMap()
                                                                                                                     .get(
                                                                                                                         exec.getProcessBusinessId()))
                                                                                                            .map(name -> Mono.just(
                                                                                                                new ExecutionMonitoringDTO(
                                                                                                                    exec,
                                                                                                                    name)))
                                                                                                            .getOrElse(() -> processRepository.findByTenantAndProcessBusinessID(
                                                                                                                                                  exec.getTenant(),
                                                                                                                                                  exec.getProcessBusinessId())
                                                                                                                                              .doOnNext(
                                                                                                                                                  process -> CACHE.put(
                                                                                                                                                      process.getProcessId(),
                                                                                                                                                      process.getProcessName()))
                                                                                                                                              .map(
                                                                                                                                                  process -> new ExecutionMonitoringDTO(
                                                                                                                                                      exec,
                                                                                                                                                      process.getProcessName()))))
                                                                                     .collectList()
                                                                                     .map(dtos -> new PageImpl<>(dtos,
                                                                                                                 p.getPageable(),
                                                                                                                 p.getTotalElements())));
    }

}
