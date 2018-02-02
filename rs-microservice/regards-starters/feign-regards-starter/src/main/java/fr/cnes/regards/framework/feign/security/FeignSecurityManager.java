/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.feign.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.security.core.context.SecurityContextHolder;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;

/**
 * Manage security information for system call. At the moment, this holder manages the system JWT.
 * @author Marc Sordi
 */
public class FeignSecurityManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(FeignSecurityManager.class);

    /**
     * Thread safe flag holder to activate system call<br/>
     *
     * If {@link Boolean#TRUE}, a system (i.e. internal) call will be done (with a system token)<br/>
     * Else the user token will be used either it is an usurpation or not (within security context holder).
     */
    private static final ThreadLocal<Boolean> systemFlagHolder = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * Thread safe flag holder to activate usurpated call<br/>
     *
     * If {@link Boolean#TRUE}, a system (i.e. internal) call will be done in behalf of a user<br/>
     * Else the user token within security context holder will be used.
     */
    private static final ThreadLocal<Boolean> usurpationFlagHolder = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * Thread safe user holder
     */
    private static final ThreadLocal<Pair<String, String>> usurpedUserHolder = new ThreadLocal<>();

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
     * @return a JWT according to thread context
     */
    public String getToken() {
        if (systemFlagHolder.get()) {
            return getSystemToken();
        } else if (usurpationFlagHolder.get()) {
            return getUsurpedToken();
        } else {
            return getUserToken();
        }
    }

    private String getUsurpedToken() {
        return jwtService.generateToken(runtimeTenantResolver.getTenant(), usurpedUserHolder.get().getFirst(),
                                        usurpedUserHolder.get().getSecond());
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
     * Generate a new system JWT for each call
     * @return a new system token for each call of each thread with its own tenant
     */
    private String getSystemToken() {

        String tenant = runtimeTenantResolver.getTenant();
        if (tenant == null) {
            // Allows request without tenant for instance endpoints
            tenant = "_NOTENANT_";
        }
        String role = RoleAuthority.getSysRole(appName);
        LOGGER.debug("Generating internal system JWT for application {}, tenant {} and role {} ", appName, tenant,
                     role);
        return jwtService.generateToken(tenant, appName, role);
    }

    /**
     * Enable system mode call. In this mode, all client requests will be done as a system call. It's equivalent to an
     * internal call that bypasses user authorizations. To disable this mode, call {@link #reset()}.<br/>
     * This method uses dynamic tenant resolution through {@link IRuntimeTenantResolver}. If tenant cannot be resolved,
     * a mock tenant is set that should reach only instance endpoints. <br/>
     * To set a specific tenant, use {@link IRuntimeTenantResolver#forceTenant(String)} before your first thread client
     * call.
     */
    public static void asSystem() {
        systemFlagHolder.set(Boolean.TRUE);
    }

    /**
     * Allows to usurp user identity if feign call needed into separate thread
     */
    public static void asUser(String user, String role) {
        usurpationFlagHolder.set(Boolean.TRUE);
        usurpedUserHolder.set(Pair.of(user, role));
    }

    /**
     * Disable system or user mode call enabled in {{@link #asSystem()}  or {{@link #asUser(String, String)}}
     */
    public static void reset() {
        systemFlagHolder.set(false);
        usurpationFlagHolder.set(false);
        usurpedUserHolder.set(null);
    }
}
