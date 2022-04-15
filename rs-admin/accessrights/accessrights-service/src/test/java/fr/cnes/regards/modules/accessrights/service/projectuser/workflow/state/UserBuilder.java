/*
 * Copyright 2017-20XX CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.service.projectuser.workflow.state;

import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;

/**
 * @author Thomas Fache
 **/
public class UserBuilder {

    private final ProjectUser user;

    private UserBuilder() {
        user = new ProjectUser();
        user.setId(1L);
        user.setEmail("toto@mail.com");
    }

    public static UserBuilder aUser() {
        return new UserBuilder();
    }

    public ProjectUser build() {
        return user;
    }

    public UserBuilder waitingAccess() {
        user.setStatus(UserStatus.WAITING_ACCESS);
        return this;
    }

    public UserBuilder unauthorized() {
        user.setStatus(UserStatus.ACCESS_DENIED);
        return this;
    }

    public UserBuilder authorized() {
        user.setStatus(UserStatus.ACCESS_GRANTED);
        return this;
    }

    public UserBuilder inactive() {
        user.setStatus(UserStatus.ACCESS_INACTIVE);
        return this;
    }

    public UserBuilder waitingActivation() {
        user.setStatus(UserStatus.WAITING_ACCOUNT_ACTIVE);
        return this;
    }

    public UserBuilder waitingMailVerification() {
        user.setStatus(UserStatus.WAITING_EMAIL_VERIFICATION);
        return this;
    }

}
