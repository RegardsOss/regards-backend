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
package fr.cnes.regards.cloud.gateway.filters;

/**
 * @author Iliana Ghazali
 **/
public final class FilterConstants {

    public static final String COMMA_WHITESPACE = ", ";

    /**
     * Header name
     */
    public static final String CORRELATION_ID = "correlation-id";

    /**
     * Header name
     */
    public static final String X_FORWARDED_FOR = "X-Forwarded-For";

    /**
     * Header name
     */
    public static final String AUTHORIZATION = "Authorization";

    public static final String BEARER = "Bearer";

    public static final String TOKEN = "token";

    private FilterConstants() {
        throw new IllegalStateException("Utility class");
    }
}
