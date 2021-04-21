package fr.cnes.regards.framework.security.endpoint;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.security.core.GrantedAuthority;

import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.security.domain.SecurityException;

/**
 * Specification of {@link MethodAuthorizationService} for instance microservices. It allows to configure resources saying to the system that the actual tenant is instance.
 * @author Sylvain VISSIERE-GUERINET
 */
public class InstanceMethodAuthorizationService extends MethodAuthorizationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceMethodAuthorizationService.class);

    private static final String INSTANCE_TENANT = "instance";

    @Override
    public void onApplicationEvent(ApplicationReadyEvent pEvent) {
        try {
            manageTenant(INSTANCE_TENANT);
        } catch (SecurityException e) {
            LOGGER.error("Cannot initialize role authorities, no access set", e);
        }
    }

    /**
     * Override {@link MethodAuthorizationService#getAuthorities(String, ResourceMapping)} to specify instance tenant
     * @param pTenant forced to instance
     * @param pResourceMapping resource to retrieve
     */
    @Override
    public Optional<List<GrantedAuthority>> getAuthorities(String pTenant, ResourceMapping pResourceMapping) {
        return super.getAuthorities(INSTANCE_TENANT, pResourceMapping);
    }
}
