/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.role.event.handler;

import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;
import fr.cnes.regards.modules.project.domain.event.NewProjectConnectionEvent;

/**
 *
 * Class NewProjectConnectionEventHandler
 *
 * Action to manage a new ProjectConnection Event
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class NewProjectConnectionEventHandler implements IHandler<NewProjectConnectionEvent> {

    /**
     * JWT Security service
     */
    private final JWTService jwtService;

    /**
     * Business service to manage {@link Role} Entities
     */
    private final IRoleService roleService;

    public NewProjectConnectionEventHandler(final JWTService pJwtService, final IRoleService pRoleService) {
        super();
        jwtService = pJwtService;
        roleService = pRoleService;
    }

    /**
     *
     * Initialize default roles in the new project connection
     * 
     * @see fr.cnes.regards.framework.amqp.domain.IHandler#handle(fr.cnes.regards.framework.amqp.domain.TenantWrapper)
     * @since 1.0-SNAPSHOT
     */
    @Override
    public void handle(final TenantWrapper<NewProjectConnectionEvent> pNewProjectConnection) {

        jwtService.injectMockToken(pNewProjectConnection.getContent().getNewProjectConnection().getProject()
                .getName(), RoleAuthority
                        .getSysRole(pNewProjectConnection.getContent().getNewProjectConnection().getMicroservice()));

        roleService.initDefaultRoles();
    }

}
