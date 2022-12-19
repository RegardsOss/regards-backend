/* Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.processing.domain.SearchExecutionEntityParameters;
import fr.cnes.regards.modules.processing.domain.dto.ExecutionMonitoringDTO;
import fr.cnes.regards.modules.processing.domain.service.IMonitoringService;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.MONITORING_EXECUTIONS_PATH;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.Param.TENANT_PARAM;

/**
 * This class defines REST endpoints to deal with monitoring in servlet application.
 *
 * @author gandrieu
 */
@RestController
@ConditionalOnProperty(name = "spring.main.web-application-type", havingValue = "servlet", matchIfMissing = true)
@RequestMapping(path = MONITORING_EXECUTIONS_PATH)
public class PMonitoringController implements IResourceController<ExecutionMonitoringDTO> {

    private final IMonitoringService monitoringService;

    @Autowired
    public PMonitoringController(IMonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @Autowired
    private PagedResourcesAssembler<ExecutionMonitoringDTO> assembler;
    
    @PostMapping
    @ResourceAccess(
        description = "List executions filtered by tenant/user/date/status depending on the given parameters",
        role = DefaultRole.ADMIN)
    public HttpEntity<PagedModel<EntityModel<ExecutionMonitoringDTO>>> executions(
        @RequestParam(name = TENANT_PARAM, required = true) String tenant,
        @RequestBody SearchExecutionEntityParameters filters,
        @Parameter(description = "Sorting and page configuration")
        @PageableDefault(sort = "created", direction = Sort.Direction.DESC) Pageable pageable) {

        RequestAttributes ctxRequestAttributes = RequestContextHolder.getRequestAttributes();

        return monitoringService.getExecutionsPageForCriteria(tenant, filters, pageable).map(p -> {
            // Reset context request attributes with requested one.
            RequestContextHolder.setRequestAttributes(ctxRequestAttributes);
            return new ResponseEntity<>(this.toPagedResources(p, assembler), HttpStatus.OK);
        }).block();
    }

    @Override
    public EntityModel<ExecutionMonitoringDTO> toResource(ExecutionMonitoringDTO element, Object... extras) {
        return EntityModel.of(element);
    }
}
