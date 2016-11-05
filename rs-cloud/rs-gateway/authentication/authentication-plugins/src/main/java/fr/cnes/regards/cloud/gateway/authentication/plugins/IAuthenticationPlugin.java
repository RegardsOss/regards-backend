/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.plugins;

import fr.cnes.regards.framework.security.utils.jwt.UserDetails;

/**
 *
 * Class IAuthenticationProvider
 *
 * Authentication Provider interface. TODO : Should be replace with the REGARDS Plugins system.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public interface IAuthenticationPlugin {

    /**
     *
     * Check if the couple pName/pPassowrd is valid for the given project pScope
     *
     * @param pName
     *            user login
     * @param pPassword
     *            user password
     * @param pScope
     *            user project
     * @return Authentication status UserStatus
     * @since 1.0-SNAPSHOT
     */
    AuthenticateStatus authenticate(String pName, String pPassword, String pScope);

    /**
     *
     * Retrieve user role
     *
     * @param pName
     *            user login
     * @param pScope
     *            user project
     * @return Project User role
     * @since 1.0-SNAPSHOT
     */
    UserDetails retreiveUserDetails(String pName, String pScope) throws UserNotFoundException;

}
