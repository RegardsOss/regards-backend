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
package fr.cnes.regards.modules.accessrights.instance.domain;

import fr.cnes.regards.framework.jpa.utils.AbstractSearchParameters;

public class AccountSearchParameters implements AbstractSearchParameters<Account> {

    private String email;

    private String lastName;

    private String firstName;

    private String status;

    private String project;

    private String origin;

    public AccountSearchParameters() {
    }

    public AccountSearchParameters(String email,
                                   String lastName,
                                   String firstName,
                                   String status,
                                   String project,
                                   String origin) {
        this.email = email;
        this.lastName = lastName;
        this.firstName = firstName;
        this.status = status;
        this.project = project;
        this.origin = origin;
    }

    public String getEmail() {
        return email;
    }

    public AccountSearchParameters setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public AccountSearchParameters setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public AccountSearchParameters setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public AccountSearchParameters setStatus(String status) {
        this.status = status;
        return this;
    }

    public String getProject() {
        return project;
    }

    public AccountSearchParameters setProject(String project) {
        this.project = project;
        return this;
    }

    public String getOrigin() {
        return origin;
    }

    public AccountSearchParameters setOrigin(String origin) {
        this.origin = origin;
        return this;
    }

}
