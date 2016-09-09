package fr.cnes.regards.modules.project.service;

import java.util.List;

import javax.naming.OperationNotSupportedException;

import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.project.domain.Project;

public interface IProjectService {

    Project retrieveProject(String pProjectId);

    List<Project> deleteProject(String pProjectId);

    Project modifyProject(String pProjectId, Project pProject) throws OperationNotSupportedException;

    List<Project> retrieveProjectList();

    Project createProject(Project pNewProject) throws AlreadyExistingException;

}
