/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.utils.endpoint;

import org.springframework.security.core.GrantedAuthority;

/**
 * REGARDS authority
 *
 * @author Marc Sordi
 * @author Sébastien Binda
 *
 */
public class RoleAuthority implements GrantedAuthority {

    /**
     * Virtual Instance administrator ROLE name
     */
    public static final String INSTANCE_ADMIN_VIRTUAL_ROLE = "INTSANCE_ADMIN";

    /**
     * Role prefix
     */
    private static final String ROLE_PREFIX = "ROLE_";

    /**
     * Serial number for serialization
     */
    private static final long serialVersionUID = 1L;

    /**
     * Prefix for all systems ROLES. Systems roles are internal roles to allow microservices to communicate witch each
     * other.
     */
    private static final String SYS_ROLE_PREFIX = "SYS_";

    /**
     * Role name prefixed with {@link #ROLE_PREFIX}
     */
    private String autority;

    /**
     *
     * Constructor
     *
     * @param pAuthority
     * @since 1.0-SNAPSHOT
     */
    public RoleAuthority(final String pAuthority) {
        if (!pAuthority.startsWith(ROLE_PREFIX)) {
            this.autority = ROLE_PREFIX + pAuthority;
        } else {
            this.autority = pAuthority;
        }
    }

    /**
     *
     * Retrieve the SYS ROLE for the current microservice. SYS ROLE is a specific role that permit access to all
     * administration endpoints.
     *
     * @param pMicroserviceName
     * @return SYS Role name
     * @since 1.0-SNAPSHOT
     */
    public static String getSysRole(final String pMicroserviceName) {
        return SYS_ROLE_PREFIX + pMicroserviceName;
    }

    /**
     *
     * Is the given role a system role ?
     *
     * @param pRoleName
     * @return [true|false]
     * @since 1.0-SNAPSHOT
     */
    public static boolean isSysRole(final String pRoleName) {
        boolean isSysRole = false;
        if (pRoleName.startsWith(ROLE_PREFIX + SYS_ROLE_PREFIX)) {
            isSysRole = true;
        }
        return isSysRole;
    }

    /**
     *
     * Is the given role the virtual instance admin role ?
     *
     * @param pRoleName
     * @return [true|false]
     * @since 1.0-SNAPSHOT
     */
    public static boolean isInstanceAdminRole(final String pRoleName) {
        boolean isInstanceAdminRole = false;
        if (pRoleName.equals(ROLE_PREFIX + INSTANCE_ADMIN_VIRTUAL_ROLE)) {
            isInstanceAdminRole = true;
        }
        return isInstanceAdminRole;
    }

    @Override
    public String getAuthority() {
        return autority;
    }

    @Override
    public boolean equals(final Object pObj) {
        if ((pObj != null) && (pObj instanceof RoleAuthority)) {
            return autority.equals(((RoleAuthority) pObj).getAuthority());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return autority.hashCode();
    }

}
