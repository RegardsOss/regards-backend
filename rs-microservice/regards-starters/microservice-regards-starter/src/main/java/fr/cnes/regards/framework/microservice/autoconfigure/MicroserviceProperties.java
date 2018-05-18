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
package fr.cnes.regards.framework.microservice.autoconfigure;

import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;

import fr.cnes.regards.framework.microservice.annotation.MicroserviceInfo;

/**
 * @author msordi
 *
 *
 * @deprecated could replace {@link MicroserviceInfo} / FIXME
 */
@Deprecated
@ConfigurationProperties(prefix = "regards.microservice")
public class MicroserviceProperties {

    /**
     *
     * Name of the microservice
     */
    @NotNull
    private String name;

    /**
     *
     * Version of the microservice
     */
    @NotNull
    private String version;

    /**
     *
     * Dependencies of microservice
     */
    private String[] dependencies;

    public String getName() {
        return name;
    }

    public void setName(String pName) {
        name = pName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String pVersion) {
        version = pVersion;
    }

    public String[] getDependencies() {
        return dependencies;
    }

    public void setDependencies(String[] pDependencies) {
        dependencies = pDependencies;
    }

}
