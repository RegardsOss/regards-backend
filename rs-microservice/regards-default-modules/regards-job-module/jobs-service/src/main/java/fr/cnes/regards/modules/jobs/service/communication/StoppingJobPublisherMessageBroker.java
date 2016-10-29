/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.communication;

import org.springframework.security.core.context.SecurityContextHolder;

import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;

/**
 *
 */
public class StoppingJobPublisherMessageBroker {

    /**
     * @param pJobInfoId
     */
    public void send(final Long pJobInfoId) {
        // TODO Auto-generated method stub
        final JWTAuthentication authentication = (JWTAuthentication) SecurityContextHolder.getContext()
                .getAuthentication();
        String tenantName = authentication.getTenant();
    }

}
