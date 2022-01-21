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
package fr.cnes.regards.modules.access.services.domain.user;

public class ProjectUserCreateDto extends ProjectUserBaseDto {

    private String roleName;
    private String password;
    private String originUrl;
    private String requestLink;


    public String getRoleName() {
        return roleName;
    }

    public ProjectUserCreateDto setRoleName(String roleName) {
        this.roleName = roleName;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public ProjectUserCreateDto setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getOriginUrl() {
        return originUrl;
    }

    public ProjectUserCreateDto setOriginUrl(String originUrl) {
        this.originUrl = originUrl;
        return this;
    }

    public String getRequestLink() {
        return requestLink;
    }

    public ProjectUserCreateDto setRequestLink(String requestLink) {
        this.requestLink = requestLink;
        return this;
    }

}
