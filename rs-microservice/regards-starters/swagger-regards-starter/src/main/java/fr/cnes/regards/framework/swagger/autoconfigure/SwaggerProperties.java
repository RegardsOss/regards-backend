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
package fr.cnes.regards.framework.swagger.autoconfigure;

import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Swagger properties
 *
 * @author msordi
 *
 */
@ConfigurationProperties(prefix = "regards.swagger")
public class SwaggerProperties {

    @NotNull
    private String apiName;

    @NotNull
    private String apiTitle;

    @NotNull
    private String apiDescription;

    @NotNull
    private String apiLicense;

    @NotNull
    private String apiVersion;

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String pApiName) {
        apiName = pApiName;
    }

    public String getApiTitle() {
        return apiTitle;
    }

    public void setApiTitle(String pApiTitle) {
        apiTitle = pApiTitle;
    }

    public String getApiDescription() {
        return apiDescription;
    }

    public void setApiDescription(String pApiDescription) {
        apiDescription = pApiDescription;
    }

    public String getApiLicense() {
        return apiLicense;
    }

    public void setApiLicense(String pApiLicense) {
        apiLicense = pApiLicense;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String pApiVersion) {
        apiVersion = pApiVersion;
    }
}
