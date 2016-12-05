/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain.projects;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import fr.cnes.regards.modules.accessrights.domain.projects.validation.HasParentOrPublic;

/**
 * Models a user's role.
 *
 * @author Sébastien Binda
 */
public class RoleDTO {

    /**
     * Role identifier
     */
    private final Long id;

    /**
     * Role name
     */
    private final String name;

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
    private final List<String> authorizedAddresses;

    /**
     * Are the cors requests authorized for this role ?
     */
    private final boolean isCorsRequestsAuthorized;

    /**
     * If CORS requests are authorized for this role, this parameter indicates the limit date of the authorization
     */
    private final LocalDateTime corsRequestsAuthorizationEndDate;

    /**
     * Is a default role ?
     */
    private final boolean isDefault;

    /**
     * Is a native role ?
     */
    private final boolean isNative;

    /**
     *
     * Constructor
     *
     * @since 1.0-SNAPSHOT
     */
    public RoleDTO(final Role pRole) {
        super();
        id = pRole.getId();
        name = pRole.getName();
        isDefault = pRole.isDefault();
        isNative = pRole.isNative();
        isCorsRequestsAuthorized = pRole.isCorsRequestsAuthorized();
        if (pRole.getPermissions() != null) {
            permissions = new ArrayList<>();
            for (final ResourcesAccess permission : pRole.getPermissions()) {
                permissions.add(new ResourcesAccess(permission.getId(), permission.getDescription(),
                        permission.getMicroservice(), permission.getResource(), permission.getVerb()));
            }
        }
        corsRequestsAuthorizationEndDate = pRole.getCorsRequestsAuthorizationEndDate();
        authorizedAddresses = pRole.getAuthorizedAddresses();
    }

    public Long getId() {
        return id;
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

    public List<String> getAuthorizedAddresses() {
        return authorizedAddresses;
    }

    public boolean isCorsRequestsAuthorized() {
        return isCorsRequestsAuthorized;
    }

    public LocalDateTime getCorsRequestsAuthorizationEndDate() {
        return corsRequestsAuthorizationEndDate;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public boolean isNative() {
        return isNative;
    }

}
