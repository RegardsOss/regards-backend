/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.client.cache;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.security.event.RoleEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

/**
 * Listen to {@link RoleEvent}s to clear {@link RolesHierarchyKeyGenerator} cache if needed.
 *
 * @author Sébastien Binda
 */
public class RoleEventHandler implements ApplicationListener<ApplicationReadyEvent> {

    /**
     * Class logger
     */
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(RoleEventHandler.class);

    private final ISubscriber subscriber;

    private final IRolesHierarchyKeyGenerator rolesKeyGen;

    public RoleEventHandler(ISubscriber subscriber, IRolesHierarchyKeyGenerator rolesKeyGen) {
        super();
        this.subscriber = subscriber;
        this.rolesKeyGen = rolesKeyGen;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent pEvent) {
        // Listen to tenant creation
        subscriber.subscribeTo(RoleEvent.class, new RoleEventListener());
    }

    private class RoleEventListener implements IHandler<RoleEvent> {

        @Override
        public void handle(TenantWrapper<RoleEvent> pWrapper) {
            rolesKeyGen.cleanCache();
        }
    }
}
