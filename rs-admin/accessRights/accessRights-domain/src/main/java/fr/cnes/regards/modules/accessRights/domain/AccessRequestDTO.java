package fr.cnes.regards.modules.accessRights.domain;

import java.util.List;

import fr.cnes.regards.modules.accessRights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessRights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.projects.Role;

public class AccessRequestDTO {

    private String email;

    private String firstName;

    private String lastName;

    private String login;

    private List<MetaData> metaData;

    private String password;

    private List<ResourcesAccess> permissions;

    private Role role;

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getLogin() {
        return login;
    }

    public List<MetaData> getMetaData() {
        return metaData;
    }

    public String getPassword() {
        return password;
    }

    public List<ResourcesAccess> getPermissions() {
        return permissions;
    }

    public Role getRole() {
        return role;
    }

    public void setEmail(final String pEmail) {
        email = pEmail;
    }

    public void setFirstName(final String pFirstName) {
        firstName = pFirstName;
    }

    public void setLastName(final String pLastName) {
        lastName = pLastName;
    }

    public void setLogin(final String pLogin) {
        login = pLogin;
    }

    public void setMetaData(final List<MetaData> pMetaData) {
        metaData = pMetaData;
    }

    public void setPassword(final String pPassword) {
        password = pPassword;
    }

    public void setPermissions(final List<ResourcesAccess> pPermissions) {
        permissions = pPermissions;
    }

    public void setRole(final Role pRole) {
        role = pRole;
    }
}
