/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.rest;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.domain.ProjectConnection;
import fr.cnes.regards.modules.project.service.IProjectConnectionService;
import fr.cnes.regards.modules.project.service.IProjectService;

/**
 * @author Marc Sordi
 *
 */
@RestController
@RequestMapping("/connections/{microservice}")
public class TenantConnectionController {

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

    @ResourceAccess(description = "Add a tenant connection", role = DefaultRole.INSTANCE_ADMIN)
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<TenantConnection> addTenantConnection(@PathVariable String microservice,
            @Valid @RequestBody TenantConnection tenantConnection) throws ModuleException {

        Project project = projectService.retrieveProject(tenantConnection.getTenant());

        ProjectConnection projectConnection = new ProjectConnection();
        projectConnection.setDriverClassName(tenantConnection.getDriverClassName());
        projectConnection.setMicroservice(microservice);
        projectConnection.setPassword(tenantConnection.getPassword());
        projectConnection.setProject(project);
        projectConnection.setUrl(tenantConnection.getUrl());
        projectConnection.setUserName(tenantConnection.getUserName());

        projectConnectionService.createProjectConnection(projectConnection, true);
        ProjectConnection connection = projectConnectionService.enableProjectConnection(microservice,
                                                                                        project.getName());
        return ResponseEntity.ok(toTenantConnection(connection));
    }

    @ResourceAccess(description = "Enable a tenant connection", role = DefaultRole.INSTANCE_ADMIN)
    @RequestMapping(method = RequestMethod.PUT, value = "/{tenant}/enable")
    public ResponseEntity<TenantConnection> enableTenantConnection(@PathVariable("microservice") String microservice,
            @PathVariable("tenant") String tenant) throws ModuleException {

        ProjectConnection connection = projectConnectionService.enableProjectConnection(microservice, tenant);
        return ResponseEntity.ok(toTenantConnection(connection));
    }

    @ResourceAccess(description = "Disable a tenant connection", role = DefaultRole.INSTANCE_ADMIN)
    @RequestMapping(method = RequestMethod.PUT, value = "/{tenant}/disable")
    public ResponseEntity<TenantConnection> disableTenantConnection(@PathVariable("microservice") String microservice,
            @PathVariable("tenant") String tenant) throws ModuleException {

        ProjectConnection connection = projectConnectionService.disableProjectConnection(microservice, tenant);
        return ResponseEntity.ok(toTenantConnection(connection));
    }

    @ResourceAccess(description = "List all enabled tenant connections for a specified microservice",
            role = DefaultRole.INSTANCE_ADMIN)
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<TenantConnection>> getTenantConnections(@PathVariable String microservice)
            throws ModuleException {
        List<ProjectConnection> projectConnections = projectConnectionService.retrieveProjectConnection(microservice);
        // Transform to tenant connection
        List<TenantConnection> tenantConnections = new ArrayList<>();
        if (projectConnections != null) {
            for (ProjectConnection projectConnection : projectConnections) {
                if (projectConnection.isEnabled()) {
                    tenantConnections.add(toTenantConnection(projectConnection));
                }
            }
        }

        return ResponseEntity.ok(tenantConnections);
    }

    /**
     * Transform a {@link ProjectConnection} in {@link TenantConnection}
     *
     * @param projectConnection
     *            {@link ProjectConnection}
     * @return {@link TenantConnection}
     */
    private TenantConnection toTenantConnection(ProjectConnection projectConnection) {
        TenantConnection tenantConnection = new TenantConnection();
        tenantConnection.setDriverClassName(projectConnection.getDriverClassName());
        tenantConnection.setTenant(projectConnection.getProject().getName());
        tenantConnection.setPassword(projectConnection.getPassword());
        tenantConnection.setUrl(projectConnection.getUrl());
        tenantConnection.setUserName(projectConnection.getUserName());
        return tenantConnection;
    }
}
