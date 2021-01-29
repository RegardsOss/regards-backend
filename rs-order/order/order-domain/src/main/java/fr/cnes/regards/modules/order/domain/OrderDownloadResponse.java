/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.order.domain;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 *
 * @author Sébastien Binda
 *
 */
public class OrderDownloadResponse {

    private HttpStatus status;

    private InputStreamResource is;

    public static OrderDownloadResponse build(HttpStatus status, InputStreamResource is) {
        OrderDownloadResponse resp = new OrderDownloadResponse();
        resp.status = status;
        resp.is = is;
        return resp;
    }

    public ResponseEntity<InputStreamResource> toResponseEntity(HttpHeaders headers) {
        if (HttpStatus.OK.equals(status)) {
            return new ResponseEntity<InputStreamResource>(this.is, headers, status);
        } else {
            return new ResponseEntity<>(status);
        }
    }

}
