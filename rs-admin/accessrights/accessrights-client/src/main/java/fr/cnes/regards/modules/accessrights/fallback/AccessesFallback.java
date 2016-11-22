/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.fallback;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.client.IAccessesClient;
import fr.cnes.regards.modules.accessrights.domain.AccessRequestDTO;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;

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
    private static final String FALLBACK_ERROR_MESSAGE = "RS-ADMIN /accesses request error. Fallback.";

    @Override
    public ResponseEntity<List<Resource<ProjectUser>>> retrieveAccessRequestList() {
        LOG.error(FALLBACK_ERROR_MESSAGE);
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Override
    public ResponseEntity<Resource<AccessRequestDTO>> requestAccess(final AccessRequestDTO pAccessRequest) {
        LOG.error(FALLBACK_ERROR_MESSAGE);
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Override
    public ResponseEntity<Void> acceptAccessRequest(final Long pAccessId) {
        LOG.error(FALLBACK_ERROR_MESSAGE);
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Override
    public ResponseEntity<Void> denyAccessRequest(final Long pAccessId) {
        LOG.error(FALLBACK_ERROR_MESSAGE);
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Override
    public ResponseEntity<Void> removeAccessRequest(final Long pAccessId) {
        LOG.error(FALLBACK_ERROR_MESSAGE);
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.signature.IAccessesSignature#updateAccessSetting(fr.cnes.regards.modules.
     * accessrights.domain.projects.AccessSettings)
     */
    @Override
    public ResponseEntity<Void> updateAccessSettings(final AccessSettings pAccessSettings)
            throws EntityNotFoundException {
        LOG.error(FALLBACK_ERROR_MESSAGE);
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);

    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.signature.IAccessesSignature#getAccessSettings()
     */
    @Override
    public ResponseEntity<Resource<AccessSettings>> getAccessSettings() {
        LOG.error(FALLBACK_ERROR_MESSAGE);
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

}
