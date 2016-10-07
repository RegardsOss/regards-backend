/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.fallback;

import java.util.List;

import javax.naming.OperationNotSupportedException;

import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.project.client.ProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;

@Component
public class ProjectsFallback implements ProjectsClient {

    @Override
    public HttpEntity<List<Resource<Project>>> retrieveProjectList() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<Resource<Project>> createProject(Project pNewProject) throws AlreadyExistingException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<Resource<Project>> retrieveProject(String pProjectId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<Void> modifyProject(String pProjectId, Project pProjectUpdated)
            throws OperationNotSupportedException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<Void> deleteProject(String pProjectId) {
        // TODO Auto-generated method stub
        return null;
    }

}
