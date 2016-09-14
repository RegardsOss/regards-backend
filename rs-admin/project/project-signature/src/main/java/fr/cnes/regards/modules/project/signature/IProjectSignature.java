/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.signature;

import java.util.List;

import feign.Param;
import feign.RequestLine;
import fr.cnes.regards.modules.project.domain.Project;

public interface IProjectSignature {

    @RequestLine("POST /projects")
    Project createProject(Project newProject);

    @RequestLine("GET /projects")
    List<Project> retrieveProjectList();

    @RequestLine("GET /projects/{project_id}")
    Project retrieveProject(@Param("project_id") String projectId);

    @RequestLine("PUT /projects/{project_id}")
    void lodifyProject(@Param("project_id") String projectId, Project projectUpdated);

    @RequestLine("DELETE /projects/{project_id}")
    void deleteProject(@Param("project_id") String projectId);
}
