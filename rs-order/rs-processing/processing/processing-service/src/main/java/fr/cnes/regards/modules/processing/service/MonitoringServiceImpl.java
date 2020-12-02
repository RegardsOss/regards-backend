/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import fr.cnes.regards.modules.processing.domain.repository.IPExecutionRepository;
import fr.cnes.regards.modules.processing.domain.service.IExecutionService;
import fr.cnes.regards.modules.processing.domain.service.IMonitoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
/**
 * This class is the implementation for the {@link IMonitoringService} interface.
 *
 * @author gandrieu
 */
@Service
public class MonitoringServiceImpl implements IMonitoringService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitoringServiceImpl.class);

    private final IPExecutionRepository execRepo;

    @Autowired
    public MonitoringServiceImpl(IPExecutionRepository execRepo) {
        this.execRepo = execRepo;
    }

    public Mono<Page<PExecution>> getExecutionsPageForCriteria(
            String tenant,
            List<ExecutionStatus> status,
            @Nullable String userEmail,
            OffsetDateTime from,
            OffsetDateTime to,
            PageRequest paged
    ) {
        if (userEmail == null) {
            return execRepo
                    .countByTenantAndCurrentStatusInAndLastUpdatedAfterAndLastUpdatedBefore(tenant, status, from, to)
                    .flatMap(total -> execRepo
                            .findByTenantAndCurrentStatusInAndLastUpdatedAfterAndLastUpdatedBefore(tenant, status, from, to, paged)
                            .collectList()
                            .map(content -> {
                                Page<PExecution> p = new PageImpl<>(content, paged, total);
                                return p;
                            })
                    )
                    .switchIfEmpty(Mono.just(new PageImpl<>(new ArrayList<>(), paged, 0)))
                    .doOnError(t -> LOGGER.error(t.getMessage(), t));
        } else {
            return execRepo
                    .countByTenantAndUserEmailAndCurrentStatusInAndLastUpdatedAfterAndLastUpdatedBefore(tenant, userEmail, status, from, to)
                    .flatMap(total -> execRepo
                            .findByTenantAndUserEmailAndCurrentStatusInAndLastUpdatedAfterAndLastUpdatedBefore(tenant, userEmail, status, from, to, paged)
                            .collectList()
                            .map(content -> {
                                Page<PExecution> p = new PageImpl<>(content, paged, total);
                                return p;
                            })
                    )
                    .switchIfEmpty(Mono.just(new PageImpl<>(new ArrayList<>(), paged, 0)))
                    .doOnError(t -> LOGGER.error(t.getMessage(), t));
        }
    }


}
