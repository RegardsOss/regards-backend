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
package fr.cnes.regards.modules.feature.dto.hateoas;

/**
 * DTO Pojo for requests treatments responses. Indicates if all requested requests has been handled or not.
 *
 * @author Sébastien Binda
 *
 */
public class RequestHandledResponse {

    private long totalRequested;

    private long totalHandled;

    private String message;

    public static RequestHandledResponse build(long totalRequested, long totalHandled, String message) {
        RequestHandledResponse resp = new RequestHandledResponse();
        resp.setTotalHandled(totalHandled);
        resp.setTotalHandled(totalRequested);
        resp.setMessage(message);
        return resp;
    }

    public long getTotalRequested() {
        return totalRequested;
    }

    public void setTotalRequested(long totalRequested) {
        this.totalRequested = totalRequested;
    }

    public long getTotalHandled() {
        return totalHandled;
    }

    public void setTotalHandled(long totalHandled) {
        this.totalHandled = totalHandled;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
