/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import fr.cnes.regards.modules.project.dao.IProjectConnectionRepository;
import fr.cnes.regards.modules.project.dao.IProjectRepository;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.domain.ProjectConnection;

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

    /**
     * JPA Repository to query projectConnection from database
     */
    private final IProjectConnectionRepository projectConnectionRepository;

    public TenantService(IProjectRepository pProjectRepository,
            IProjectConnectionRepository pProjectConnectionRepository) {
        this.projectRepository = pProjectRepository;
        this.projectConnectionRepository = pProjectConnectionRepository;
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

    /*
     * Retrieve all tenants fully configured : i.e. tenants that have a database configuration
     */
    @Override
    public Set<String> getAllActiveTenants(String pMicroserviceName) {
        Assert.notNull(pMicroserviceName);
        Set<String> tenants = new HashSet<>();
        // Retrieve all projects
        List<Project> projects = projectRepository.findAll();
        for (Project project : projects) {
            ProjectConnection pc = projectConnectionRepository.findOneByProjectNameAndMicroservice(project.getName(),
                                                                                                   pMicroserviceName);
            if ((pc != null) && pc.isEnabled()) {
                tenants.add(project.getName());
            }
        }
        return tenants;
    }

}
