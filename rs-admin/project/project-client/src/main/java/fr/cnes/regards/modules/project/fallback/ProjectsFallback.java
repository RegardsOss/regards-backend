/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.fallback;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.EntityException;
import fr.cnes.regards.modules.project.client.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.domain.ProjectConnection;

/**
 *
 * Class ProjectsFallback
 *
 * Fallback methods for Projects client.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Component
public class ProjectsFallback implements IProjectsClient {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ProjectsFallback.class);

    /**
     * Common error message to log
     */
    private static final String FALLBACK_ERROR_MESSAGE = "RS-ADMIN / Projects request error. Fallback.";

    @Override
    public ResponseEntity<List<Resource<Project>>> retrieveProjectList() {
        LOG.error(FALLBACK_ERROR_MESSAGE);
        final ResponseEntity<List<Resource<Project>>> response = new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        return response;
    }

    @Override
    public ResponseEntity<Resource<Project>> createProject(final Project pNewProject) throws AlreadyExistingException {
        LOG.error(FALLBACK_ERROR_MESSAGE);
        final ResponseEntity<Resource<Project>> response = new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        return response;
    }

    @Override
    public ResponseEntity<Resource<Project>> retrieveProject(final String pProjectId) {
        LOG.error(FALLBACK_ERROR_MESSAGE);
        final ResponseEntity<Resource<Project>> response = new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        return response;
    }

    @Override
    public ResponseEntity<Resource<Project>> updateProject(final String pProjectId, final Project pProjectUpdated)
            throws EntityException {
        LOG.error(FALLBACK_ERROR_MESSAGE);
        final ResponseEntity<Resource<Project>> response = new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        return response;
    }

    @Override
    public ResponseEntity<Void> deleteProject(final String pProjectId) {
        LOG.error(FALLBACK_ERROR_MESSAGE);
        final ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        return response;
    }

    @Override
    public ResponseEntity<Resource<ProjectConnection>> retrieveProjectConnection(final String pProjectId,
            final String pMicroService) {
        LOG.error(FALLBACK_ERROR_MESSAGE);
        final ResponseEntity<Resource<ProjectConnection>> response = new ResponseEntity<>(
                HttpStatus.SERVICE_UNAVAILABLE);
        return response;
    }

    @Override
    public ResponseEntity<Resource<ProjectConnection>> createProjectConnection(
            final ProjectConnection pProjectConnection) throws AlreadyExistingException {
        LOG.error(FALLBACK_ERROR_MESSAGE);
        final ResponseEntity<Resource<ProjectConnection>> response = new ResponseEntity<>(
                HttpStatus.SERVICE_UNAVAILABLE);
        return response;
    }

    @Override
    public ResponseEntity<Resource<ProjectConnection>> updateProjectConnection(
            final ProjectConnection pProjectConnection) {
        LOG.error(FALLBACK_ERROR_MESSAGE);
        final ResponseEntity<Resource<ProjectConnection>> response = new ResponseEntity<>(
                HttpStatus.SERVICE_UNAVAILABLE);
        return response;
    }

    @Override
    public ResponseEntity<Void> deleteProjectConnection(final String pProjectName, final String pMicroservice) {
        LOG.error(FALLBACK_ERROR_MESSAGE);
        final ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        return response;
    }

}
