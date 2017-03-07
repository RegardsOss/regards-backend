/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.feign.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;

/**
 *
 * Manage security information for system call. At the moment, this holder manages the system JWT.
 *
 * @author Marc Sordi
 *
 */
public class FeignSecurityManager {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(FeignSecurityManager.class);

    /**
     * Thread safe flag holder to activate system call<br/>
     *
     * If {@link Boolean#TRUE}, a system (i.e. internal) call will be done (with a system token)<br/>
     * Else the user token within security context holder will be used.
     */
    private static final ThreadLocal<Boolean> systemFlagHolder = new ThreadLocal<>();

    /**
     * Instance of JWT for internal call
     */
    private String sysJwt;

    /**
     * Application name
     */
    @Value("${spring.application.name}")
    private String appName;

    /**
     * Allows to generate an internal JWT
     */
    @Autowired
    private JWTService jwtService;

    /**
     * Allows to retrieve current tenant
     */
    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * If system flag is enabled, this method return a system (i.e. internal) token else propagate the
     * {@link SecurityContextHolder} user token.
     *
     * @return a JWT according to thread context
     */
    public String getToken() {
        Boolean asSysCall = systemFlagHolder.get();
        if ((asSysCall != null) && asSysCall) {
            return getSystemToken();
        } else {
            return getUserToken();
        }
    }

    private String getUserToken() {
        final JWTAuthentication authentication = (JWTAuthentication) SecurityContextHolder.getContext()
                .getAuthentication();
        if (authentication != null) {
            return authentication.getJwt();
        } else {
            String message = "No authentication found from security context.";
            LOGGER.error(message);
            throw new UnsupportedOperationException(message);
        }
    }

    /**
     * FIXME : attention à la date d'expiration. Aucune date d'expiration ne doit être utilisée lors de la génération du
     * token.<br/>
     * Generate a system JWT once and return it to use for system calls.
     *
     * @return a system token.
     */
    private String getSystemToken() {
        if (sysJwt == null) {
            String tenant = runtimeTenantResolver.getTenant();
            String role = RoleAuthority.getSysRole(appName);
            LOGGER.info("Generating internal system JWT for application {}, tenant {} and with role {} ", appName,
                        tenant, role);
            sysJwt = jwtService.generateToken(tenant, appName, role);
        }
        return sysJwt;
    }

    /**
     * Enable system mode call. Following client requests will be done as a system call. It's equivalent to an internal
     * call that bypasses user authorizations. To disable this mode, call {@link #reset()}.
     */
    public static void asSystem() {
        systemFlagHolder.set(Boolean.TRUE);
    }

    /**
     * Disable system mode call enabled in {{@link #asSystem()}
     */
    public static void reset() {
        systemFlagHolder.remove();
    }
}
