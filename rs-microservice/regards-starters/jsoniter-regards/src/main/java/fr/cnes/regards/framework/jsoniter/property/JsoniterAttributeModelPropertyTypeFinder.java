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

package fr.cnes.regards.framework.jsoniter.property;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModelBuilder;
import fr.cnes.regards.modules.model.domain.attributes.Fragment;
import fr.cnes.regards.modules.model.domain.event.AttributeModelCreated;
import fr.cnes.regards.modules.model.domain.event.AttributeModelDeleted;
import fr.cnes.regards.modules.model.domain.event.FragmentDeletedEvent;
import fr.cnes.regards.modules.model.dto.event.AttributeCacheRefreshEvent;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;
import fr.cnes.regards.modules.model.gson.IAttributeHelper;
import fr.cnes.regards.modules.model.gson.ModelJsoniterReadyEvent;
import io.vavr.Tuple;
import io.vavr.collection.HashMultimap;
import io.vavr.collection.List;
import io.vavr.collection.Multimap;
import io.vavr.collection.Traversable;
import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class JsoniterAttributeModelPropertyTypeFinder
    implements ApplicationListener<ApplicationStartedEvent>, AttributeModelPropertyTypeFinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsoniterAttributeModelPropertyTypeFinder.class);

    @Autowired
    private ApplicationEventPublisher publisher;

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
     * Helper class to initialize factory based on stored {@link AttributeModel}
     */
    @Autowired
    private IAttributeHelper attributeHelper;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private INotificationClient notifClient;

    @Value("spring.application.name")
    private String microserviceName;

    private volatile Multimap<String, AttributeModel> attributes = HashMultimap.withSet().empty();

    public Option<PropertyType> getPropertyTypeForAttributeWithName(String name) {
        String tenant = runtimeTenantResolver.getTenant();
        return attributes.get(tenant)
                         .flatMap(attrs -> findAttributeWithSameName(name,
                                                                     attrs).orElse(() -> findAttributeWithFragmentNamed(
                             name,
                             attrs)));
    }

    private Option<PropertyType> findAttributeWithSameName(String name, Traversable<AttributeModel> attrs) {
        return attrs.find(attr -> attr.getName().equals(name)).map(AttributeModel::getType);
    }

    private Option<PropertyType> findAttributeWithFragmentNamed(String name, Traversable<AttributeModel> attrs) {
        return attrs.find(attr -> !attr.getFragment().isDefaultFragment() && attr.getFragment().getName().equals(name))
                    .map(attr -> PropertyType.OBJECT);
    }

    @Override
    public void refresh(String tenant, java.util.List<AttributeModel> atts) {
        LOGGER.info("Registering already configured attributes and fragments for jsoniter decoders");
        // Register for tenant
        attributes = attributes.merge(multimapOf(tenant, List.ofAll(atts)));
        LOGGER.info("Registering attributes for jsoniter decoders for tenant {} done", tenant);
    }

    @Override
    public void onApplicationEvent(final ApplicationStartedEvent pEvent) {
        subscriber.subscribeTo(AttributeModelCreated.class, new RegisterHandler());
        subscriber.subscribeTo(AttributeModelDeleted.class, new UnregisterHandler());
        subscriber.subscribeTo(FragmentDeletedEvent.class, new UnregisterFragmentHandler());
        subscriber.subscribeTo(AttributeCacheRefreshEvent.class, new AttributeCacheRefreshHandler());
        // Retrieve all tenants
        for (final String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            try {
                LOGGER.info("Registering already configured attributes and fragments for jsoniter decoders");
                // Register for tenant
                final List<AttributeModel> atts = List.ofAll(attributeHelper.getAllAttributes(tenant));
                attributes = attributes.merge(multimapOf(tenant, atts));
                LOGGER.info("Registering attributes for jsoniter decoders for tenant {} done", tenant);
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
        publisher.publishEvent(new ModelJsoniterReadyEvent(this));
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

            String tenant = pWrapper.getTenant();
            Fragment fragment = new Fragment();
            fragment.setName(amc.getFragmentName());
            AttributeModel attributeModel = AttributeModelBuilder.build(amc.getAttributeName(),
                                                                        amc.getPropertyType(),
                                                                        null).fragment(fragment).get();

            attributes = attributes.merge(multimapOf(tenant, List.of(attributeModel)));
        }
    }

    /**
     * Handle {@link AttributeModel} deletion
     */
    private class UnregisterHandler implements IHandler<AttributeModelDeleted> {

        @Override
        public void handle(final TenantWrapper<AttributeModelDeleted> pWrapper) {
            AttributeModelDeleted amd = pWrapper.getContent();
            Fragment fragment = new Fragment();
            fragment.setName(amd.getFragmentName());
            AttributeModel attributeModel = AttributeModelBuilder.build(amd.getAttributeName(),
                                                                        amd.getPropertyType(),
                                                                        null).fragment(fragment).get();

            attributes = attributes.filterValues(am -> !am.equals(attributeModel));
        }
    }

    /**
     * Handle {@link Fragment} deletion
     */
    private class UnregisterFragmentHandler implements IHandler<FragmentDeletedEvent> {

        @Override
        public void handle(final TenantWrapper<FragmentDeletedEvent> pWrapper) {
            FragmentDeletedEvent fragmentDeleted = pWrapper.getContent();
            Fragment fragment = new Fragment();
            fragment.setName(fragmentDeleted.getFragmentName());
            attributes = attributes.filterValues(am -> am.hasFragment() && am.getFragment().equals(fragment));
        }
    }

    private class AttributeCacheRefreshHandler implements IHandler<AttributeCacheRefreshEvent> {

        @Override
        public void handle(TenantWrapper<AttributeCacheRefreshEvent> wrapper) {
            String tenant = wrapper.getTenant();
            final List<AttributeModel> atts = List.ofAll(attributeHelper.getAllAttributes(tenant));
            attributes = attributes.merge(multimapOf(tenant, atts));
            notifClient.notify(String.format(
                                   "Attribute cache refresh finished for microservice %s on project %s. %s attributes detected",
                                   microserviceName,
                                   tenant,
                                   attributes.size()),
                               String.format("[%s] Attribute cache refresh done", microserviceName),
                               NotificationLevel.INFO,
                               DefaultRole.ADMIN);
        }
    }

    private HashMultimap<String, AttributeModel> multimapOf(String tenant, List<AttributeModel> atts) {
        return HashMultimap.withSet().ofEntries(atts.map(m -> Tuple.of(tenant, m)));
    }

}
