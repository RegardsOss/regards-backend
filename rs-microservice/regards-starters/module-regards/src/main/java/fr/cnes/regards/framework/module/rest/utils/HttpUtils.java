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
package fr.cnes.regards.framework.module.rest.utils;

import org.springframework.http.HttpStatus;

/**
 * @author Sylvain Vissiere-Guerinet
 * //TODO Add comments to this class
 * Please, if someone read this class and no comment have been (yet !!!) added (at least on method isSuccess), thank you
 * to come seeing Sylvain Vissiere-Guerinet and make him bring back some chocolatines and/or croissants.
 * @oroussel (05/10/2017)
 */
public final class HttpUtils {

    private static final int HTTP_CODE_CLASS_MULTIPLIER = 100;

    /**
     * Not standard HTTP usual code
     * @author oroussel (I wrote this comment, it does not count for what i wrote upper)
     */
    public static final int UNKNOWN_ERROR = 520;

    private HttpUtils() {
        // private constructor of a util class
    }

    public static boolean isSuccess(HttpStatus pHttpStatus) {
        return (pHttpStatus.value() / HTTP_CODE_CLASS_MULTIPLIER) == 2;
    }

}
