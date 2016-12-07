/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.event.handler;

import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.security.event.UpdateAuthoritiesEvent;

/**
 *
 * Class UpdateAuthoritiesEventHandler
 *
 * Handler when Microservice recieved a {@link UpdateAuthoritiesEvent} event by amq queue. The action is to update the
 * local authorities by asking to administration microservice.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class UpdateAuthoritiesEventHandler implements IHandler<UpdateAuthoritiesEvent> {

    /**
     * Service that contains the current cached authorities to update.
     */
    private final MethodAuthorizationService service;

    public UpdateAuthoritiesEventHandler(final MethodAuthorizationService pService) {
        super();
        service = pService;
    }

    /**
     *
     * Refresh authorities of the current microservice
     * 
     * @see fr.cnes.regards.framework.amqp.domain.IHandler#handle(fr.cnes.regards.framework.amqp.domain.TenantWrapper)
     * @since 1.0-SNAPSHOT
     */
    @Override
    public void handle(final TenantWrapper<UpdateAuthoritiesEvent> pT) {
        service.refreshAuthorities();
    }
}
