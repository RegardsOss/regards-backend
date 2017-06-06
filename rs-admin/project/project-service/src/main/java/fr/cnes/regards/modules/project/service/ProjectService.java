/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.service;

import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.amqp.IInstancePublisher;
import fr.cnes.regards.framework.amqp.event.tenant.TenantCreatedEvent;
import fr.cnes.regards.framework.amqp.event.tenant.TenantDeletedEvent;
import fr.cnes.regards.framework.jpa.instance.transactional.InstanceTransactional;
import fr.cnes.regards.framework.jpa.multitenant.properties.MultitenantDaoProperties;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.project.dao.IProjectRepository;
import fr.cnes.regards.modules.project.domain.Project;

/**
 *
 * Service class to manage REGARDS projects.
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Christophe Mertz
 * @author Sébastien Binda
 *
 * @since 1.0-SNAPSHOT
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
     * JPA Multitenants default configuration from properties file.
     */
    private final MultitenantDaoProperties defaultProperties;

    /**
     * AMQP message publisher
     */
    private final IInstancePublisher instancePublisher;

    /**
     * The constructor.
     *
     * @param pProjectRepository
     *            The JPA repository.
     */
    public ProjectService(final IProjectRepository pProjectRepository,
            final MultitenantDaoProperties pDefaultProperties, IInstancePublisher instancePublisher) {
        super();
        projectRepository = pProjectRepository;
        defaultProperties = pDefaultProperties;
        this.instancePublisher = instancePublisher;
    }

    @PostConstruct
    public void projectsInitialization() throws ModuleException {

        // Create project from properties files it does not exists yet
        for (final TenantConnection tenant : defaultProperties.getTenants()) {
            if (projectRepository.findOneByName(tenant.getTenant()) == null) {
                LOG.info(String.format("Creating new project %s from static properties configuration",
                                       tenant.getTenant()));
                Project project=new Project("", "", true, tenant.getTenant());
                project.setLabel(tenant.getTenant());
                project.setAccessible(true);
                createProject(project);
            }
        }
    }

    @Override
    public Project retrieveProject(final String pProjectName) throws ModuleException {
        final Project project = projectRepository.findOneByName(pProjectName);
        if (project == null) {
            throw new EntityNotFoundException(pProjectName, Project.class);
        }
        return project;
    }

    @Override
    public void deleteProject(final String pProjectName) throws ModuleException {
        final Project deleted = retrieveProject(pProjectName);
        deleted.setDeleted(true);
        projectRepository.save(deleted);
        // Publish tenant deletion
        TenantDeletedEvent tde = new TenantDeletedEvent();
        tde.setTenant(pProjectName);
        instancePublisher.publish(tde);
    }

    @Override
    public Project updateProject(final String pProjectName, final Project pProject) throws ModuleException {
        final Project theProject = projectRepository.findOneByName(pProjectName);
        if (theProject == null) {
            throw new EntityNotFoundException(pProjectName, Project.class);
        }
        if (theProject.isDeleted()) {
            throw new IllegalStateException("This project is deleted.");
        }
        if (!pProject.getName().equals(pProjectName)) {
            throw new EntityInvalidException("projectId and updated project does not match.");
        }
        return projectRepository.save(pProject);
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
        final Project theProject = projectRepository.findOneByName(pNewProject.getName());
        if (theProject != null) {
            throw new EntityAlreadyExistsException(pNewProject.getName());
        }

        Project project = projectRepository.save(pNewProject);

        // Publish tenant creation
        TenantCreatedEvent tce = new TenantCreatedEvent();
        tce.setTenant(pNewProject.getName());
        instancePublisher.publish(tce);

        return project;
    }

}
