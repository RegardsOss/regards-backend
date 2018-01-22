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
package fr.cnes.regards.modules.authentication.plugins.regards;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.authentication.plugins.IAuthenticationPlugin;
import fr.cnes.regards.modules.authentication.plugins.domain.AuthenticationPluginResponse;

/**
 *
 * Class SimpleAuthentication
 *
 * Regards internal authentication plugin
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Plugin(author = "CSSI", description = "Regards internal authentication plugin",
        id = "RegardsInternalAuthenticationPlugin", version = "1.0", contact = "regards@c-s.fr", licence = "GPL V3",
        owner = "CNES", url = "www.cnes.fr")
public class RegardsInternalAuthenticationPlugin implements IAuthenticationPlugin {

    /**
     * rs-admin microservice client for accounts
     */
    @Autowired
    private IAccountsClient accountsClient;

    @Override
    public AuthenticationPluginResponse authenticate(final String pEmail, final String pPassword, final String pScope) {

        Boolean accessGranted = false;
        String errorMessage = null;

        // Validate password as system
        FeignSecurityManager.asSystem();
        final ResponseEntity<Boolean> validateResponse = accountsClient.validatePassword(pEmail, pPassword);

        if (validateResponse.getStatusCode().equals(HttpStatus.OK)) {
            if (validateResponse.getBody()) {
                accessGranted = validateResponse.getBody();
            } else {
                errorMessage = String
                        .format("[REMOTE ADMINISTRATION] - validatePassword - Accound %s doesn't exists", pEmail);
            }
        } else {
            errorMessage = String
                    .format("[REMOTE ADMINISTRATION] - validatePassword - Accound %s doesn't exists", pEmail);
        }

        return new AuthenticationPluginResponse(accessGranted, pEmail, errorMessage);

    }

}
