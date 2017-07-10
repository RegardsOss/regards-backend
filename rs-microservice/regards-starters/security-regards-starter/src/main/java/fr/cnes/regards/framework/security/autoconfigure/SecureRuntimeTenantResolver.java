/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.autoconfigure;

import org.apache.log4j.MDC;
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

    /**
     * Name of the static and fixed name of instance virtual tenant.
     */
    private final String instanceTenantName;

    public SecureRuntimeTenantResolver(String pInstanceTenantName) {
        super();
        this.instanceTenantName = pInstanceTenantName;
    }

    @Override
    public String getTenant() {
        // Try to get tenant from tenant holder
        final String tenant = tenantHolder.get();
        if (tenant != null) {
            return tenant;
        }
        // Try to get tenant from JWT
        final JWTAuthentication authentication = (JWTAuthentication) SecurityContextHolder.getContext()
                .getAuthentication();
        if (authentication != null) {
            return authentication.getTenant();
        } else {
            return null;
        }
    }

    @Override
    public void forceTenant(final String pTenant) {
        // when we force the tenant for the application, we set it for logging too
        MDC.put("tenant", pTenant);
        tenantHolder.set(pTenant);
    }

    @Override
    public void clearTenant() {
        LOGGER.debug("Clearing tenant");
        tenantHolder.remove();
        JWTAuthentication authentication = (JWTAuthentication) SecurityContextHolder.getContext().getAuthentication();
        // when we clear the tenant, system will act by getting it from the security context holder, so we do the same for logging
        if(authentication!=null) {
            MDC.put("tenant", authentication.getTenant());
        } else {
            MDC.put("tenant", null);
        }
    }

    @Override
    public Boolean isInstance() {
        return instanceTenantName.equals(getTenant());
    }
}
