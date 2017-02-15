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
public class SecureRuntimeTenantResolver implements IRuntimeTenantResolver {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SecureRuntimeTenantResolver.class);

    // Thread safe tenant holder for forced tenant
    private static final ThreadLocal<String> tenantHolder = new ThreadLocal<>();

    @Override
    public String getTenant() {
        // Try to get tenant from tenant holder
        String tenant = tenantHolder.get();
        if (tenant != null) {
            return tenant;
        }
        // Try to get tenant from JWT
        JWTAuthentication authentication = (JWTAuthentication) SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return authentication.getTenant();
        } else {
            return null;
        }
    }

    @Override
    public void forceTenant(String pTenant) {
        LOGGER.info("Forcing tenant to : {}", pTenant);
        tenantHolder.set(pTenant);
    }

    public void clearTenant() {
        LOGGER.info("Clearing tenant");
        tenantHolder.remove();
    }
}
