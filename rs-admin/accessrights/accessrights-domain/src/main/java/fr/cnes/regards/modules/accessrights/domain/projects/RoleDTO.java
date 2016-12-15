/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain.projects;

import java.util.ArrayList;
import java.util.List;

/**
 * Models a user's role.
 *
 * @author Sébastien Binda
 */
public class RoleDTO extends Role {

    /**
     *
     * Constructor
     * 
     * @param pRole
     *            The {@link Role} to extends
     *
     * @since 1.0-SNAPSHOT
     */
    public RoleDTO(final Role pRole) {
        super(pRole.getName(), pRole.getParentRole());
        super.setId(pRole.getId());
        super.setDefault(pRole.isDefault());
        super.setNative(pRole.isNative());
        super.setCorsRequestsAuthorized(pRole.isCorsRequestsAuthorized());
        if (pRole.getPermissions() != null) {
            final List<ResourcesAccess> permissions = new ArrayList<>();
            for (final ResourcesAccess permission : pRole.getPermissions()) {
                permissions.add(new ResourcesAccess(permission.getId(), permission.getDescription(),
                        permission.getMicroservice(), permission.getResource(), permission.getVerb()));
            }
            super.setPermissions(permissions);
        }
        super.setCorsRequestsAuthorizationEndDate(pRole.getCorsRequestsAuthorizationEndDate());
        super.setAuthorizedAddresses(pRole.getAuthorizedAddresses());
    }

}
