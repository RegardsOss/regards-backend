/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.entities.service.adapters.gson;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.entities.domain.attribute.ObjectAttribute;
import fr.cnes.regards.modules.models.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.models.domain.attributes.Fragment;
import fr.cnes.regards.modules.models.domain.event.AttributeModelCreated;
import fr.cnes.regards.modules.models.domain.event.AttributeModelDeleted;
import fr.cnes.regards.modules.models.domain.event.FragmentDeletedEvent;
import fr.cnes.regards.modules.models.service.IAttributeModelService;

/**
 * Handler to initialize subTypes for MultitenantFlattenedAttributeAdapterFactory after ApplicationReadyEvent sent.
 *
 * @author Sébastien Binda
 */
@Component
public class MultitenantFlattenedAttributeAdapterFactoryEventHandler
        implements ApplicationListener<ApplicationReadyEvent> {

    /**
     * AMQ Subscriber
     */
    @Autowired
    private ISubscriber subscriber;

    /**
     * Tenant resolver
     */
    @Autowired
    private ITenantResolver tenantResolver;

    /**
     * Runtime tenant resolver
     */
    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * AttributeModel service to retrieve AttributeModels entities.
     */
    @Autowired
    private IAttributeModelService attributeModelService;

    /**
     * Factory to work with
     */
    @Autowired
    private MultitenantFlattenedAttributeAdapterFactory factory;

    @Override
    public void onApplicationEvent(final ApplicationReadyEvent pEvent) {
        subscriber.subscribeTo(AttributeModelCreated.class, new RegisterHandler());
        subscriber.subscribeTo(AttributeModelDeleted.class, new UnregisterHandler());
        subscriber.subscribeTo(FragmentDeletedEvent.class, new UnregisterFragmentHandler());
        // Retrieve all tenants
        for (final String tenant : tenantResolver.getAllActiveTenants()) {
            // Set thread tenant to route database retrieval
            runtimeTenantResolver.forceTenant(tenant);
            // Register for tenant
            final List<AttributeModel> atts = attributeModelService.getAttributes(null, null);
            factory.registerAttributes(tenant, atts);
        }
    }

    /**
     * Handle {@link AttributeModel} creation
     *
     * @author Marc Sordi
     */
    private class RegisterHandler implements IHandler<AttributeModelCreated> {

        @Override
        public void handle(final TenantWrapper<AttributeModelCreated> pWrapper) {
            final AttributeModelCreated amc = pWrapper.getContent();
            String tenant=pWrapper.getTenant();
            runtimeTenantResolver.forceTenant(tenant);
            AttributeModel attributeModel=attributeModelService.findByNameAndFragmentName(amc.getAttributeName(), amc.getFragmentName());
            factory.registerAttribute(tenant, attributeModel);
        }
    }

    /**
     * Handle {@link AttributeModel} deletion
     *
     * @author Marc Sordi
     */
    private class UnregisterHandler implements IHandler<AttributeModelDeleted> {

        @Override
        public void handle(final TenantWrapper<AttributeModelDeleted> pWrapper) {
            final AttributeModelDeleted amd = pWrapper.getContent();
            if(amd.getFragmentName().equals(Fragment.getDefaultName())) {
                factory.unregisterSubtype(pWrapper.getTenant(), factory.getClassByType(amd.getAttributeType()), amd.getAttributeName(), null);
            } else {
                factory.unregisterSubtype(pWrapper.getTenant(), factory.getClassByType(amd.getAttributeType()), amd.getAttributeName(), amd.getFragmentName());
            }
        }
    }

    private class UnregisterFragmentHandler implements IHandler<FragmentDeletedEvent> {

        @Override
        public void handle(final TenantWrapper<FragmentDeletedEvent> pWrapper) {
            String tenant=pWrapper.getTenant();
            FragmentDeletedEvent fragmentDeleted = pWrapper.getContent();
            factory.unregisterSubtype(tenant, ObjectAttribute.class, fragmentDeleted.getFragmentName());
        }
    }

}
