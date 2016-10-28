/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;
import fr.cnes.regards.modules.project.dao.IProjectConnectionRepository;
import fr.cnes.regards.modules.project.dao.IProjectRepository;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.domain.ProjectConnection;

/**
 *
 * Class ProjectService
 *
 * Service class to manage REGARDS projects.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Service
public class ProjectConnectionService implements IProjectConnectionService {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ProjectConnectionService.class);

    /**
     * JPA Repository to query projects from database
     */
    private final IProjectRepository projectRepository;

    /**
     * JPA Repository to query projectConnection from database
     */
    private final IProjectConnectionRepository projectConnectionRepository;

    public ProjectConnectionService(final IProjectRepository pProjectRepository,
            final IProjectConnectionRepository pProjectConnectionRepository) {
        super();
        projectRepository = pProjectRepository;
        projectConnectionRepository = pProjectConnectionRepository;
    }

    @Override
    public ProjectConnection retreiveProjectConnection(final String pProjectName, final String pMicroService)
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
