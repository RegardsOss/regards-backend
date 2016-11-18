package fr.cnes.regards.modules.accessrights.domain;

import java.util.List;
import java.util.Optional;

import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;

/**
 * Dto class wrapping data required for both account and project user creation.
 *
 * @author Xavier-Alexandre Brochard
 */
public class AccessRequestDTO {

    /**
     * The email
     */
    private String email;

    /**
     * The first name
     */
    private String firstName;

    /**
     * The last name
     */
    private String lastName;

    /**
     * The list of meta data
     */
    private List<MetaData> metaData;

    /**
     * The password
     */
    private String password;

    /**
     * The list of permissions
     */
    private List<ResourcesAccess> permissions;

    /**
     * The role name
     */
    private String roleName;

    /**
     * Default constructor
     */
    public AccessRequestDTO() {
        super();
    }

    /**
     * Create a dto from the passed account
     *
     * @param pAccount
     *            the account containing values
     */
    public AccessRequestDTO(final Account pAccount) {
        super();
        email = pAccount.getEmail();
        firstName = pAccount.getFirstName();
        lastName = pAccount.getLastName();
        password = pAccount.getPassword();
    }

    /**
     * Create a dto from the passed project user
     *
     * @param pProjectUser
     *            the project user containing values
     */
    public AccessRequestDTO(final ProjectUser pProjectUser) {
        email = pProjectUser.getEmail();
        metaData = pProjectUser.getMetaData();
        permissions = pProjectUser.getPermissions();
        final Optional<Role> role = Optional.ofNullable(pProjectUser.getRole());
        roleName = role.orElse(null).getName();
    }

    /**
     * @return an account with values from the dto
     */
    public Account toAccount() {
        return new Account(email, firstName, lastName, password);
    }

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param pEmail
     *            the email to set
     */
    public void setEmail(final String pEmail) {
        email = pEmail;
    }

    /**
     * @return the firstName
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * @param pFirstName
     *            the firstName to set
     */
    public void setFirstName(final String pFirstName) {
        firstName = pFirstName;
    }

    /**
     * @return the lastName
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * @param pLastName
     *            the lastName to set
     */
    public void setLastName(final String pLastName) {
        lastName = pLastName;
    }

    /**
     * @return the metaData
     */
    public List<MetaData> getMetaData() {
        return metaData;
    }

    /**
     * @param pMetaData
     *            the metaData to set
     */
    public void setMetaData(final List<MetaData> pMetaData) {
        metaData = pMetaData;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param pPassword
     *            the password to set
     */
    public void setPassword(final String pPassword) {
        password = pPassword;
    }

    /**
     * @return the permissions
     */
    public List<ResourcesAccess> getPermissions() {
        return permissions;
    }

    /**
     * @param pPermissions
     *            the permissions to set
     */
    public void setPermissions(final List<ResourcesAccess> pPermissions) {
        permissions = pPermissions;
    }

    /**
     * @return the roleName
     */
    public String getRoleName() {
        return roleName;
    }

    /**
     * @param pRoleName
     *            the roleName to set
     */
    public void setRoleName(final String pRoleName) {
        roleName = pRoleName;
    }

}
