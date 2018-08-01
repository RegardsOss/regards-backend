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
package fr.cnes.regards.framework.security.configurer;

/**
 * Exception which indicates an error occurred during the configuration of custom web security.
 *
 * @author Xavier-Alexandre Brochard
 */
public class CustomWebSecurityConfigurationException extends Exception {

    /**
     * Serial
     */
    private static final long serialVersionUID = 7027393628503256052L;

    /**
     * Constructs a new exception with the specified cause.
     */
    public CustomWebSecurityConfigurationException(final Throwable pCause) {
        super("An error occurred during the configuration of custom web security ", pCause);
    }

}
