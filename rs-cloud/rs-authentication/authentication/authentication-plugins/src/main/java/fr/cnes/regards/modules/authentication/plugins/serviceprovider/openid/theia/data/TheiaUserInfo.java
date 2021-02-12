package fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.theia.data;

import io.vavr.collection.Map;

import java.util.Objects;

public class TheiaUserInfo {

    private String email;

    private String firstName;

    private String lastName;

    private Map<String, String> metadata;

    public TheiaUserInfo(String email, String firstName, String lastName, Map<String, String> metadata) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.metadata = metadata;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TheiaUserInfo that = (TheiaUserInfo) o;
        return Objects.equals(email, that.email)
            && Objects.equals(firstName, that.firstName)
            && Objects.equals(lastName, that.lastName)
            && Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, firstName, lastName, metadata);
    }
}
