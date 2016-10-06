/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.interfaces;

import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.UserStatus;

/**
 *
 * Class IAuthenticationProvider
 *
 * Authentication Provider interface. TODO : Should be replace with the REGARDS Plugins system.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public interface IAuthenticationProvider {

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
    UserStatus authenticate(String pName, String pPassword, String pScope);

    /**
     *
     * Retrieve user accounts informations.
     *
     * @param pName
     *            user login
     * @param pScope
     *            user project
     * @return ProjectUser
     * @since 1.0-SNAPSHOT
     */
    ProjectUser retreiveUser(String pName, String pScope);

}
