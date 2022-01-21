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
package fr.cnes.regards.framework.modules.session.commons.service.delete;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.modules.session.commons.domain.events.SourceDeleteEvent;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

/**
 * Handle deletion of source
 *
 * @author Iliana Ghazali
 **/
public class SourceDeleteEventHandler
        implements ApplicationListener<ApplicationReadyEvent>, IHandler<SourceDeleteEvent> {

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private List<ISourceDeleteService> sourceDeleteServices;

    private static final Logger LOGGER = LoggerFactory.getLogger(SourceDeleteEventHandler.class);

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(SourceDeleteEvent.class, this);
    }

    @Override
    public void handle(String tenant, SourceDeleteEvent message) {
        runtimeTenantResolver.forceTenant(tenant);
        try {
            String source = message.getSource();
            long start = System.currentTimeMillis();

            LOGGER.trace("Handling deleting of source {} for tenant {}", source, tenant);
            for(ISourceDeleteService sourceDeleteService : sourceDeleteServices) {
                sourceDeleteService.deleteSource(source);
            }
            LOGGER.trace("Deleting of source {} for tenant {} handled in {}ms", source, tenant,
                         start - System.currentTimeMillis());
        } finally {
            runtimeTenantResolver.clearTenant();

        }
    }
}
