/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.authentication.plugins.identityprovider.regards;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.utils.ResponseEntityUtils;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.authentication.domain.plugin.AuthenticationPluginResponse;
import fr.cnes.regards.modules.authentication.domain.plugin.IAuthenticationPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

/**
 * Class SimpleAuthentication
 * <p>
 * Regards internal authentication plugin
 *
 * @author Sébastien Binda
 */
@Plugin(author = "CSSI", description = "Regards internal authentication plugin",
    id = "RegardsInternalAuthenticationPlugin", version = "1.0", contact = "regards@c-s.fr", license = "GPLv3",
    owner = "CNES", url = "www.cnes.fr")
public class RegardsInternalAuthenticationPlugin implements IAuthenticationPlugin {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(RegardsInternalAuthenticationPlugin.class);

    /**
     * rs-admin microservice client for accounts
     */
    @Autowired
    private IAccountsClient accountsClient;

    @Override
    public AuthenticationPluginResponse authenticate(final String pEmail, final String pPassword, final String pScope) {

        Boolean accessGranted = false;
        String errorMessage = null;

        ResponseEntity<Boolean> validateResponse;
        // Validate password as system
        try {
            FeignSecurityManager.asSystem();
            ResponseEntity<EntityModel<Account>> response = accountsClient.retrieveAccounByEmail(pEmail);
            Account account = ResponseEntityUtils.extractContentOrNull(response);
            if (account != null && !account.isExternal()) {
                validateResponse = accountsClient.validatePassword(pEmail, pPassword);
            } else {
                String message = String.format(
                    "Account %s is not allowed to authenticate (External account authentication)",
                    pEmail);
                LOG.error(message);
                return new AuthenticationPluginResponse(false, pEmail, message);
            }
        } catch (HttpServerErrorException |

                 HttpClientErrorException e) {
            LOG.error(e.getMessage(), e);
            return new AuthenticationPluginResponse(false, pEmail, "Invalid password");
        } finally {
            FeignSecurityManager.reset();
        }

        if (validateResponse.getStatusCode().equals(HttpStatus.OK)) {
            if (validateResponse.getBody()) {
                accessGranted = validateResponse.getBody();
            } else {
                // This probably means that the password is not correct.
                // As we are afraid from hackers,
                // we do not want the end user to know that the account exists so we are lying.
                errorMessage = String.format("[REMOTE ADMINISTRATION] - validatePassword - Accound %s doesn't exists",
                                             pEmail);
            }
        } else {
            errorMessage = String.format("[REMOTE ADMINISTRATION] - validatePassword - Accound %s doesn't exists",
                                         pEmail);
        }

        return new AuthenticationPluginResponse(accessGranted, pEmail, errorMessage);

    }

}
