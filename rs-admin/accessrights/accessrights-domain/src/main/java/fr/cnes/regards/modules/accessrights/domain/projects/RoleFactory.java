/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain.projects;

import java.time.LocalDateTime;
import java.util.List;

import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.domain.projects.validation.HasParentOrPublic;

/**
 * Helper classs for creating Roles implemented as a fluent API.
 * </p>
 * 1 - Create an instance of the factory. A new role will be created with default values.<br>
 * 2 - You can use the different 'with' methods to specify the non-default fields for your role.<br>
 * 3 - Call 'create' to return the created role.<br>
 * OR<br>
 * 3 - Call a 'createPublic'/'createAdmin/...' method to use the predefined role templates.<br>
 * </p>
 * Note: The values set via 'with' are then cached in the instance of the factory. This allows to reuse previously set
 * values when creating various roles successively, and thus reducing code boilerplate. If you do not want to use cached
 * values, simply create a new instance of the factory.
 *
 * @author Xavier-Alexandre Brochard
 */
public class RoleFactory {

    /**
     * Role indentifier
     */
    private Long id;

    /**
     * Role name
     */
    private String name;

    /**
     * The parent role.
     * <p/>
     * Must not be null except if current role is PUBLIC. Validated via type-level {@link HasParentOrPublic} annotation.
     */
    private Role parentRole;

    /**
     * Role permissions
     */
    private List<ResourcesAccess> permissions;

    /**
     * Role associated authorized IP addresses
     */
    private List<String> authorizedAddresses;

    /**
     * Are the cors requests authorized for this role ?
     */
    private boolean isCorsRequestsAuthorized;

    /**
     * If CORS requests are authorized for this role, this parameter indicates the limit date of the authorization
     */
    private LocalDateTime corsRequestsAuthorizationEndDate;

    /**
     * Is a default role ?
     */
    private boolean isDefault;

    /**
     * Is a native role ?
     */
    private boolean isNative;

    /**
     * When create a native role, should we automaticly create in cascade the role parent ?
     */
    private boolean autoCreateParents = true;

    /**
     * Private constructor for static class
     */
    public RoleFactory() {
        final Role forDefaultValues = new Role();
        id = forDefaultValues.getId();
        name = forDefaultValues.getName();
        parentRole = forDefaultValues.getParentRole();
        permissions = forDefaultValues.getPermissions();
        authorizedAddresses = forDefaultValues.getAuthorizedAddresses();
        isCorsRequestsAuthorized = forDefaultValues.isCorsRequestsAuthorized();
        corsRequestsAuthorizationEndDate = forDefaultValues.getCorsRequestsAuthorizationEndDate();
        isDefault = forDefaultValues.isDefault();
        isNative = forDefaultValues.isNative();
    }

    /**
     * Create a new role with values set in the factory
     *
     * @return the role
     */
    public Role create() {
        final Role toCreate = new Role();
        toCreate.setId(id);
        toCreate.setName(name);
        toCreate.setParentRole(parentRole);
        toCreate.setPermissions(permissions);
        toCreate.setAuthorizedAddresses(authorizedAddresses);
        toCreate.setCorsRequestsAuthorized(isCorsRequestsAuthorized);
        toCreate.setCorsRequestsAuthorizationEndDate(corsRequestsAuthorizationEndDate);
        toCreate.setDefault(isDefault);
        toCreate.setNative(isNative);
        return toCreate;
    }

    public Role createAdmin() {
        final Role toCreate = create();
        final RoleFactory factoryForParentRole = new RoleFactory();
        toCreate.setName(DefaultRole.ADMIN.toString());
        toCreate.setNative(true);
        if (autoCreateParents) {
            toCreate.setParentRole(factoryForParentRole.createRegisteredUser());
        }
        return toCreate;
    }

