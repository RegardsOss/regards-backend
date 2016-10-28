/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.EntityException;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;
import fr.cnes.regards.modules.core.exception.InvalidEntityException;
import fr.cnes.regards.modules.project.dao.IProjectRepository;
import fr.cnes.regards.modules.project.domain.Project;

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

    public ProjectService(final IProjectRepository pProjectRepository) {
        super();
        projectRepository = pProjectRepository;
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
        if (!existProject(pProjectName)) {
            throw new EntityNotFoundException(pProjectName, Project.class);
        }
        if (!notDeletedProject(pProjectName)) {
            throw new IllegalStateException("This project is deleted");
        }
        if (!pProject.getName().equals(pProjectName)) {
            throw new InvalidEntityException("projectId and updated project does not match");
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
        if (existProject(pNewProject.getName())) {
            throw new AlreadyExistingException(pNewProject.getName());
        }
        return projectRepository.save(pNewProject);
    }

    @Override
    public boolean existProject(final String pProjectId) {
        try (Stream<Project> stream = StreamSupport.stream(projectRepository.findAll().spliterator(), false)) {
            return stream.filter(p -> p.getName().equals(pProjectId)).findFirst().isPresent();
        }
    }

    @Override
    public boolean notDeletedProject(final String pProjectId) {
        try (Stream<Project> stream = StreamSupport.stream(projectRepository.findAll().spliterator(), false)) {
            return stream.filter(p -> !p.isDeleted()).filter(p -> p.getName().equals(pProjectId)).findFirst()
                    .isPresent();
        }
    }

}
