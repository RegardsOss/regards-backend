/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.project.dao.IProjectRepository;
import fr.cnes.regards.modules.project.domain.Project;

/**
 * Default implementation of {@link ITenantService}.
 *
 * @author Marc Sordi
 *
 */
@Service
public class TenantService implements ITenantService {

    /**
     * JPA Repository to query projects from database
     */
    private final IProjectRepository projectRepository;

    public TenantService(IProjectRepository pProjectRepository) {
        this.projectRepository = pProjectRepository;
    }

    @Override
    public Set<String> getAllTenants() {
        Set<String> tenants = new HashSet<>();
        List<Project> projects = projectRepository.findAll();
        if (projects != null) {
            projects.forEach(project -> tenants.add(project.getName()));
        }
        return tenants;
    }

}
