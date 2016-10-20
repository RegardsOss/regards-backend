/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.fallback;

import java.util.List;

import javax.naming.OperationNotSupportedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.accessRights.client.IAccessesClient;
import fr.cnes.regards.modules.accessRights.domain.AccessRequestDTO;
import fr.cnes.regards.modules.accessRights.domain.projects.AccessSettings;
import fr.cnes.regards.modules.accessRights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;

/**
 *
 * Class AccessesFallback
 *
 * Fallback for Accesses Feign client. This implementation is used in case of error during feign client calls.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Component
public class AccessesFallback implements IAccessesClient {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(AccessesFallback.class);

    /**
     * Common error message to log
     */
    private static final String fallBackErrorMessage = "RS-ADMIN /accesses request error. Fallback.";

    @Override
    public HttpEntity<List<Resource<ProjectUser>>> retrieveAccessRequestList() {
        LOG.error(fallBackErrorMessage);
        final ResponseEntity<List<Resource<ProjectUser>>> response = new ResponseEntity<>(
                HttpStatus.SERVICE_UNAVAILABLE);
        return response;
    }

    @Override
    public HttpEntity<Resource<AccessRequestDTO>> requestAccess(final AccessRequestDTO pAccessRequest)
            throws AlreadyExistingException {
        LOG.error(fallBackErrorMessage);
        final ResponseEntity<Resource<AccessRequestDTO>> response = new ResponseEntity<>(
                HttpStatus.SERVICE_UNAVAILABLE);
        return response;
    }

    @Override
    public HttpEntity<Void> acceptAccessRequest(final Long pAccessId) throws OperationNotSupportedException {
        LOG.error(fallBackErrorMessage);
        final ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        return response;
    }

    @Override
    public HttpEntity<Void> denyAccessRequest(final Long pAccessId) throws OperationNotSupportedException {
        LOG.error(fallBackErrorMessage);
        final ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        return response;
    }

    @Override
    public HttpEntity<Void> removeAccessRequest(final Long pAccessId) throws EntityNotFoundException {
        LOG.error(fallBackErrorMessage);
        final ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        return response;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessRights.signature.IAccessesSignature#updateAccessSetting(fr.cnes.regards.modules.
     * accessRights.domain.projects.AccessSettings)
     */
    @Override
    public HttpEntity<Void> updateAccessSettings(final AccessSettings pAccessSettings) throws EntityNotFoundException {
        LOG.error(fallBackErrorMessage);
        final ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        return response;

    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessRights.signature.IAccessesSignature#getAccessSettings()
     */
    @Override
    public HttpEntity<Resource<AccessSettings>> getAccessSettings() {
        LOG.error(fallBackErrorMessage);
        final ResponseEntity<Resource<AccessSettings>> response = new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        return response;
    }

}
