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
package fr.cnes.regards.modules.feature.dto.hateoas;

/**
 * Information section for requests pagination endpoints
 *
 * @author Sébastien Binda
 */
public class RequestsInfo {

    private Long nbErrors = 0L;

    public static RequestsInfo build(Long nbErrors) {
        RequestsInfo info = new RequestsInfo();
        info.setNbErrors(nbErrors);
        return info;
    }

    public Long getNbErrors() {
        return nbErrors;
    }

    public void setNbErrors(Long nbErrors) {
        this.nbErrors = nbErrors;
    }

    @Override
    public String toString() {
        return "RequestsInfo [" + (nbErrors != null ? "nbErrors=" + nbErrors : "") + "]";
    }

}
