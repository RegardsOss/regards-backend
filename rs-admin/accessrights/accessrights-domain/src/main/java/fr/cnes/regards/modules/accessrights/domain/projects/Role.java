/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain.projects;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.OrderBy;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.Valid;

import org.hibernate.validator.constraints.NotBlank;

import fr.cnes.regards.framework.gson.annotation.GsonIgnore;
import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.modules.accessrights.domain.projects.validation.HasValidParent;

/**
 * Models a user's role.
 *
 * @author Xavier-Alexandre Brochard
 * @author Sébastien Binda
 */
@Entity
@Table(name = "T_ROLE", indexes = { @Index(name = "IDX_ROLE_NAME", columnList = "name") })
@SequenceGenerator(name = "roleSequence", initialValue = 1, sequenceName = "SEQ_ROLE")
@HasValidParent
@NamedEntityGraph(name = "graph.role.permissions", attributeNodes = @NamedAttributeNode(value = "permissions", subgraph = "permissions"))
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
     * Must not be null except if current role is PUBLIC. Validated via type-level {@link HasValidParent} annotation.
     */
    @ManyToOne
    @JoinColumn(name = "parent_role_id", foreignKey = @ForeignKey(name = "FK_ROLE_PARENT_ROLE"))
    private Role parentRole;

    /**
     * Role permissions
     */
    @Valid
    @ManyToMany(cascade = CascadeType.ALL)
    // TODO Remove cascade property. Test fails ?
    @OrderBy("resource")
    @JoinTable(name = "TA_RESOURCE_ROLE", joinColumns = @JoinColumn(name = "ROLE_ID", referencedColumnName = "ID"), inverseJoinColumns = @JoinColumn(name = "RESOURCE_ID", referencedColumnName = "ID"))
    @GsonIgnore
    private Set<ResourcesAccess> permissions;

    /**
     * Role associated authorized IP addresses
     */
    @Column(name = "authorized_addresses")
    @Convert(converter = RoleAuthorizedAdressesConverter.class)
    private List<String> authorizedAddresses;

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
        permissions = new HashSet<>();
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
    public Set<ResourcesAccess> getPermissions() {
        return permissions;
    }

    /**
     * @param pPermissions
     *            the permissions to set
     */
    public void setPermissions(final Set<ResourcesAccess> pPermissions) {
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

    /**
     *
     * Add the given {@link ResourcesAccess} to the permissions of the current {@link Role}
     *
     * @param pResourcesAccess
     *            A {@link ResourcesAccess} to add
     */
    public void addPermission(final ResourcesAccess pResourcesAccess) {
        if (permissions == null) {
            permissions = new HashSet<>();
        }
        permissions.add(pResourcesAccess);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Role other = (Role) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Role [id=" + id + ", name=" + name + ", parentRole=" + parentRole + ", isDefault=" + isDefault
                + ", isNative=" + isNative + "]";
    }

}
