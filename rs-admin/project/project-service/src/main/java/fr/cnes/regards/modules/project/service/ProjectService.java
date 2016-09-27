/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.naming.OperationNotSupportedException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.project.dao.IProjectRepository;
import fr.cnes.regards.modules.project.domain.Project;

@Service
public class ProjectService implements IProjectService {

    @Autowired
    private IProjectRepository projectRepository_;

    @Override
    public Project retrieveProject(String pProjectName) {
        return projectRepository_.findOneByName(pProjectName);
    }

    @Override
    public List<Project> deleteProject(String pProjectId) {
        Project deleted = retrieveProject(pProjectId);
        deleted.setDeleted(true);
        projectRepository_.delete(deleted);
        return this.retrieveProjectList();
    }

    @Override
    public Project modifyProject(String projectId, Project pProject) throws OperationNotSupportedException {
        if (!existProject(projectId)) {
            throw new NoSuchElementException(projectId);
        }
        if (!notDeletedProject(projectId)) {
            throw new IllegalStateException("This project is deleted");
        }
        if (!pProject.getName().equals(projectId)) {
            throw new OperationNotSupportedException("projectId and updated project does not match");
        }
        return projectRepository_.save(pProject);
    }

    @Override
    public List<Project> retrieveProjectList() {
        return StreamSupport.stream(projectRepository_.findAll().spliterator(), false).collect(Collectors.toList());
    }

    @Override
    public Project createProject(Project pNewProject) throws AlreadyExistingException {
        if (existProject(pNewProject.getName())) {
            throw new AlreadyExistingException(pNewProject.getName());
        }
        return projectRepository_.save(pNewProject);
    }

    @Override
    public boolean existProject(String pProjectId) {
        return StreamSupport.stream(projectRepository_.findAll().spliterator(), false)
                .filter(p -> p.getName().equals(pProjectId)).findFirst().isPresent();
    }

    @Override
    public boolean notDeletedProject(String pProjectId) {
        return StreamSupport.stream(projectRepository_.findAll().spliterator(), false).filter(p -> !p.isDeleted())
                .filter(p -> p.getName().equals(pProjectId)).findFirst().isPresent();
    }

}
