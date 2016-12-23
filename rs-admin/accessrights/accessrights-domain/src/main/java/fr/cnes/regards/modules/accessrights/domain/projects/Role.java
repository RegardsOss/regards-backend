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
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.Valid;

import org.hibernate.validator.constraints.NotBlank;

import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.framework.security.entity.listeners.UpdateAuthoritiesListener;
import fr.cnes.regards.modules.accessrights.domain.projects.validation.HasParentOrPublic;

/**
 * Models a user's role.
 *
 * @author Xavier-Alexandre Brochard
 * @author Sébastien Binda
 */
@Entity
@EntityListeners(UpdateAuthoritiesListener.class)
@Table(name = "T_ROLE", indexes = { @Index(name = "IDX_ROLE_NAME", columnList = "name") })
@SequenceGenerator(name = "roleSequence", initialValue = 1, sequenceName = "SEQ_ROLE")
@HasParentOrPublic
public class Role implements IIdentifiable<Long> {

    /**
     * Role identifier
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
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_role_id", foreignKey = @ForeignKey(name = "FK_ROLE_PARENT_ROLE"))
    private Role parentRole;

    /**
     * Role permissions
     */
    @Valid
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "TA_RESOURCE_ROLE", joinColumns = @JoinColumn(name = "ROLE_ID", referencedColumnName = "ID"),
            inverseJoinColumns = @JoinColumn(name = "RESOURCE_ID", referencedColumnName = "ID"))
    private List<ResourcesAccess> permissions;

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
    @Column(name = "is_default")
    private boolean isDefault;

    /**
     * Is a native role ?
     */
    @Column(name = "is_native")
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
        isCorsRequestsAuthorized = true;
        permissions = new ArrayList<>();
        authorizedAddresses = new ArrayList<>();
    }

    public Role(final String pName, final Role pParentRole) {
        this();
        name = pName;
        parentRole = pParentRole;
    }

    public void setNative(final boolean pIsNative) {
        isNative = pIsNative;
    }

    @Override
    public Long getId() {
        return id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param pName
     *            the name to set
     */
    public void setName(final String pName) {
        name = pName;
    }

    /**
     * @return the parentRole
     */
    public Role getParentRole() {
        return parentRole;
    }

    /**
     * @param pParentRole
     *            the parentRole to set
     */
    public void setParentRole(final Role pParentRole) {
        parentRole = pParentRole;
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
     * @return the authorizedAddresses
     */
    public List<String> getAuthorizedAddresses() {
        return authorizedAddresses;
    }

    /**
     * @param pAuthorizedAddresses
     *            the authorizedAddresses to set
     */
    public void setAuthorizedAddresses(final List<String> pAuthorizedAddresses) {
        authorizedAddresses = pAuthorizedAddresses;
    }

    /**
     * @return the isCorsRequestsAuthorized
     */
    public boolean isCorsRequestsAuthorized() {
        return isCorsRequestsAuthorized;
    }

    /**
     * @param pIsCorsRequestsAuthorized
     *            the isCorsRequestsAuthorized to set
     */
    public void setCorsRequestsAuthorized(final boolean pIsCorsRequestsAuthorized) {
        isCorsRequestsAuthorized = pIsCorsRequestsAuthorized;
    }

    /**
     * @return the corsRequestsAuthorizationEndDate
     */
    public LocalDateTime getCorsRequestsAuthorizationEndDate() {
        return corsRequestsAuthorizationEndDate;
    }

    /**
     * @param pCorsRequestsAuthorizationEndDate
     *            the corsRequestsAuthorizationEndDate to set
     */
    public void setCorsRequestsAuthorizationEndDate(final LocalDateTime pCorsRequestsAuthorizationEndDate) {
        corsRequestsAuthorizationEndDate = pCorsRequestsAuthorizationEndDate;
    }

    /**
     * @return the isDefault
     */
    public boolean isDefault() {
        return isDefault;
    }

    /**
     * @param pIsDefault
     *            the isDefault to set
     */
    public void setDefault(final boolean pIsDefault) {
        isDefault = pIsDefault;
    }

    /**
     * @return the isNative
     */
    public boolean isNative() {
        return isNative;
    }

    /**
     * @param pId
     *            the id to set
     */
    public void setId(final Long pId) {
        id = pId;
    }

    @Override
    public int hashCode() {
        if (this.id != null) {
            return this.id.hashCode();
        } else
            if (this.name != null) {
                return this.name.hashCode();
            } else {
                return 0;
            }
    }

    @Override
    public boolean equals(final Object pObj) {
        if (pObj instanceof Role) {
            return this.hashCode() == pObj.hashCode();
        }
        return false;
    }

}
