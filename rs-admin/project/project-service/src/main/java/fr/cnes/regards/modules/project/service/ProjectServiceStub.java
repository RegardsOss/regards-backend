/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.naming.OperationNotSupportedException;

import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.project.domain.Project;

@Service
public class ProjectServiceStub implements IProjectService {

    private static List<Project> projects = new ArrayList<>();

    @PostConstruct
    public void init() {
        projects.add(new Project(0L, "desc", "icon", true, "name"));
    }

    @Override
    public Project retrieveProject(String pProjectId) {
        return projects.stream().filter(p -> p.getName().equals(pProjectId)).findFirst().get();
    }

    @Override
    public List<Project> deleteProject(String pProjectId) {

        Project deleted = retrieveProject(pProjectId);
        deleted.setDeleted(true);
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
        projects.stream().map(p -> p.equals(pProject) ? pProject : p).collect(Collectors.toList());
        return pProject;
    }

    @Override
    public List<Project> retrieveProjectList() {
        return projects;
    }

    @Override
    public Project createProject(Project pNewProject) throws AlreadyExistingException {
        if (existProject(pNewProject.getName())) {
            throw new AlreadyExistingException(pNewProject.getName());
        }
        projects.add(pNewProject);
        return pNewProject;
    }

    @Override
    public boolean existProject(String pProjectId) {
        return projects.stream().filter(p -> p.getName().equals(pProjectId)).findFirst().isPresent();
    }

    @Override
    public boolean notDeletedProject(String pProjectId) {
        return projects.stream().filter(p -> !p.isDeleted()).filter(p -> p.getName().equals(pProjectId)).findFirst()
                .isPresent();
    }

}
