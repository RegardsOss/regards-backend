/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain.projects;

import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.validation.Valid;

import org.hibernate.validator.constraints.NotBlank;

import fr.cnes.regards.framework.jpa.IIdentifiable;

/**
 * Models a user's role.
 *
 * @author Xaver-Alexandre Brochard
 */
@Entity(name = "T_ROLE")
@SequenceGenerator(name = "roleSequence", initialValue = 1, sequenceName = "SEQ_ROLE")
public class Role implements IIdentifiable<Long> {

    /**
     * Role indentifier
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "roleSequence")
    @Column(name = "id")
    private Long id;

    /**
     * Role name
     */
    @NotBlank
    @Column(name = "name")
    private String name;

    /**
     * Role parent role
     *
     * TODO: Create a specific constraint => only role PUBLIC can have no parent
     *
     */
    @OneToOne
    private Role parentRole;

    /**
     * Role permissions
     */
    @Valid
    @OneToMany
    @Column(name = "permissions")
    private List<ResourcesAccess> permissions;

    /**
     * Role associated project users
     */
    @Valid
    @OneToMany
    @Column(name = "projectUsers")
    private List<ProjectUser> projectUsers;

    /**
     * Role associated authorized IP addresses
     */
    @Column(name = "authorizedAdresses")
    @Convert(converter = RoleAuthorizedAdressesConverter.class)
    private List<String> authorizedAddresses;

    /**
     * Are the cors requests authorized for this role ?
     */
    @Column(name = "corsRequestsAuthorized")
    private boolean isCorsRequestsAuthorized = false;

    /**
     * If CORS requests are authorized for this role, this parameter indicates the limit date of the authorization
     */
    @Column(name = "corsRequestsAuthEndDate")
    private LocalDateTime corsRequestsAuthorizationEndDate;

    /**
     * Is a default role ?
     */
    @Column(name = "default")
    private boolean isDefault;

    /**
     * Is a native role ?
     */
    @Column(name = "native")
    private boolean isNative;

    /**
     *
     * Constructor
     *
     * @since 1.0-SNAPSHOT
     */
    public Role() {
        super();
        isDefault = false;
        isNative = false;
    }

    /**
     *
     * Constructor
     *
     * @param pRoleId
     *            Role identifier
     * @since 1.0-SNAPSHOT
     */
    public Role(final Long pRoleId) {
        id = pRoleId;
    }

    public Role(final Long pRoleId, final String pName, final Role pParentRole,
            final List<ResourcesAccess> pPermissions, final List<ProjectUser> pProjectUsers) {
        this(pRoleId);
        name = pName;
        parentRole = pParentRole;
        permissions = pPermissions;
        projectUsers = pProjectUsers;
    }

    public Role(final Long pRoleId, final String pName, final Role pParentRole,
            final List<ResourcesAccess> pPermissions, final List<ProjectUser> pProjectUsers, final boolean pIsDefault,
            final boolean pIsNative) {
        this(pRoleId, pName, pParentRole, pPermissions, pProjectUsers);
        isDefault = pIsDefault;
        isNative = pIsNative;
    }

    public Role(final Long pRoleId, final String pName, final Role pParentRole,
            final List<ResourcesAccess> pPermissions, final List<String> pAuthorizedAddresses,
            final List<ProjectUser> pProjectUsers, final boolean pIsDefault, final boolean pIsNative,
            final boolean pIsCorsRequestsAuthorized, final LocalDateTime pCorsRequestsEndDate) {
        this(pRoleId, pName, pParentRole, pPermissions, pProjectUsers);
        isDefault = pIsDefault;
        isNative = pIsNative;
        isCorsRequestsAuthorized = pIsCorsRequestsAuthorized;
        corsRequestsAuthorizationEndDate = pCorsRequestsEndDate;
        authorizedAddresses = pAuthorizedAddresses;
    }

    public void setNative(final boolean pIsNative) {
        isNative = pIsNative;
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(final Long pId) {
        id = pId;
    }

    public String getName() {
        return name;
    }

    public Role getParentRole() {
        return parentRole;
    }

    public List<ResourcesAccess> getPermissions() {
        return permissions;
    }

    public List<ProjectUser> getProjectUsers() {
        return projectUsers;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public boolean isNative() {
        return isNative;
    }

    public void setDefault(final boolean pIsDefault) {
        isDefault = pIsDefault;
    }

    public void setName(final String pName) {
        name = pName;
    }

    public void setParentRole(final Role pParentRole) {
        parentRole = pParentRole;
    }

    public void setPermissions(final List<ResourcesAccess> pPermissions) {
        permissions = pPermissions;
    }

    public void setProjectUsers(final List<ProjectUser> pProjectUsers) {
        projectUsers = pProjectUsers;
    }

    public List<String> getAuthorizedAddresses() {
        return authorizedAddresses;
    }

    public void setAuthorizedAddresses(final List<String> pAuthorizedAddresses) {
        authorizedAddresses = pAuthorizedAddresses;
    }

    public boolean isCorsRequestsAuthorized() {
        return isCorsRequestsAuthorized;
    }

    public void setCorsRequestsAuthorized(final boolean pIsCorsRequestsAuthorized) {
        isCorsRequestsAuthorized = pIsCorsRequestsAuthorized;
    }

    public LocalDateTime getCorsRequestsAuthorizationEndDate() {
        return corsRequestsAuthorizationEndDate;
    }

    public void setCorsRequestsAuthorizationEndDate(final LocalDateTime pCorsRequestsAuthorizationEndDate) {
        corsRequestsAuthorizationEndDate = pCorsRequestsAuthorizationEndDate;
    }

    @Override
    public boolean equals(final Object pObj) {
        return (pObj instanceof Role) && ((Role) pObj).getId().equals(id);
    }

    @Override
    public int hashCode() {
        return (int) (long) id;
    }

}
