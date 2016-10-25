/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.multitenant.autoconfigure.tenant.ITenantResolver;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.service.IProjectService;

/**
 *
 * Class LocalTenantResolver
 *
 * Administration microservice local tenant resolver.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public class LocalTenantResolver implements ITenantResolver {

    /**
     * Administration project service
     */
    @Autowired
    private IProjectService projectService;

    @Override
    public Set<String> getAllTenants() {
        final Set<String> tenants = new HashSet<>();
        final List<Project> projects = projectService.retrieveProjectList();
        projects.forEach(project -> tenants.add(project.getName()));
        return tenants;

    }

}
