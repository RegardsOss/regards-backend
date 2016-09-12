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

    public ProjectServiceStub() {
        super();
    }

    @PostConstruct
    public void init() {
        projects.add(new Project("desc", "icon", true, "name"));
    }

    @Override
    public Project retrieveProject(String pProjectId) {
        return projects.stream().filter(p -> p.getName().equals(pProjectId)).findFirst().get();
    }

    @Override
    public List<Project> deleteProject(String pProjectId) {
        projects = projects.stream().filter(p -> !p.getName().equals(pProjectId)).collect(Collectors.toList());
        return this.retrieveProjectList();
    }

    @Override
    public Project modifyProject(String projectId, Project pProject) throws OperationNotSupportedException {
        if (!existProject(projectId)) {
            throw new NoSuchElementException(projectId);
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

    public boolean existProject(String pProjectId) {
        return projects.stream().filter(p -> p.getName().equals(pProjectId)).findFirst().isPresent();
    }

}
