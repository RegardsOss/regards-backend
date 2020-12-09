/* Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.modules.processing.domain.dto.ExecutionMonitoringDTO;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import fr.cnes.regards.modules.processing.domain.service.IMonitoringService;
import fr.cnes.regards.modules.processing.utils.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.MONITORING_EXECUTIONS_PATH;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.Param.*;
import static fr.cnes.regards.modules.processing.rest.utils.PageUtils.DEFAULT_PAGE;
import static fr.cnes.regards.modules.processing.rest.utils.PageUtils.DEFAULT_SIZE;
/**
 * This class defines REST endpoints to deal with monitoring in servlet application.
 *
 * @author gandrieu
 */
@RestController
@ConditionalOnProperty(name = "spring.main.web-application-type", havingValue = "servlet", matchIfMissing = true)
@RequestMapping(path = MONITORING_EXECUTIONS_PATH)
public class PMonitoringController implements IResourceController<ExecutionMonitoringDTO> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PMonitoringController.class);

    private final IMonitoringService monitoringService;

    @Autowired
    public PMonitoringController(IMonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @GetMapping
    @ResourceAccess(
            description = "List executions filtered by tenant/user/date/status depending on the given parameters",
            role = DefaultRole.ADMIN)
    public HttpEntity<PagedModel<EntityModel<ExecutionMonitoringDTO>>> executions(
            @RequestParam(name = TENANT_PARAM) String tenant,
            @RequestParam(name = STATUS_PARAM) List<ExecutionStatus> status,
            @RequestParam(name = USER_EMAIL_PARAM, required = false) String userEmail,
            @RequestParam(name = DATE_FROM_PARAM, defaultValue = "2000-01-01T00:00:00.000Z") String fromStr,
            @RequestParam(name = DATE_TO_PARAM, defaultValue = "2100-01-01T00:00:00.000Z") String toStr,
            @RequestParam(name = PAGE_PARAM, defaultValue = DEFAULT_PAGE) int page,
            @RequestParam(name = SIZE_PARAM, defaultValue = DEFAULT_SIZE) int size,
            final PagedResourcesAssembler<ExecutionMonitoringDTO> assembler
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        LOGGER.info("status={}", status);
        LOGGER.info("userEmail={}", userEmail);
        LOGGER.info("from={}", fromStr);
        LOGGER.info("to={}", toStr);
        OffsetDateTime from = TimeUtils.parseUtc(fromStr);
        OffsetDateTime to = TimeUtils.parseUtc(toStr);

        PageRequest paged = PageRequest.of(page, size);
        return monitoringService.getExecutionsPageForCriteria(tenant, status, userEmail, from, to, paged)
                .map(p -> new ResponseEntity<>(this.toPagedResources(p, assembler), HttpStatus.OK))
                .block();
    }

    @Override
    public EntityModel<ExecutionMonitoringDTO> toResource(ExecutionMonitoringDTO element, Object... extras) {
        return EntityModel.of(element);
    }
}
