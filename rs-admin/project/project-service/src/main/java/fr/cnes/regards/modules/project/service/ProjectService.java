/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;
import fr.cnes.regards.framework.jpa.multitenant.properties.MultitenantDaoProperties;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;
import fr.cnes.regards.framework.module.rest.exception.AlreadyExistingException;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.InvalidEntityException;
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
     * The constructor.
     *
     * @param pProjectRepository
     *            The JPA repository.
     */
    public ProjectService(final IProjectRepository pProjectRepository,
            final MultitenantDaoProperties pDefaultProperties) {
        super();
        projectRepository = pProjectRepository;
        defaultProperties = pDefaultProperties;
    }

    /**
     *
     * Initialize projects.
     *
     * @throws RabbitMQVhostException
     *
     * @since 1.0-SNAPHOT
     */
    @PostConstruct
    public void projectsInitialization() throws RabbitMQVhostException {

        // Create project from properties files it does not exists yet
        for (final TenantConnection tenant : defaultProperties.getTenants()) {
            if (projectRepository.findOneByName(tenant.getName()) == null) {
                LOG.info(String.format("Creating new project %s from static properties configuration",
                                       tenant.getName()));
                projectRepository.save(new Project("", "", true, tenant.getName()));
            }
        }
    }

    @Override
    public Project retrieveProject(final String pProjectName) throws EntityNotFoundException {
        final Project project = projectRepository.findOneByName(pProjectName);
        if (project == null) {
            throw new EntityNotFoundException(pProjectName, Project.class);
        }
        return project;
    }

    @Override
    public List<Project> deleteProject(final String pProjectName) throws EntityNotFoundException {
        final Project deleted = retrieveProject(pProjectName);
        deleted.setDeleted(true);
        projectRepository.delete(deleted);
        return this.retrieveProjectList();
    }

    @Override
    public Project updateProject(final String pProjectName, final Project pProject) throws EntityException {
        final Project theProject = projectRepository.findOneByName(pProjectName);
        if (theProject == null) {
            throw new EntityNotFoundException(pProjectName, Project.class);
        }
        if (theProject.isDeleted()) {
            throw new IllegalStateException("This project is deleted.");
        }
        if (!pProject.getName().equals(pProjectName)) {
            throw new InvalidEntityException("projectId and updated project does not match.");
        }
        return projectRepository.save(pProject);
    }

    @Override
    public List<Project> retrieveProjectList() {
        try (Stream<Project> stream = StreamSupport.stream(projectRepository.findAll().spliterator(), false)) {
            return stream.collect(Collectors.toList());
        }
    }

    @Override
    public Project createProject(final Project pNewProject) throws AlreadyExistingException {
        final Project theProject = projectRepository.findOneByName(pNewProject.getName());
        if (theProject != null) {
            throw new AlreadyExistingException(pNewProject.getName());
        }

        return projectRepository.save(pNewProject);
    }

}
