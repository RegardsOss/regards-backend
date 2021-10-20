/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.notifier.service;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.modules.notifier.dao.INotificationRequestRepository;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;

public abstract class AbstractNotificationService<T extends AbstractNotificationService<T>> {

    protected static final String OPTIMIST_LOCK_LOG_MSG = "Another schedule has updated some of the requests handled by this method while it was running.";

    protected ApplicationContext applicationContext;
    protected INotificationRequestRepository notificationRequestRepository;
    protected IPublisher publisher;
    protected T self;

    protected AbstractNotificationService(INotificationRequestRepository notificationRequestRepository, IPublisher publisher, ApplicationContext applicationContext) {
        this.notificationRequestRepository = notificationRequestRepository;
        this.publisher = publisher;
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void post() {
        self = ((T) applicationContext.getBean(this.getClass()));
    }

}
