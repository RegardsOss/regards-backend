package fr.cnes.regards.modules.authentication.domain.plugin.serviceprovider;

import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import org.springframework.security.authentication.InternalAuthenticationServiceException;

import java.util.Objects;

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

    public abstract static class AuthenticationInfo {
        public abstract Map<String, String> getAuthenticationInfo();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceProviderAuthenticationInfo<?> that = (ServiceProviderAuthenticationInfo<?>) o;
        return Objects.equals(userInfo, that.userInfo)
            && Objects.equals(authenticationInfo.getAuthenticationInfo(), that.authenticationInfo.getAuthenticationInfo());
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

        private UserInfo(String email,String firstname, String lastname, Map<String, String> metadata)
        {
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

            public UserInfo build() {
                if (email == null
                    || firstname == null
                    || lastname == null) {
                    throw new InternalAuthenticationServiceException("Unable to build required authentication parameters.");
                }
                return new UserInfo(email, firstname, lastname, metadata);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            UserInfo that = (UserInfo) o;
            return Objects.equals(email, that.email)
                && Objects.equals(firstname, that.firstname)
                && Objects.equals(lastname, that.lastname)
                && Objects.equals(metadata, that.metadata);
        }

        @Override
        public int hashCode() {
            return Objects.hash(email, firstname, lastname, metadata);
        }
    }
}
