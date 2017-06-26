/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.utils.endpoint;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;

import fr.cnes.regards.framework.security.role.DefaultRole;

/**
 * REGARDS authority
 *
 * @author Marc Sordi
 * @author Sébastien Binda
 * @author Sylvain Vissiere-Guerinet
 * @since 1.0-SNAPSHOT
 *
 */
public class RoleAuthority implements GrantedAuthority {

    /**
     * Role prefix
     */
    private static final String ROLE_PREFIX = "ROLE_";

    /**
     * Serial number for serialization
     */
    private static final long serialVersionUID = -8843987659284743509L;

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
     * List of authorized id addresses for the current rrole
     */
    private List<String> authorizedIpAdresses = new ArrayList<>();

    /**
     *
     * Constructor
     *
     * @param pRoleName
     *            The role name
     * @since 1.0-SNAPSHOT
     */
    public RoleAuthority(final String pRoleName) {
        if (!pRoleName.startsWith(ROLE_PREFIX)) {
            autority = getRoleAuthority(pRoleName);
        } else {
            autority = pRoleName;
        }
    }

    /**
     *
     * Remove Authority ROLE_ prefix to get real role name
     *
     * @param pRoleAuthorityName
     *            Authority role name with ROLE_PREFIX
     * @return role name
     * @since 1.0-SNAPSHOT
     */
    public static String getRoleName(final String pRoleAuthorityName) {
        String roleName = pRoleAuthorityName;
        if ((pRoleAuthorityName != null) && pRoleAuthorityName.startsWith(ROLE_PREFIX)) {
            roleName = roleName.substring(ROLE_PREFIX.length());
        }
        return roleName;
    }

    /**
     *
     * Add Authority PREFIX to given Role name if necessary
     *
     * @param pRoleName
     *            The role name
     * @return RoleAuthority
     * @since 1.0-SNAPSHOT
     */
    public static String getRoleAuthority(final String pRoleName) {
        if (pRoleName.startsWith(ROLE_PREFIX)) {
            return pRoleName;
        }
        return ROLE_PREFIX + pRoleName;
    }

    /**
     *
     * Retrieve the SYS ROLE for the current microservice. SYS ROLE is a specific role that permit access to all
     * administration endpoints.
     *
     * @param pMicroserviceName
     *            the current microservice name
     * @return SYS Role name
     * @since 1.0-SNAPSHOT
     */
    public static String getSysRole(final String pMicroserviceName) {
        return ROLE_PREFIX + SYS_ROLE_PREFIX + pMicroserviceName;
    }

    /**
     *
     * Is the given role a system role ?
     *
     * @param pRoleName
     *            The role name
     * @return [true|false]
     * @since 1.0-SNAPSHOT
     */
    public static boolean isSysRole(final String pRoleName) {
        boolean isSysRole = false;
        if (getRoleAuthority(pRoleName).startsWith(ROLE_PREFIX + SYS_ROLE_PREFIX)) {
            isSysRole = true;
        }
        return isSysRole;
    }

    /**
     *
     * Is the given role the virtual instance admin role ?
     *
     * @param pRoleName
     *            The role name
     * @return [true|false]
     * @since 1.0-SNAPSHOT
     */
    public static boolean isInstanceAdminRole(final String pRoleName) {
        boolean isInstanceAdminRole = false;
        if (getRoleAuthority(pRoleName).equals(getRoleAuthority(DefaultRole.INSTANCE_ADMIN.toString()))) {
            isInstanceAdminRole = true;
        }
        return isInstanceAdminRole;
    }

    /**
     *
     * Is the given role the virtual project admin role ?
     *
     * @param pRoleName
     *            The role name
     * @return [true|false]
     * @since 1.0-SNAPSHOT
     */
    public static boolean isProjectAdminRole(final String pRoleName) {
        return getRoleAuthority(pRoleName).equals(getRoleAuthority(DefaultRole.PROJECT_ADMIN.toString()));
    }

    @Override
    public String getAuthority() {
        return autority;
    }

    public String getRoleName() {
        return getRoleName(autority);
    }

    public List<String> getAuthorizedIpAdresses() {
        return authorizedIpAdresses;
    }

    public void setAuthorizedIpAdresses(final List<String> pAuthorizedIpAdresses) {
        authorizedIpAdresses = pAuthorizedIpAdresses;
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