    public Role createInstanceAdmin() {
        final Role toCreate = create();
        final RoleFactory factoryForParentRole = new RoleFactory();
        toCreate.setName(DefaultRole.INSTANCE_ADMIN.toString());
        toCreate.setNative(true);
        if (autoCreateParents) {
            toCreate.setParentRole(factoryForParentRole.createProjectAdmin());
        }
        return toCreate;
    }

    public Role createProjectAdmin() {
        final Role toCreate = create();
        final RoleFactory factoryForParentRole = new RoleFactory();
        toCreate.setName(DefaultRole.PROJECT_ADMIN.toString());
        toCreate.setNative(true);
        if (autoCreateParents) {
            toCreate.setParentRole(factoryForParentRole.createAdmin());
        }
        return toCreate;
    }

    public Role createPublic() {
        final Role toCreate = create();
        toCreate.setName(DefaultRole.PUBLIC.toString());
        toCreate.setParentRole(null);
        toCreate.setNative(true);
        toCreate.setDefault(true);
        return toCreate;
    }

    public Role createRegisteredUser() {
        final Role toCreate = create();
        final RoleFactory factoryForParentRole = new RoleFactory();
        toCreate.setName(DefaultRole.REGISTERED_USER.toString());
        toCreate.setNative(true);
        if (autoCreateParents) {
            toCreate.setParentRole(factoryForParentRole.createPublic());
        }
        return toCreate;
    }

    /**
     * @param pAuthorizedAddresses
     *            the authorizedAddresses to set
     * @return this for chaining
     */
    public RoleFactory withAuthorizedAddresses(final List<String> pAuthorizedAddresses) {
        authorizedAddresses = pAuthorizedAddresses;
        return this;
    }

    /**
     * @param pCorsRequestsAuthorizationEndDate
     *            the corsRequestsAuthorizationEndDate to set
     * @return this for chaining
     */
    public RoleFactory withCorsRequestsAuthorizationEndDate(final LocalDateTime pCorsRequestsAuthorizationEndDate) {
        corsRequestsAuthorizationEndDate = pCorsRequestsAuthorizationEndDate;
        return this;
    }

    /**
     * @param pIsCorsRequestsAuthorized
     *            the isCorsRequestsAuthorized to set
     * @return this for chaining
     */
    public RoleFactory withCorsRequestsAuthorized(final boolean pIsCorsRequestsAuthorized) {
        isCorsRequestsAuthorized = pIsCorsRequestsAuthorized;
        return this;
    }

    /**
     * @param pIsDefault
     *            the isDefault to set
     * @return this for chaining
     */
    public RoleFactory withDefault(final boolean pIsDefault) {
        isDefault = pIsDefault;
        return this;
    }

    /**
     * @param pId
     *            the id to set
     * @return this for chaining
     */
    public RoleFactory withId(final Long pId) {
        id = pId;
        return this;
    }

    /**
     * @param pName
     *            the name to set
     * @return this for chaining
     */
    public RoleFactory withName(final String pName) {
        name = pName;
        return this;
    }

    /**
     * @param pIsNative
     *            the isNative to set
     * @return this for chaining
     */
    public RoleFactory withNative(final boolean pIsNative) {
        isNative = pIsNative;
        return this;
    }

    /**
     * @param pParentRole
     *            the parent role to set
     * @return this for chaining
     */
    public RoleFactory withParentRole(final Role pParentRole) {
        parentRole = pParentRole;
        return this;
    }

    /**
     * @param pPermissions
     *            the permissions to set
     * @return this for chaining
     */
    public RoleFactory withPermissions(final List<ResourcesAccess> pPermissions) {
        permissions = pPermissions;
        return this;
    }

    /**
     * Set <code>autoCreateParents</code> to <code>true</code>
     *
     * @return this for chaining
     */
    public RoleFactory doNotAutoCreateParents() {
        autoCreateParents = false;
        return this;
    }

    /**
     * Set <code>autoCreateParents</code> to <code>false</code>
     *
     * @return this for chaining
     */
    public RoleFactory doAutoCreateParents() {
        autoCreateParents = true;
        return this;
    }

}
