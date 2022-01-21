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
package fr.cnes.regards.modules.project.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnectionState;
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
        List<Project> projects = projectRepository.findByIsDeletedFalse();
        if (projects != null) {
            projects.forEach(project -> tenants.add(project.getName()));
        }
        return tenants;
    }

    /*
     * Retrieve all tenants fully configured : i.e. tenants that have a database configuration
     */
    @Override
    public Set<String> getAllActiveTenants(String microserviceName) {
        Assert.notNull(microserviceName, "Microservice name is required");
        Set<String> tenants = new HashSet<>();
        // Retrieve all projects
        List<Project> projects = projectRepository.findByIsDeletedFalse();
        for (Project project : projects) {
            ProjectConnection pc = projectConnectionRepository.findOneByProjectNameAndMicroservice(project.getName(),
                                                                                                   microserviceName);
            if ((pc != null) && TenantConnectionState.ENABLED.equals(pc.getState())) {
                tenants.add(project.getName());
            }
        }
        return tenants;
    }

}
