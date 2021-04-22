/* Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.modules.processing.domain.dto.PProcessDTO;
import fr.cnes.regards.modules.processing.domain.service.IProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.PROCESS_PATH;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.Param.PROCESS_BUSINESS_ID_PARAM;

/**
 * This class defines REST endpoints to deal with processes in reactive application.
 *
 * @author gandrieu
 */
@RestController
@ConditionalOnProperty(name = "spring.main.web-application-type", havingValue = "reactive")
@RequestMapping(path = PROCESS_PATH)
public class PProcessReactiveController {

    @Autowired
    private IProcessService processService;

    @GetMapping
    public Flux<PProcessDTO> findAll() {
        return ReactiveSecurityContextHolder.getContext().flatMapMany(ctx -> {
            JWTAuthentication authentication = (JWTAuthentication) ctx.getAuthentication();
            String tenant = authentication.getTenant();
            return processService.findByTenant(tenant);
        });
    }

    @GetMapping(path = "/{name}")
    public Mono<PProcessDTO> findByUuid(@PathVariable(PROCESS_BUSINESS_ID_PARAM) UUID processId) {
        return ReactiveSecurityContextHolder.getContext().flatMap(ctx -> {
            JWTAuthentication authentication = (JWTAuthentication) ctx.getAuthentication();
            String tenant = authentication.getTenant();
            return processService.findByTenant(tenant).filter(p -> p.getProcessId().equals(processId)).next();
        });
    }

}
