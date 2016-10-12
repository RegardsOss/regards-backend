/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.EntityException;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;
import fr.cnes.regards.modules.core.exception.InvalidEntityException;
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
     * JPA Repository to query projectConnection from database
     */
    private final IProjectConnectionRepository projectConnectionRepository;

    public ProjectService(final IProjectRepository pProjectRepository,
            final IProjectConnectionRepository pProjectConnectionRepository) {
        super();
        projectRepository = pProjectRepository;
        projectConnectionRepository = pProjectConnectionRepository;
    }

    @Override
    public Project retrieveProject(final String pProjectName) {
        return projectRepository.findOneByName(pProjectName);
    }

    @Override
    public List<Project> deleteProject(final String pProjectId) {
        final Project deleted = retrieveProject(pProjectId);
        deleted.setDeleted(true);
        projectRepository.delete(deleted);
        return this.retrieveProjectList();
    }

    @Override
    public Project modifyProject(final String projectId, final Project pProject) throws EntityException {
        if (!existProject(projectId)) {
            throw new EntityNotFoundException(projectId, Project.class);
        }
        if (!notDeletedProject(projectId)) {
            throw new IllegalStateException("This project is deleted");
        }
        if (!pProject.getName().equals(projectId)) {
            throw new InvalidEntityException("projectId and updated project does not match");
        }
        return projectRepository.save(pProject);
    }

    @Override
    public List<Project> retrieveProjectList() {
        return StreamSupport.stream(projectRepository.findAll().spliterator(), false).collect(Collectors.toList());
    }

    @Override
    public Project createProject(final Project pNewProject) throws AlreadyExistingException {
        if (existProject(pNewProject.getName())) {
            throw new AlreadyExistingException(pNewProject.getName());
        }
        return projectRepository.save(pNewProject);
    }

    @Override
    public boolean existProject(final String pProjectId) {
        return StreamSupport.stream(projectRepository.findAll().spliterator(), false)
                .filter(p -> p.getName().equals(pProjectId)).findFirst().isPresent();
    }

    @Override
    public boolean notDeletedProject(final String pProjectId) {
        return StreamSupport.stream(projectRepository.findAll().spliterator(), false).filter(p -> !p.isDeleted())
                .filter(p -> p.getName().equals(pProjectId)).findFirst().isPresent();
    }

    @Override
    public ProjectConnection retreiveProjectConnection(final String pProjectId, final String pMicroService) {
        return projectConnectionRepository.findOneByProjectNameAndMicroservice(pProjectId, pMicroService);
    }

    @Override
    public ProjectConnection createProjectConnection(final ProjectConnection pProjectConnection)
            throws AlreadyExistingException, EntityNotFoundException {
        ProjectConnection connection;
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
            final String message = String.format(
                                                 "Invalid entity <ProjectConnection> for deletion. Entity (id=%d) does not exists",
                                                 pProjectConnectionId);
            LOG.error(message);
            throw new EntityNotFoundException(pProjectConnectionId.toString(), ProjectConnection.class);
        }
    }

    @Override
    public ProjectConnection updateProjectConnection(final ProjectConnection pProjectConnection)
            throws EntityNotFoundException {
        ProjectConnection connection;
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
