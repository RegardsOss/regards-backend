/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

package fr.cnes.regards.modules.search.service;

import org.springframework.http.HttpStatus;

/**
 * Enum containing causes of non availability of a file, dataobject, or dataset.
 *
 * @author tguillou
 */
public enum ExceptionCauseEnum {
    NOT_FOUND(HttpStatus.NOT_FOUND),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    TOO_MUCH_PRODUCTS(HttpStatus.BAD_REQUEST),
    TOO_MANY_FILES(HttpStatus.BAD_REQUEST),
    BAD_REQUEST(HttpStatus.BAD_REQUEST);

    private final HttpStatus httpStatus;

    /**
     * @param httpStatus corresponding httpStatus
     */
    ExceptionCauseEnum(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus getCorrespondingHttpsStatus() {
        return this.httpStatus;
    }
}
