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

import fr.cnes.regards.framework.amqp.IInstancePublisher;
import fr.cnes.regards.framework.amqp.event.tenant.TenantCreatedEvent;
import fr.cnes.regards.framework.amqp.event.tenant.TenantDeletedEvent;
import fr.cnes.regards.framework.jpa.instance.transactional.InstanceTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.project.dao.IProjectRepository;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.domain.ProjectUpdateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service class to manage REGARDS projects.
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Christophe Mertz
 * @author Sébastien Binda
 */
@Service
@InstanceTransactional
public class ProjectService implements IProjectService {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ProjectService.class);

    /**
     * JPA Repository to query projects from database
     */
    private final IProjectRepository projectRepository;

    /**
     * AMQP message publisher
     */
    private final IInstancePublisher instancePublisher;

    private final IProjectConnectionService projectConnectionService;

    /**
     * Default tenants which are to be initialized at system installation
     */
    private final Set<String> defaultTenants;

    /**
     * Default host to access default tenants.
     */
    private final String defaultTenantHost;

    /**
     * Default tenant name.
     */
    private final String instanceTenantName;

    public ProjectService(IProjectConnectionService projectConnectionService,
                          final IProjectRepository projectRepository,
                          IInstancePublisher instancePublisher,
                          @Value("${regards.default.tenants}") String defaultTenants,
                          @Value("${regards.config.first.project.public.access}") String defaultTenantHost,
                          @Value("${regards.instance.tenant.name:instance}") String instanceTenantName) {
        this.projectRepository = projectRepository;
        this.projectConnectionService = projectConnectionService;
        this.instancePublisher = instancePublisher;
        this.defaultTenants = Arrays.stream(defaultTenants.split(",")).map(String::trim).collect(Collectors.toSet());
        this.defaultTenantHost = defaultTenantHost;
        this.instanceTenantName = instanceTenantName;
    }

    @EventListener
    public void projectsInitialization(ApplicationReadyEvent applicationReadyEvent) throws ModuleException {
        LOG.info("Initializing projects");
        // Before creating projects, lets see if we need to initialize them by checking if any project already exists
        List<Project> projects = projectRepository.findAll();
        if (projects.isEmpty()) {
            for (String tenant : defaultTenants) {
                LOG.info(String.format("Creating new project %s from static properties configuration", tenant));
                Project project = new Project("", "", true, tenant);
                project.setLabel(tenant);
                project.setAccessible(true);
                project.setHost(this.defaultTenantHost);
                createProject(project);
            }
        } else {
            LOG.info("Projects are already initialized, skipping project initialization");
        }
    }

    @Override
    public Project retrieveProject(final String pProjectName) throws ModuleException {
        final Project project = projectRepository.findOneByNameIgnoreCase(pProjectName);
        if (project == null) {
            throw new EntityNotFoundException(pProjectName, Project.class);
        }
        return project;
    }

    @Override
    public void deleteProject(final String pProjectName) throws ModuleException {

        Project deleted = retrieveProject(pProjectName);
        deleted.setDeleted(true);
        projectRepository.save(deleted);

        // Remove all related connections
        projectConnectionService.deleteProjectConnections(deleted);

        // Publish tenant deletion
        TenantDeletedEvent tde = new TenantDeletedEvent();
        tde.setTenant(pProjectName);
        instancePublisher.publish(tde);
    }

    @Override
    public Project updateProject(final String pProjectName, final Project pProject) throws ModuleException {
        final Project theProject = projectRepository.findOneByNameIgnoreCase(pProjectName);
        if (theProject == null) {
            throw new EntityNotFoundException(pProjectName, Project.class);
        }
        if (theProject.isDeleted()) {
            throw new IllegalStateException("This project is deleted.");
        }
        if (!pProject.getName().equals(pProjectName)) {
            throw new EntityInvalidException("projectId and updated project does not match.");
        }
        Project project = projectRepository.save(pProject);
        ProjectUpdateEvent e = ProjectUpdateEvent.build(project);
        instancePublisher.publish(e);
        return project;
    }

    @Override
    public Page<Project> retrieveProjectList(final Pageable pPageable) {
        return projectRepository.findAll(pPageable);
    }

    @Override
    public List<Project> retrieveProjectList() {
        return projectRepository.findAll();
    }

    @Override
    public Page<Project> retrievePublicProjectList(final Pageable pPageable) {
        return projectRepository.findByIsPublicTrue(pPageable);
    }

    @Override
    public Project createProject(final Project pNewProject) throws ModuleException {
        final Project theProject = projectRepository.findOneByNameIgnoreCase(pNewProject.getName());
        if (theProject != null) {
            throw new EntityAlreadyExistsException("A Project with name " + pNewProject.getName() + " already exists");
        }
        if (pNewProject.getName().toLowerCase().equals(instanceTenantName)) {
            throw new ModuleException("You cannot create a tenant named " + instanceTenantName);

        }

        Project project = projectRepository.save(pNewProject);

        // Publish tenant creation
        TenantCreatedEvent tce = new TenantCreatedEvent();
        tce.setTenant(pNewProject.getName());
        instancePublisher.publish(tce);

        return project;
    }

}
