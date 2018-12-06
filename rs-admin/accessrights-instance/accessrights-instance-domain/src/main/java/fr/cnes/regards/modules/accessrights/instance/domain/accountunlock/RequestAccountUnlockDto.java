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
package fr.cnes.regards.modules.accessrights.instance.domain.accountunlock;

import javax.validation.constraints.NotBlank;

/**
 * Dto class wrapping data required for the {@link AccountsController#requestAccountUnlock} endpoint.
 *
 * @author Xavier-Alexandre Brochard
 */
public class RequestAccountUnlockDto {

    /**
     * The origin url
     */
    @NotBlank
    private String originUrl;

    /**
     * The request link
     */
    @NotBlank
    private String requestLink;

    /**
     * @param pOriginUrl
     * @param pRequestLink
     */
    public RequestAccountUnlockDto(final String pOriginUrl, final String pRequestLink) {
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
     * @param pOriginUrl
     *            the originUrl to set
     */
    public void setOriginUrl(final String pOriginUrl) {
        originUrl = pOriginUrl;
    }

    /**
     * @return the requestLink
     */
    public String getRequestLink() {
        return requestLink;
    }

    /**
     * @param pRequestLink
     *            the requestLink to set
     */
    public void setRequestLink(final String pRequestLink) {
        requestLink = pRequestLink;
    }

}
