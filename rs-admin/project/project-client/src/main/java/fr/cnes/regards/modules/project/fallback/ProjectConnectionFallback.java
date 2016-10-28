/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.fallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.project.client.IProjectConnectionClient;
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
public class ProjectConnectionFallback implements IProjectConnectionClient {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ProjectConnectionFallback.class);

    /**
     * Common error message to log
     */
    private static final String FALLBACK_ERROR_MESSAGE = "RS-ADMIN / Projects request error. Fallback.";

    @Override
    public ResponseEntity<Resource<ProjectConnection>> retrieveProjectConnection(final String pProjectId,
            final String pMicroService) {
        LOG.error(FALLBACK_ERROR_MESSAGE);
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Override
    public ResponseEntity<Resource<ProjectConnection>> createProjectConnection(
            final ProjectConnection pProjectConnection) throws AlreadyExistingException {
        LOG.error(FALLBACK_ERROR_MESSAGE);
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Override
    public ResponseEntity<Resource<ProjectConnection>> updateProjectConnection(
            final ProjectConnection pProjectConnection) {
        LOG.error(FALLBACK_ERROR_MESSAGE);
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Override
    public ResponseEntity<Void> deleteProjectConnection(final String pProjectName, final String pMicroservice) {
        LOG.error(FALLBACK_ERROR_MESSAGE);
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

}
