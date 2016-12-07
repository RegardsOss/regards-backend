/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.service;

import java.util.ArrayList;
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
    public List<Project> retrieveProjectList() {
        try (Stream<Project> stream = StreamSupport.stream(projectRepository.findAll().spliterator(), false)) {
            return stream.collect(Collectors.toList());
        }
    }

    @Override
    public Project createProject(final Project pNewProject) throws ModuleException {
        final Project theProject = projectRepository.findOneByName(pNewProject.getName());
        if (theProject != null) {
            throw new EntityAlreadyExistsException(pNewProject.getName());
        }

        return projectRepository.save(pNewProject);
    }

    @Override
    public List<Project> retrievePublicProjectList() {
        final List<Project> results = new ArrayList<>();
        retrieveProjectList().forEach(p -> {
            if (p.isPublic()) {
                results.add(p);
            }
        });
        return results;
    }

}
