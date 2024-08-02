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
package fr.cnes.regards.modules.processing.rest;

import fr.cnes.regards.modules.processing.domain.dto.PBatchRequest;
import fr.cnes.regards.modules.processing.domain.dto.PBatchResponse;
import fr.cnes.regards.modules.processing.domain.service.IBatchService;
import fr.cnes.regards.modules.processing.domain.service.IPUserAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.BATCH_PATH;

/**
 * This class defines REST endpoints to deal with batches in reactive application.
 *
 * @author gandrieu
 */
@RestController
@ConditionalOnProperty(name = "spring.main.web-application-type", havingValue = "reactive")
@RequestMapping(path = BATCH_PATH)
public class PBatchReactiveController {

    private final IBatchService batchService;

    private final IPUserAuthService authFactory;

    @Autowired
    public PBatchReactiveController(IBatchService batchService, IPUserAuthService authFactory) {
        this.batchService = batchService;
        this.authFactory = authFactory;
    }

    @PostMapping
    public Mono<PBatchResponse> createBatch(@RequestBody PBatchRequest data) {
        return ReactiveSecurityContextHolder.getContext()
                                            .flatMap(ctx -> batchService.checkAndCreateBatch(authFactory.fromContext(ctx),
                                                                                             data)
                                                                        .map(b -> new PBatchResponse(b.getId(),
                                                                                                     b.getCorrelationId())));
    }

}
