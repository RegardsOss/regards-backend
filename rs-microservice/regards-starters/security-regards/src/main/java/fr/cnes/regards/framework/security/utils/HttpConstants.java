/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.security.utils;

/**
 * Security constants
 * @author Marc Sordi
 * @author Sébastien Binda
 */
public final class HttpConstants {

    /**
     * Authorization header
     */
    public static final String AUTHORIZATION = "Authorization";

    /**
     * Content-type
     */
    public static final String CONTENT_TYPE = "Content-type";

    /**
     * Accept type
     */
    public static final String ACCEPT = "Accept";

    /**
     * Authorization header scheme
     */
    public static final String BEARER = "Bearer";

    /**
     * Scope parameter to read in Header or request query parameters
     */
    public static final String SCOPE = "scope";

    private HttpConstants() {
    }
}
