/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.notifier.service.flow;

/**
 * Default amqp queue used by default recipient plugin
 * @author kevin
 *
 */
//@Component
//@Profile("!nohandler")
//public class NotificationEventHandler
//        implements IBatchHandler<NotificationEvent>, ApplicationListener<ApplicationReadyEvent> {
//
//    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationEventHandler.class);
//
//    @Autowired
//    private ISubscriber subscriber;
//
//    @Override
//    public void onApplicationEvent(ApplicationReadyEvent event) {
//        subscriber.subscribeTo(NotificationEvent.class, this);
//
//    }
//
//    @Override
//    public boolean validate(String tenant, NotificationEvent message) {
//        return true;
//
//    }
//
//    @Override
//    public void handleBatch(String tenant, List<NotificationEvent> messages) {
//        LOGGER.debug(String.format("Notification handler reception of %s messages", messages.size()));
//    }
//
//}
