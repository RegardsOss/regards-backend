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
package fr.cnes.regards.modules.project.rest;

import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.domain.ProjectConnection;
import fr.cnes.regards.modules.project.service.IProjectConnectionService;
import fr.cnes.regards.modules.project.service.IProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * System API for managing tenant connection lifecycle. Should only be used by other microservices.
 *
 * @author Marc Sordi
 */
@RestController
@RequestMapping("/connections/{microservice}")
public class TenantConnectionController {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(TenantConnectionController.class);

    /**
     * {@link ProjectConnection} service
     */
    private final IProjectConnectionService projectConnectionService;

    /**
     * {@link Project} service
     */
    private final IProjectService projectService;

    public TenantConnectionController(IProjectConnectionService connectionService, IProjectService projectService) {
        this.projectConnectionService = connectionService;
        this.projectService = projectService;
    }

    /**
     * This endpoint is exclusively called by JPA multitenant starter to init first project (static configuration).
     * Allows the system to register a tenant connection.
     *
     * @param microservice     target microservice
     * @param tenantConnection connection to register
     * @return registered connection
     * @throws ModuleException if error occurs!
     */
    @ResourceAccess(description = "Add a project (i.e. tenant) connection", role = DefaultRole.INSTANCE_ADMIN)
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<TenantConnection> addTenantConnection(@PathVariable String microservice,
                                                                @Valid @RequestBody TenantConnection tenantConnection)
        throws ModuleException {
        Project project = projectService.retrieveProject(tenantConnection.getTenant());
        ProjectConnection projectConnection = new ProjectConnection();
        projectConnection.setDriverClassName(tenantConnection.getDriverClassName());
        projectConnection.setMicroservice(microservice);
        projectConnection.setPassword(tenantConnection.getPassword());
        projectConnection.setProject(project);
        projectConnection.setUrl(tenantConnection.getUrl());
        projectConnection.setUserName(tenantConnection.getUserName());
        ProjectConnection connection = projectConnectionService.createStaticProjectConnection(projectConnection);
        return ResponseEntity.ok(connection.toTenantConnection());
    }

    /**
     * Allows the system to update connection state. Only tenant, state and errorCause are useful.
     *
     * @param microservice     target microservice
     * @param tenantConnection connection to update
     * @return updated connection
     */
    @ResourceAccess(description = "Update a project (i.e. tenant) connection state")
    @RequestMapping(method = RequestMethod.PUT)
    public ResponseEntity<TenantConnection> updateState(@PathVariable String microservice,
                                                        @Valid @RequestBody TenantConnection tenantConnection)
        throws ModuleException {
        ProjectConnection connection = projectConnectionService.updateState(microservice,
                                                                            tenantConnection.getTenant(),
                                                                            tenantConnection.getState(),
                                                                            Optional.ofNullable(tenantConnection.getErrorCause()));
        return ResponseEntity.ok(connection.toTenantConnection());
    }

    @ResourceAccess(description = "List all enabled project (i.e. tenant) connections for a specified microservice",
                    role = DefaultRole.INSTANCE_ADMIN)
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<TenantConnection>> getTenantConnections(@PathVariable String microservice) {
        List<ProjectConnection> projectConnections = projectConnectionService.retrieveProjectConnections(microservice);
        // Transform to tenant connection
        List<TenantConnection> tenantConnections = new ArrayList<>();
        if (projectConnections != null) {
            for (ProjectConnection projectConnection : projectConnections) {
                tenantConnections.add(projectConnection.toTenantConnection());
            }
        }
        return ResponseEntity.ok(tenantConnections);
    }
}
