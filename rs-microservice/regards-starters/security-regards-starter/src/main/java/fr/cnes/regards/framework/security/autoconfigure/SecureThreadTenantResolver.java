/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.autoconfigure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;

/**
 * Retrieve thread tenant according to security context
 *
 * @author Marc Sordi
 *
 */
public class SecureThreadTenantResolver implements IRuntimeTenantResolver {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SecureThreadTenantResolver.class);

    @Override
    public String getTenant() {
        JWTAuthentication authentication = ((JWTAuthentication) SecurityContextHolder.getContext().getAuthentication());
        if (authentication != null) {
            return authentication.getTenant();
        } else {
            return null;
        }
    }

    @Override
    public void forceTenant(String pTenant) {
        JWTAuthentication authentication = ((JWTAuthentication) SecurityContextHolder.getContext().getAuthentication());
        if (authentication != null) {
            authentication.setTenant(pTenant);
        } else {
            String errorMessage = "Cannot force tenant cause no authentication set";
            LOGGER.error(errorMessage);
            throw new UnsupportedOperationException(errorMessage);
        }

    }
}
