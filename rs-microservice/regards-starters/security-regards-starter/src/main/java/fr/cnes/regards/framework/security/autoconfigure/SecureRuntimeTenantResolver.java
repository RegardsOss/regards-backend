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
     * serialVersionUID field.
     *
     * @author CS
     * @since 1.0-SNAPSHOT
     */
    private static final long serialVersionUID = 1L;

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

    /**
     *
     * Constructeur
     *
     * @param pInstanceTenantName
     * @since 1.0-SNAPSHOT
     */
    public SecureRuntimeTenantResolver(final String pInstanceTenantName) {
        super();
        instanceTenantName = pInstanceTenantName;
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
        tenantHolder.set(pTenant);
    }

    public void clearTenant() {
        LOGGER.info("Clearing tenant");
        tenantHolder.remove();
    }

    @Override
    public Boolean isInstance() {
        return instanceTenantName.equals(getTenant());
    }
}
