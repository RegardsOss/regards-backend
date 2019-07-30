/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.amqp.configuration;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.listener.exception.ListenerExecutionFailedException;
import org.springframework.util.ErrorHandler;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import fr.cnes.regards.framework.amqp.IInstancePublisher;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.notification.NotificationEvent;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.notification.NotificationDtoBuilder;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.security.role.DefaultRole;

/**
 * @author Marc SORDI
 *
 */
public class RegardsErrorHandler implements ErrorHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegardsErrorHandler.class);

    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    private static final String AMQP_DLQ_FAILURE_MESSAGE = "AMQP message failure";

    private final IInstancePublisher instancePublisher;

    private final IPublisher publisher;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final String microserviceName;

    // Basic JSON parser
    private final JsonParser parser = new JsonParser();

    public RegardsErrorHandler(IRuntimeTenantResolver runtimeTenantResolver, IInstancePublisher instancePublisher,
            IPublisher publisher, String microserviceName) {
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.instancePublisher = instancePublisher;
        this.publisher = publisher;
        this.microserviceName = microserviceName;
    }

    /**
     * This method notifies AMQP failure to INSTANCE_ADMIN and if possible to PROJECT_ADMIN.<br/>
     * To notify PROJECT_ADMIN, this method uses {@link TenantWrapper#getTenant()} property to retrieve the target tenant.
     */
    @Override
    public void handleError(Throwable t) {

        // Try to notify message
        if (ListenerExecutionFailedException.class.isAssignableFrom(t.getClass())) {
            ListenerExecutionFailedException lefe = (ListenerExecutionFailedException) t;
            if (lefe.getFailedMessage() != null) {

                LOGGER.error("AMQP failed message : {}", lefe.getFailedMessage().toString());

                // Build event
                // Message#toString is already handling encoding and content type if possible
                String message = "AMQP message has been routed to DLQ (dead letter queue).";
                Set<String> roles = new HashSet<>(Arrays.asList(DefaultRole.PROJECT_ADMIN.toString()));
                NotificationEvent event = NotificationEvent.build(new NotificationDtoBuilder(message,
                        AMQP_DLQ_FAILURE_MESSAGE, NotificationLevel.ERROR, microserviceName).toRoles(roles));

                // Publish to INSTANCE ADMIN
                instancePublisher.publish(event);

                // Try to publish to PROJECT_ADMIN looking for tenant to properly route the notification
                try (Reader json = new InputStreamReader(new ByteArrayInputStream(lefe.getFailedMessage().getBody()),
                        DEFAULT_CHARSET)) {
                    JsonElement el = parser.parse(json);
                    if (el.isJsonObject()) {
                        JsonObject o = el.getAsJsonObject();
                        String tenant = o.get("tenant").getAsString();
                        LOGGER.trace("Tenant {} detected", tenant);

                        try {
                            // Route notification to the right tenant
                            runtimeTenantResolver.forceTenant(tenant);
                            publisher.publish(event);
                        } finally {
                            runtimeTenantResolver.clearTenant();
                        }
                    }
                } catch (Exception e) { // NOSONAR avoid any exception trying to retrieve tenant
                    // Cannot retrieve tenant if message cannot be parsed
                    LOGGER.warn("Cannot parse AMQP message, skipping project admin notification", e);
                }
            }
        } else {
            NotificationDtoBuilder notifBuilder = new NotificationDtoBuilder(t.getMessage(), AMQP_DLQ_FAILURE_MESSAGE,
                    NotificationLevel.ERROR, microserviceName);
            Set<String> roles = new HashSet<>();
            roles.add(DefaultRole.PROJECT_ADMIN.name());
            // Publish to INSTANCE ADMIN
            instancePublisher.publish(NotificationEvent.build(notifBuilder.toRoles(roles)));
        }

        LOGGER.error("Listener failed - message rerouted to dead letter queue", t);
    }

}
