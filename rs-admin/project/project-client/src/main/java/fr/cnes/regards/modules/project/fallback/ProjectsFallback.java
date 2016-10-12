/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.fallback;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.EntityException;
import fr.cnes.regards.modules.project.client.ProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.domain.ProjectConnection;

@Component
public class ProjectsFallback implements ProjectsClient {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectsFallback.class);

    private static final String FALLBACK_ERROR_MESSAGE = "RS-ADMIN / Projects request error. Fallback.";

    @Override
    public HttpEntity<List<Resource<Project>>> retrieveProjectList() {
        LOG.error(FALLBACK_ERROR_MESSAGE);
        return null;
    }

    @Override
    public HttpEntity<Resource<Project>> createProject(final Project pNewProject) throws AlreadyExistingException {
        LOG.error(FALLBACK_ERROR_MESSAGE);
        return null;
    }

    @Override
    public HttpEntity<Resource<Project>> retrieveProject(final String pProjectId) {
        LOG.error(FALLBACK_ERROR_MESSAGE);
        return null;
    }

    @Override
    public HttpEntity<Void> modifyProject(final String pProjectId, final Project pProjectUpdated)
            throws EntityException {
        LOG.error(FALLBACK_ERROR_MESSAGE);
        return null;
    }

    @Override
    public HttpEntity<Void> deleteProject(final String pProjectId) {
        LOG.error(FALLBACK_ERROR_MESSAGE);
        return null;
    }

    @Override
    public HttpEntity<Resource<ProjectConnection>> retrieveProjectConnection(final String pProjectId,
            final String pMicroService) {
        LOG.error(FALLBACK_ERROR_MESSAGE);
        return null;
    }

    @Override
    public HttpEntity<Resource<ProjectConnection>> createProjectConnection(final ProjectConnection pProjectConnection)
            throws AlreadyExistingException {
        LOG.error(FALLBACK_ERROR_MESSAGE);
        return null;
    }

    @Override
    public HttpEntity<Resource<ProjectConnection>> updateProjectConnection(final ProjectConnection pProjectConnection) {
        LOG.error(FALLBACK_ERROR_MESSAGE);
        return null;
    }

    @Override
    public HttpEntity<Void> deleteProjectConnection(final String pProjectName, final String pMicroservice) {
        LOG.error(FALLBACK_ERROR_MESSAGE);
        return null;
    }

}
