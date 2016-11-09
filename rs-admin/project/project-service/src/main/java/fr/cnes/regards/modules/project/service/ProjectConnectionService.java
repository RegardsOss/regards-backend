/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.service;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.properties.MultitenantDaoProperties;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;
import fr.cnes.regards.framework.module.rest.exception.AlreadyExistingException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.project.dao.IProjectConnectionRepository;
import fr.cnes.regards.modules.project.dao.IProjectRepository;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.domain.ProjectConnection;

/**
 *
 * Service class to manage REGARDS projects.
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Christophe Mertz
 *
 * @since 1.0-SNAPSHOT
 */
@Service
public class ProjectConnectionService implements IProjectConnectionService {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ProjectConnectionService.class);

    /**
     * Name of the current microservice
     */
    private final String microserviceName;

    /**
     * JPA Repository to query projects from database
     */
    private final IProjectRepository projectRepository;

    /**
     * JPA Repository to query projectConnection from database
     */
    private final IProjectConnectionRepository projectConnectionRepository;

    /**
     * JPA Multitenants default configuration from properties file.
     */
    private final MultitenantDaoProperties defaultProperties;

    /**
     * The constructor.
     *
     * @param pProjectRepository
     *            The JPA {@link Project} repository.
     * @param pProjectConnectionRepository
     *            The JPA {@link ProjectConnection} repository.
     * @param pDefaultProperties
     *            multitenant DAO properties form config file
     * @param pMicroserviceName
     *            current microservice name
     */
    public ProjectConnectionService(final IProjectRepository pProjectRepository,
            final IProjectConnectionRepository pProjectConnectionRepository,
            final MultitenantDaoProperties pDefaultProperties,
            @Value("${spring.application.name}") final String pMicroserviceName) {
        super();
        projectRepository = pProjectRepository;
        projectConnectionRepository = pProjectConnectionRepository;
        defaultProperties = pDefaultProperties;
        microserviceName = pMicroserviceName;
    }

    /**
     *
     * Initialize projects.
     *
     * @since 1.0-SNAPHOT
     */
    @PostConstruct
    public void projectsInitialization() {
        // Create project from properties files it does not exists yet
        for (final TenantConnection tenant : defaultProperties.getTenants()) {
            if (projectRepository.findOneByName(tenant.getName()) == null) {
                LOG.info(String.format("Creating new project %s from static properties configuration",
                                       tenant.getName()));
                final Project project = projectRepository.save(new Project("", "", true, tenant.getName()));
                // Then create connection for current microservice
                projectConnectionRepository.save(new ProjectConnection(project, microserviceName, tenant.getUserName(),
                        tenant.getPassword(), tenant.getDriverClassName(), tenant.getUrl()));
            }
        }
    }

    @Override
    public ProjectConnection retrieveProjectConnection(final String pProjectName, final String pMicroService)
            throws EntityNotFoundException {
        final ProjectConnection connection = projectConnectionRepository
                .findOneByProjectNameAndMicroservice(pProjectName, pMicroService);
        if (connection == null) {
            throw new EntityNotFoundException(String.format("%s:%s", pProjectName, pMicroService),
                    ProjectConnection.class);
        }
        return connection;
    }

    @Override
    public ProjectConnection createProjectConnection(final ProjectConnection pProjectConnection)
            throws AlreadyExistingException, EntityNotFoundException {
        final ProjectConnection connection;
        final Project project = pProjectConnection.getProject();
        // Check referenced project exists
        if ((project.getId() != null) && projectRepository.exists(project.getId())) {
            // Check project connection to create doesn't already exists
            if (projectConnectionRepository
                    .findOneByProjectNameAndMicroservice(project.getName(),
                                                         pProjectConnection.getMicroservice()) == null) {
                connection = projectConnectionRepository.save(pProjectConnection);
            } else {
                throw new AlreadyExistingException(project.getName());
            }
        } else {
            throw new EntityNotFoundException(pProjectConnection.getId().toString(), ProjectConnection.class);
        }

        return connection;
    }

    @Override
    public void deleteProjectConnection(final Long pProjectConnectionId) throws EntityNotFoundException {
        if (projectConnectionRepository.exists(pProjectConnectionId)) {
            projectConnectionRepository.delete(pProjectConnectionId);
        } else {
            final String message = "Invalid entity <ProjectConnection> for deletion. Entity (id=%d) does not exists";
            LOG.error(String.format(message, pProjectConnectionId));
            throw new EntityNotFoundException(pProjectConnectionId.toString(), ProjectConnection.class);
        }
    }

    @Override
    public ProjectConnection updateProjectConnection(final ProjectConnection pProjectConnection)
            throws EntityNotFoundException {
        final ProjectConnection connection;
        // Check that entity to update exists
        if ((pProjectConnection.getId() != null) && projectConnectionRepository.exists(pProjectConnection.getId())) {
            final Project project = pProjectConnection.getProject();
            // Check that the referenced project exists
            if ((project.getId() != null) && projectRepository.exists(project.getId())) {
                // Update entity
                connection = projectConnectionRepository.save(pProjectConnection);
            } else {
                throw new EntityNotFoundException(project.getName(), Project.class);
            }
        } else {
            throw new EntityNotFoundException(pProjectConnection.getId().toString(), ProjectConnection.class);
        }
        return connection;
    }

}
