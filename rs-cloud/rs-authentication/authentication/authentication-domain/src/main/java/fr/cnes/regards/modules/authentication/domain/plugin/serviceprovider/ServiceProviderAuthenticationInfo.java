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
package fr.cnes.regards.modules.authentication.domain.plugin.serviceprovider;

import java.util.Objects;

import fr.cnes.regards.modules.authentication.domain.exception.ServiceProviderPluginException;
import fr.cnes.regards.modules.authentication.domain.plugin.serviceprovider.ServiceProviderAuthenticationInfo.AuthenticationInfo;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;

public class ServiceProviderAuthenticationInfo<AuthenticationInfo extends ServiceProviderAuthenticationInfo.AuthenticationInfo> {

    private final UserInfo userInfo;

    private final AuthenticationInfo authenticationInfo;

    public ServiceProviderAuthenticationInfo(UserInfo userInfo, AuthenticationInfo authenticationInfo) {
        this.userInfo = userInfo;
        this.authenticationInfo = authenticationInfo;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public Map<String, String> getAuthenticationInfo() {
        return authenticationInfo.getAuthenticationInfo();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        ServiceProviderAuthenticationInfo<?> that = (ServiceProviderAuthenticationInfo<?>) o;
        return Objects.equals(userInfo, that.userInfo) && Objects
                .equals(authenticationInfo.getAuthenticationInfo(), that.authenticationInfo.getAuthenticationInfo());
    }

    @Override
    public int hashCode() {
        return Objects.hash(userInfo, authenticationInfo.getAuthenticationInfo());
    }

    public static class UserInfo {

        private final String email;

        private final String firstname;

        private final String lastname;

        private final Map<String, String> metadata;

        private UserInfo(String email, String firstname, String lastname, Map<String, String> metadata) {
            this.email = email;
            this.firstname = firstname;
            this.lastname = lastname;
            this.metadata = metadata;
        }

        public String getEmail() {
            return email;
        }

        public String getFirstname() {
            return firstname;
        }

        public String getLastname() {
            return lastname;
        }

        public Map<String, String> getMetadata() {
            return metadata;
        }

        public static class Builder {

            private String email;

            private String firstname;

            private String lastname;

            private Map<String, String> metadata = HashMap.empty();

            public Builder withEmail(String email) {
                this.email = email;
                return this;
            }

            public Builder withFirstname(String firstname) {
                this.firstname = firstname;
                return this;
            }

            public Builder withLastname(String lastname) {
                this.lastname = lastname;
                return this;
            }

            public Builder addMetadata(String key, String value) {
                this.metadata = this.metadata.put(key, value);
                return this;
            }

            public UserInfo build() throws ServiceProviderPluginException {
                if (email == null) {
                    throw new ServiceProviderPluginException("Unable to build required authentication parameters.");
                }
                return new UserInfo(email, firstname, lastname, metadata);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if ((o == null) || (getClass() != o.getClass())) {
                return false;
            }
            UserInfo that = (UserInfo) o;
            return Objects.equals(email, that.email) && Objects.equals(firstname, that.firstname)
                    && Objects.equals(lastname, that.lastname) && Objects.equals(metadata, that.metadata);
        }

        @Override
        public int hashCode() {
            return Objects.hash(email, firstname, lastname, metadata);
        }
    }

    public abstract static class AuthenticationInfo {

        public abstract Map<String, String> getAuthenticationInfo();
    }
}
