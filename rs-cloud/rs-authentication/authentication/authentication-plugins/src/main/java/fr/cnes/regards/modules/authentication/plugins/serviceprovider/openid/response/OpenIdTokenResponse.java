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
package fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.response;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

public class OpenIdTokenResponse {

    @SerializedName("token_type")
    private final String tokenType;

    @SerializedName("expires_in")
    private final Long expiresIn;

    @SerializedName("refresh_token")
    private final String refreshToken;

    @SerializedName("access_token")
    private final String accessToken;

    public OpenIdTokenResponse(String tokenType, Long expiresIn, String refreshToken, String accessToken) {
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.refreshToken = refreshToken;
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OpenIdTokenResponse that = (OpenIdTokenResponse) o;
        return Objects.equals(tokenType, that.tokenType)
            && Objects.equals(expiresIn, that.expiresIn)
            && Objects.equals(refreshToken, that.refreshToken)
            && Objects.equals(accessToken, that.accessToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tokenType, expiresIn, refreshToken, accessToken);
    }
}
