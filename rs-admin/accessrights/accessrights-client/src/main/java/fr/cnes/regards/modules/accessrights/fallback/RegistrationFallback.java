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

import fr.cnes.regards.modules.accessrights.client.IRegistrationClient;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;

/**
 *
 * Class RegistrationFallback
 *
 * Fallback for Accesses Feign client. This implementation is used in case of error during feign client calls.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Component
public class RegistrationFallback implements IRegistrationClient {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(RegistrationFallback.class);

    /**
     * Common error message to log
     */
    private static final String FALLBACK_ERROR_MESSAGE = "RS-ADMIN /accesses request error. Fallback.";

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.client.IAccessesClient#retrieveAccessRequestList()
     */
    @Override
    public ResponseEntity<List<Resource<ProjectUser>>> retrieveAccessRequestList() {
        LOG.error(FALLBACK_ERROR_MESSAGE);
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.client.IAccessesClient#requestAccess()
     */
    @Override
    public ResponseEntity<Resource<AccessRequestDto>> requestAccess(final AccessRequestDto pAccessRequest) {
        LOG.error(FALLBACK_ERROR_MESSAGE);
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.client.IAccessesClient#acceptAccessRequest()
     */
    @Override
    public ResponseEntity<Void> acceptAccessRequest(final Long pAccessId) {
        LOG.error(FALLBACK_ERROR_MESSAGE);
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.client.IAccessesClient#denyAccessRequest()
     */
    @Override
    public ResponseEntity<Void> denyAccessRequest(final Long pAccessId) {
        LOG.error(FALLBACK_ERROR_MESSAGE);
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.client.IAccessesClient#removeAccessRequest()
     */
    @Override
    public ResponseEntity<Void> removeAccessRequest(final Long pAccessId) {
        LOG.error(FALLBACK_ERROR_MESSAGE);
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.client.IAccessesClient#updateAccessSettings(fr.cnes.regards.modules.
     * accessrights.domain.projects.AccessSettings)
     */
    @Override
    public ResponseEntity<Void> updateAccessSettings(final AccessSettings pAccessSettings) {
        LOG.error(FALLBACK_ERROR_MESSAGE);
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);

    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.client.IAccessesClient#getAccessSettings()
     */
    @Override
    public ResponseEntity<Resource<AccessSettings>> getAccessSettings() {
        LOG.error(FALLBACK_ERROR_MESSAGE);
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

}
