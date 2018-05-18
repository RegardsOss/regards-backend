/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.authentication.exception;

import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;

import fr.cnes.regards.framework.authentication.internal.AuthenticationStatus;

/**
 *
 * Class AuthenticationException
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class AuthenticationException extends OAuth2Exception {

    /**
     * serialVersionUID field.
     *
     * @author CS
     * @since 1.0-SNAPSHOT
     */
    private static final long serialVersionUID = 1L;

    /**
     * Error additional key into RequestEntity response
     */
    private static final String ERROR_TYPE_KEY = "error";

    public AuthenticationException(final String pMsg, final AuthenticationStatus pStatus) {
        super(pMsg);
        this.addAdditionalInformation(ERROR_TYPE_KEY, pStatus.toString());
    }

}
