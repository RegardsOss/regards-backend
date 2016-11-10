/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain.projects;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Helper classs for creating Roles implemented as a fluent API.
 * </p>
 * 1 - Call 'getInstance' in order to enable chaining.<br>
 * 2 - You can use the different 'with' methods to specify the non-default fields for your role.<br>
 * 3 - Call 'create', to return the created role.<br>
 * OR<br>
 * 3 - Call a 'createPublic'/'createAdmin/...' method to use the predefined role templates.<br>
 * Step 3 is called a terminal operation.
 * </p>
 * The methods 'doRetain' and 'doNotRetain' allow the caller to define the fields retainment strategy:<br>
 * - With doNotRetain, the fields set <i>via</i> 'with' methods will be reset to default values after each terminal
 * operation. This is the default behavious.<br>
 * - With doNotRetain, the fields set <i>via</i> 'with' methods will NOT be reset, allowing the caller the 'reuse'
 * previously set values and thus reduce the code to write.
 * </p>
 *
 * @author Xavier-Alexandre Brochard
 */
public class RoleFactory {

    /**
     * Store static instance for chaining
     */
    private static RoleFactory instance = new RoleFactory();

    /**
     * The role to be creted
     */
    private static Role proxyRole;

    /**
     * Do we reset the proxyRole during terminal operations (create[...])?<br>
     * Default <code>false</code>
     */
    private boolean retain = false;

    /**
     * Set retain to true
     */
    public static void doRetain() {
        instance.retain = true;
    }

    /**
     * Set retain to false
     */
    public static void doNotRetain() {
        instance.retain = false;
    }

    /**
     * @return the instance
     */
    public static RoleFactory getInstance() {
        if (!instance.retain) {
            reset();
        }
        return instance;
    }

    /**
     * Reset the helper
     */
    public static void reset() {
        instance = new RoleFactory();
    }

    /**
     * Private constructor for static class
     */
    private RoleFactory() {
        proxyRole = new Role();
    }

    public Role create() {
        terminate();
        return proxyRole;
    }

    public Role createAdmin() {
        proxyRole.setName(DefaultRoleNames.ADMIN.toString());
        proxyRole.setParentRole(createRegisteredUser());
        proxyRole.setNative(true);
        return proxyRole;
    }

    public Role createInstanceAdmin() {
        proxyRole.setName(DefaultRoleNames.INSTANCE_ADMIN.toString());
        proxyRole.setParentRole(createProjectAdmin());
        proxyRole.setNative(true);
        return proxyRole;
    }

    public Role createProjectAdmin() {
        proxyRole.setName(DefaultRoleNames.PROJECT_ADMIN.toString());
        proxyRole.setParentRole(createAdmin());
        proxyRole.setNative(true);
        return proxyRole;
    }

    public Role createPublic() {
        proxyRole.setName(DefaultRoleNames.PUBLIC.toString());
        proxyRole.setParentRole(null);
        proxyRole.setNative(true);
        proxyRole.setDefault(true);
        return proxyRole;
    }

    public Role createRegisteredUser() {
        proxyRole.setName(DefaultRoleNames.REGISTERED_USER.toString());
        proxyRole.setParentRole(createPublic());
        proxyRole.setNative(true);
        return proxyRole;
    }

    private void terminate() {
        if (!instance.retain) {
            reset();
        }
    }

    /**
     * @param pAuthorizedAddresses
     *            the authorizedAddresses to set
     */
    public RoleFactory withAuthorizedAddresses(final List<String> pAuthorizedAddresses) {
        proxyRole.setAuthorizedAddresses(pAuthorizedAddresses);
        return instance;
    }

    /**
     * @param pCorsRequestsAuthorizationEndDate
     *            the corsRequestsAuthorizationEndDate to set
     */
    public RoleFactory withCorsRequestsAuthorizationEndDate(final LocalDateTime pCorsRequestsAuthorizationEndDate) {
        proxyRole.setCorsRequestsAuthorizationEndDate(pCorsRequestsAuthorizationEndDate);
        return instance;
    }

    /**
     * @param pIsCorsRequestsAuthorized
     *            the isCorsRequestsAuthorized to set
     */
    public RoleFactory withCorsRequestsAuthorized(final boolean pIsCorsRequestsAuthorized) {
        proxyRole.setCorsRequestsAuthorized(pIsCorsRequestsAuthorized);
        return instance;
    }

    /**
     * @param pIsDefault
     *            the isDefault to set
     */
    public RoleFactory withDefault(final boolean pIsDefault) {
        proxyRole.setDefault(pIsDefault);
        return instance;
    }

    /**
     * @param pId
     *            the id to set
     */
    public RoleFactory withId(final Long pId) {
        proxyRole.setId(pId);
        return instance;
    }

    /**
     * @param pName
     *            the name to set
     */
    public RoleFactory withName(final String pName) {
        proxyRole.setName(pName);
        return instance;
    }

    /**
     * @param pIsNative
     *            the isNative to set
     */
    public RoleFactory withNative(final boolean pIsNative) {
        proxyRole.setNative(pIsNative);
        return instance;
    }

    /**
     * @param pParentRole
     *            the parent role to set
     */
    public RoleFactory withParentRole(final Role pParentRole) {
        proxyRole.setParentRole(pParentRole);
        return instance;
    }

    /**
     * @param pPermissions
     *            the permissions to set
     */
    public RoleFactory withPermissions(final List<ResourcesAccess> pPermissions) {
        proxyRole.setPermissions(pPermissions);
        return instance;
    }

    /**
     * @param pProjectUsers
     *            the projectUsers to set
     */
    public RoleFactory withProjectUsers(final List<ProjectUser> pProjectUsers) {
        proxyRole.setProjectUsers(pProjectUsers);
        return instance;
    }

}
