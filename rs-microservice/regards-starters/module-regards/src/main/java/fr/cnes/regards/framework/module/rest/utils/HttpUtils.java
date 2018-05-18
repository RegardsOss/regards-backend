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
package fr.cnes.regards.framework.module.rest.utils;

import org.springframework.http.HttpStatus;

/**
 * @author Sylvain Vissiere-Guerinet
 */
public final class HttpUtils {

    /**
     * Http code class multiplier
     */
    private static final int HTTP_CODE_CLASS_MULTIPLIER = 100;

    /**
     * Not standard HTTP usual code
     */
    public static final int UNKNOWN_ERROR = 520;

    private HttpUtils() {
        // private constructor of a util class
    }

    /**
     * check {https://tools.ietf.org/html/rfc7231#section-6} for information
     */
    public static boolean isSuccess(HttpStatus pHttpStatus) {
        return (pHttpStatus.value() / HTTP_CODE_CLASS_MULTIPLIER) == 2;
    }

}
