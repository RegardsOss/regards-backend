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
package fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.request;

import feign.form.FormProperty;

import java.util.Objects;

public class OpenIdTokenRequest {

    @FormProperty("grant_type")
    private String grantType = "authorization_code";

    private String code;

    @FormProperty("redirect_uri")
    private String redirectUri;

    public OpenIdTokenRequest(String code, String redirectUri) {
        this.code = code;
        this.redirectUri = redirectUri;
    }

    public String getGrantType() {
        return grantType;
    }

    public String getCode() {
        return code;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OpenIdTokenRequest that = (OpenIdTokenRequest) o;
        return Objects.equals(grantType, that.grantType) && Objects.equals(code, that.code) && Objects.equals(
            redirectUri,
            that.redirectUri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(grantType, code, redirectUri);
    }
}
