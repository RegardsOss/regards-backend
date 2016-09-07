package fr.cnes.regards.modules.project.dao;

import java.util.List;

import fr.cnes.regards.modules.project.domain.Project;

public interface IDaoProject {

    public Project getProjectById(String projectName);

    public Project saveProject(Project projectToSave);

    public List<Project> getAllProject();

}
