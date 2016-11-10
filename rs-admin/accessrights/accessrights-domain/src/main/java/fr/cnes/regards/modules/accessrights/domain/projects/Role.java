/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain.projects;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.Valid;

import org.hibernate.validator.constraints.NotBlank;

import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.modules.accessrights.domain.projects.validation.HasParentOrPublic;

/**
 * Models a user's role.
 *
 * @author Xavier-Alexandre Brochard
 */
@Entity
@Table(name = "T_ROLE", indexes = { @Index(name = "IDX_ROLE_NAME", columnList = "name") })
@SequenceGenerator(name = "roleSequence", initialValue = 1, sequenceName = "SEQ_ROLE")
@HasParentOrPublic
public class Role implements IIdentifiable<Long> {

    /**
     * Role indentifier
     */
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "roleSequence")
    private Long id;

    /**
     * Role name
     */
    @NotBlank
    @Column(unique = true)
    private String name;

    /**
     * The parent role.
     * <p/>
     * Must not be null except if current role is PUBLIC. Validated via type-level {@link HasParentOrPublic} annotation.
     */
    @ManyToOne
    @JoinColumn(name = "parent_role_id", foreignKey = @ForeignKey(name = "FK_ROLE_PARENT_ROLE"))
    private Role parentRole;

    /**
     * Role permissions
     */
    @Valid
    @OneToMany(fetch = FetchType.EAGER)
    private List<ResourcesAccess> permissions;

    /**
     * Role associated project users
     */
    @Valid
    @OneToMany(mappedBy = "role", fetch = FetchType.EAGER)
    private List<ProjectUser> projectUsers;

    /**
     * Role associated authorized IP addresses
     */
    @Column(name = "authorized_addresses")
    @Convert(converter = RoleAuthorizedAdressesConverter.class)
    private List<String> authorizedAddresses;

    /**
     * Are the cors requests authorized for this role ?
     */
    @Column(name = "cors_requests_authorized")
    private boolean isCorsRequestsAuthorized;

    /**
     * If CORS requests are authorized for this role, this parameter indicates the limit date of the authorization
     */
    @Column(name = "cors_requests_auth_end_date")
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
        isCorsRequestsAuthorized = false;
        permissions = new ArrayList<>();
        projectUsers = new ArrayList<>();
        authorizedAddresses = new ArrayList<>();
    }

    // /**
    // *
    // * Constructor
    // *
    // * @param pRoleId
    // * Role identifier
    // * @since 1.0-SNAPSHOT
    // */
    // public Role(final Long pRoleId) {
    // id = pRoleId;
    // }

    public Role(final String pName, final Role pParentRole) {
        this();
        name = pName;
        parentRole = pParentRole;
    }

    // public Role(final Long pRoleId, final String pName, final Role pParentRole,
    // final List<ResourcesAccess> pPermissions, final List<ProjectUser> pProjectUsers, final boolean pIsDefault,
    // final boolean pIsNative) {
    // this(pRoleId, pName, pParentRole, pPermissions, pProjectUsers);
    // isDefault = pIsDefault;
    // isNative = pIsNative;
    // }
    //
    // public Role(final Long pRoleId, final String pName, final Role pParentRole,
    // final List<ResourcesAccess> pPermissions, final List<String> pAuthorizedAddresses,
    // final List<ProjectUser> pProjectUsers, final boolean pIsDefault, final boolean pIsNative,
    // final boolean pIsCorsRequestsAuthorized, final LocalDateTime pCorsRequestsEndDate) {
    // this(pRoleId, pName, pParentRole, pPermissions, pProjectUsers);
    // isDefault = pIsDefault;
    // isNative = pIsNative;
    // isCorsRequestsAuthorized = pIsCorsRequestsAuthorized;
    // corsRequestsAuthorizationEndDate = pCorsRequestsEndDate;
    // authorizedAddresses = pAuthorizedAddresses;
    // }

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

}
