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
package fr.cnes.regards.modules.accessrights.instance.domain.passwordreset;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Dto class
 *
 * @author Xavier-Alexandre Brochard
 */
public class RequestResetPasswordDto {

    @Schema(description = "The URL of the app from where the request was issued.")
    private String originUrl;

    @Schema(description = "The URL to redirect the user to the reset password interface.")
    private String requestLink;

    public RequestResetPasswordDto(final String pOriginUrl, final String pRequestLink) {
        super();
        originUrl = pOriginUrl;
        requestLink = pRequestLink;
    }

    /**
     * @return the originUrl
     */
    public String getOriginUrl() {
        return originUrl;
    }

    /**
     * @param pOriginUrl the originUrl to set
     */
    public void setOriginUrl(final String pOriginUrl) {
        originUrl = pOriginUrl;
    }

    public String getRequestLink() {
        return requestLink;
    }

    public void setRequestLink(final String pRequestLink) {
        requestLink = pRequestLink;
    }

}
