package fr.cnes.regards.framework.security.endpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import fr.cnes.regards.framework.security.domain.SecurityException;

/**
 * Specification of {@link MethodAuthorizationService} for instance microservices. It allows to configure resources saying to the system that the actual tenant is instance.
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public class InstanceMethodAuthorizationService extends MethodAuthorizationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceMethodAuthorizationService.class);

    @Override
    public void onApplicationEvent(ApplicationReadyEvent pEvent) {
        try {
            manageTenant("INSTANCE");
        } catch (SecurityException e) {
            LOGGER.error("Cannot initialize role authorities, no access set", e);
        }
    }
}
