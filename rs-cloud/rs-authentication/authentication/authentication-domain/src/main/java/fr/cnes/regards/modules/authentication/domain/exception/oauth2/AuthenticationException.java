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
package fr.cnes.regards.modules.authentication.domain.exception.oauth2;

import fr.cnes.regards.modules.authentication.domain.data.AuthenticationStatus;

import java.util.Map;
import java.util.TreeMap;

/**
 * Class AuthenticationException
 *
 * @author Sébastien Binda
 */
public class AuthenticationException extends RuntimeException {

    private Map<String, String> additionalInformation = null;

    /**
     * Error additional key into RequestEntity response
     */
    private static final String ERROR_TYPE_KEY = "error";

    public AuthenticationException(String msg, AuthenticationStatus status) {
        super(msg);
        addAdditionalInformation(ERROR_TYPE_KEY, status.toString());
    }

    /**
     * Get any additional information associated with this error.
     */
    public Map<String, String> getAdditionalInformation() {
        return this.additionalInformation;
    }

    /**
     * Add some additional information
     */
    public void addAdditionalInformation(String key, String value) {
        if (this.additionalInformation == null) {
            this.additionalInformation = new TreeMap<String, String>();
        }

        this.additionalInformation.put(key, value);
    }

    public String getDetailMessage() {
        return getMessage();
    }
}
