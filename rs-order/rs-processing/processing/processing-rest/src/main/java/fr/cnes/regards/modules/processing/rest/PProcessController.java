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

import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.PROCESS_PATH;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.Param.PROCESS_BUSINESS_ID_PARAM;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.processing.domain.dto.PProcessDTO;
import fr.cnes.regards.modules.processing.domain.service.IProcessService;
/**
 * This class defines REST endpoints to deal with processes in servlet application.
 *
 * @author gandrieu
 */
@RestController
@ConditionalOnProperty(name = "spring.main.web-application-type", havingValue = "servlet", matchIfMissing = true)
@RequestMapping(path = PROCESS_PATH)
public class PProcessController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PProcessController.class);

    private final IProcessService processService;

    private final IRuntimeTenantResolver tenantResolver;

    public PProcessController(IProcessService processService, IRuntimeTenantResolver tenantResolver) {
        this.processService = processService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping
    @ResourceAccess(description = "Find all registered processes", role = DefaultRole.REGISTERED_USER)
    public List<PProcessDTO> findAll() {
        String tenant = tenantResolver.getTenant();
        return processService.findByTenant(tenant).collectList().block();
    }

    @GetMapping(path = "/{" + PROCESS_BUSINESS_ID_PARAM + "}")
    @ResourceAccess(description = "Find process by their business uuid", role = DefaultRole.REGISTERED_USER)
    public PProcessDTO findByUuid(@PathVariable(PROCESS_BUSINESS_ID_PARAM) UUID processId) {
        String tenant = tenantResolver.getTenant();
        return processService.findByTenant(tenant).filter(p -> p.getProcessId().equals(processId)).next().block();
    }

}
