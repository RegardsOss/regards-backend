/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.authentication.domain.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Mimic Spring Authentication object
 *
 * NOTE : Both SerializedName & JsonProperty annotations are mandatory her for both Jackson and gson
 * serialization/deserialization.
 * Gson is used by regards microservices.
 * Jackson is used in rs-gateway by the ExternalAuthenticationVerifier coded with reactive WebFlux.
 */
public class Authentication {

    /**
     * It's a constant but the aim is to be serialized in json => final with a getter
     */
    @SerializedName("token_type")
    @JsonProperty("token_type")
    private final String tokenType = "bearer";

    /**
     * project, tenant and scope is same thing
     */
    @SerializedName("tenant")
    @JsonProperty("tenant")
    private String project;

    private String scope;

    private String role;

    /**
     * sub(ject) and email is same thing
     */
    private String sub;

    private String email;

    @SerializedName("service_provider_name")
    @JsonProperty("service_provider_name")
    private String serviceProviderName;

    @SerializedName("access_token")
    @JsonProperty("access_token")
    private String accessToken;

    @SerializedName("expires_in")
    @JsonProperty("expires_in")
    private Long expiresIn;

    public Authentication() {
    }

    public Authentication(String tenant,
                          String email,
                          String role,
                          String serviceProviderName,
                          String token,
                          OffsetDateTime expirationDate) {
        this.project = tenant;
        this.scope = tenant;
        this.role = role;
        this.sub = email;
        this.email = email;
        this.serviceProviderName = serviceProviderName;
        this.accessToken = token;
        this.expiresIn = expirationDate.toEpochSecond() - OffsetDateTime.now().toEpochSecond();
    }

    public String getProject() {
        return project;
    }

    public String getScope() {
        return scope;
    }

    public String getRole() {
        return role;
    }

    public String getSub() {
        return sub;
    }

    public String getServiceProviderName() {
        return serviceProviderName;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public String getTenant() {
        return scope;
    }

    public String getEmail() {
        return sub;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Authentication that = (Authentication) o;
        return Objects.equals(project, that.project)
               && Objects.equals(scope, that.scope)
               && Objects.equals(role, that.role)
               && Objects.equals(sub, that.sub)
               && Objects.equals(serviceProviderName, that.serviceProviderName)
               && Objects.equals(accessToken, that.accessToken)
               && Objects.equals(expiresIn, that.expiresIn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(project, scope, role, sub, serviceProviderName, accessToken, tokenType, expiresIn);
    }
}
