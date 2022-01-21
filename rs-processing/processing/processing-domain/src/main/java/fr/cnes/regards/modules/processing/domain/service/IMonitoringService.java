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

package fr.cnes.regards.modules.processing.domain.service;

import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.dto.ExecutionMonitoringDTO;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * This interface defines a service contract for monitoring {@link PExecution} entities.
 *
 * @author gandrieu
 */
public interface IMonitoringService {

    Mono<Page<ExecutionMonitoringDTO>> getExecutionsPageForCriteria(
        String tenant,
        List<ExecutionStatus> status,
        @Nullable String processBid,
        @Nullable String userEmail,
        OffsetDateTime from,
        OffsetDateTime to,
        PageRequest paged
    );

}
