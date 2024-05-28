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
package fr.cnes.regards.modules.accessrights.domain.projects;

import fr.cnes.regards.framework.gson.annotation.GsonIgnore;
import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.modules.accessrights.domain.projects.validation.HasValidParent;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Models a user's role.
 * <p>
 * Role hierarchy is as follow:
 * <ol>
 *     <li>PUBLIC</li>
 *     <li>Custom roles having PUBLIC as parent</li>
 *     <li>REGISTERED USER</li>
 *     <li>Custom roles having REGISTERED USER as parent</li>
 *     <li>ADMIN</li>
 *     <li>Custom roles having ADMIN as parent</li>
 *     <li>PROJECT ADMIN</li>
 * </ol>
 *
 * @author Xavier-Alexandre Brochard
 * @author Sébastien Binda
 */
@Entity
@Table(name = "t_role",
       indexes = { @Index(name = "idx_role_name", columnList = "name") },
       uniqueConstraints = @UniqueConstraint(name = "uk_role_name", columnNames = { "name" }))
@SequenceGenerator(name = "roleSequence", initialValue = 1, sequenceName = "seq_role")
@HasValidParent
@NamedEntityGraphs(value = { @NamedEntityGraph(name = "graph.role.permissions",
                                               attributeNodes = @NamedAttributeNode(value = "permissions")),
                             @NamedEntityGraph(name = "graph.role.parent",
                                               attributeNodes = { @NamedAttributeNode(value = "permissions"),
                                                                  @NamedAttributeNode(value = "parentRole",
                                                                                      subgraph = "parentGraph") },
                                               subgraphs = { @NamedSubgraph(name = "parentGraph",
                                                                            attributeNodes = { @NamedAttributeNode(value = "permissions") }) }) })
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
    @Column
    private String name;

    /**
     * The parent role.
     * <p/>
     * Must not be null except if current role is PUBLIC. Validated via type-level {@link HasValidParent} annotation.
     */
    @ManyToOne
    @JoinColumn(name = "parent_role_id", foreignKey = @ForeignKey(name = "fk_role_parent_role"))
    private Role parentRole;

    /**
     * Role permissions
     */
    @Valid
    @ManyToMany
    @OrderBy("resource")
    @JoinTable(name = "ta_resource_role",
               joinColumns = @JoinColumn(name = "role_id",
                                         referencedColumnName = "ID",
                                         foreignKey = @ForeignKey(name = "fk_resource_role_role_id")),
               inverseJoinColumns = @JoinColumn(name = "resource_id",
                                                referencedColumnName = "ID",
                                                foreignKey = @ForeignKey(name = "fk_resource_role_resource_id")))
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
     * Constructor
     */
    public Role() {
        super();
        isDefault = false;
        isNative = false;
        permissions = new HashSet<>();
    }

    /**
     * Contructor setting the parameter as attribute
     */
    public Role(String name) {
        this();
        this.name = name;
    }

    /**
     * Constructor
     *
     * @param pName       the name
     * @param pParentRole the parent
     */
    public Role(final String pName, final Role pParentRole) {
        this(pName);
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
     * @param pName the name to set
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
     * @param pParentRole the parentRole to set
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
     * @param pPermissions the permissions to set
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
     * @param pAuthorizedAddresses the authorizedAddresses to set
     */
    public void setAuthorizedAddresses(final List<String> pAuthorizedAddresses) {
        authorizedAddresses = pAuthorizedAddresses;
    }

    /**
     * @return the isDefault
     */
    @Schema(name = "isDefault") // Force name isDefault otherwise swagger believes that attribute name is "default"
    public boolean isDefault() {
        return isDefault;
    }

    /**
     * @param pIsDefault the isDefault to set
     */
    public void setDefault(final boolean pIsDefault) {
        isDefault = pIsDefault;
    }

    /**
     * @return the isNative
     */
    @Schema(name = "isNative") // Force name isNative otherwise swagger believes that attribute name is "native"
    public boolean isNative() {
        return isNative;
    }

    /**
     * @param pId the id to set
     */
    public void setId(final Long pId) {
        id = pId;
    }

    /**
     * Add the given {@link ResourcesAccess} to the permissions of the current {@link Role}
     *
     * @param pResourcesAccess A {@link ResourcesAccess} to add
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
            return other.name == null;
        } else {
            return name.equals(other.name);
        }
    }

    @Override
    public String toString() {
        return "Role [id="
               + id
               + ", name="
               + name
               + ", parentRole="
               + parentRole
               + ", isDefault="
               + isDefault
               + ", isNative="
               + isNative
               + "]";
    }

}
