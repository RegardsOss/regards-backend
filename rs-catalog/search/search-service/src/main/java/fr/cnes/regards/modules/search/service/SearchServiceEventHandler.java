/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.dataaccess.domain.accessgroup.event.AccessGroupAssociationEvent;
import fr.cnes.regards.modules.dataaccess.domain.accessgroup.event.AccessGroupDissociationEvent;
import fr.cnes.regards.modules.dataaccess.domain.accessgroup.event.AccessGroupPublicEvent;
import fr.cnes.regards.modules.search.service.cache.accessgroup.IAccessGroupCache;

/**
 * @author Marc Sordi
 *
 */
@Component
public class SearchServiceEventHandler implements ApplicationListener<ApplicationReadyEvent> {

    private final IAccessGroupCache accessGroupCache;

    private final ISubscriber subscriber;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    public SearchServiceEventHandler(IAccessGroupCache accessGroupClientService, ISubscriber subscriber,
            IRuntimeTenantResolver runtimeTenantResolver) {
        this.accessGroupCache = accessGroupClientService;
        this.subscriber = subscriber;
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent pEvent) {
        subscriber.subscribeTo(AccessGroupAssociationEvent.class, new AccessGroupAssociationEventHandler());
        subscriber.subscribeTo(AccessGroupDissociationEvent.class, new AccessGroupDissociationEventHandler());
        subscriber.subscribeTo(AccessGroupPublicEvent.class, new AccessGroupPublicEventHandler());
    }

    /**
     * Handle {@link AccessGroupAssociationEvent} event to refresh group cache
     *
     * @author Marc Sordi
     *
     */
    private class AccessGroupAssociationEventHandler implements IHandler<AccessGroupAssociationEvent> {

        @Override
        public void handle(TenantWrapper<AccessGroupAssociationEvent> wrapper) {
            try {
                runtimeTenantResolver.forceTenant(wrapper.getTenant());
                accessGroupCache.cleanAccessGroups(wrapper.getContent().getUserEmail(), wrapper.getTenant());
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    /**
     * Handle {@link AccessGroupDissociationEvent} event to refresh group cache
     *
     * @author Marc Sordi
     *
     */
    private class AccessGroupDissociationEventHandler implements IHandler<AccessGroupDissociationEvent> {

        @Override
        public void handle(TenantWrapper<AccessGroupDissociationEvent> wrapper) {
            try {
                runtimeTenantResolver.forceTenant(wrapper.getTenant());
                accessGroupCache.cleanAccessGroups(wrapper.getContent().getUserEmail(), wrapper.getTenant());
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    /**
     * Handle {@link AccessGroupPublicEvent} event to clean all cache entries
     *
     * @author Marc Sordi
     *
     */
    private class AccessGroupPublicEventHandler implements IHandler<AccessGroupPublicEvent> {

        @Override
        public void handle(TenantWrapper<AccessGroupPublicEvent> wrapper) {
            try {
                runtimeTenantResolver.forceTenant(wrapper.getTenant());
                accessGroupCache.cleanPublicAccessGroups(wrapper.getTenant());
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

}
