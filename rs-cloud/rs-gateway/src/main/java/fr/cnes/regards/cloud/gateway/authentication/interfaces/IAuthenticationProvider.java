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

    UserStatus authenticate(String pName, String pPassword, String pScope);

    ProjectUser retreiveUser(String pName, String pScope);

}
