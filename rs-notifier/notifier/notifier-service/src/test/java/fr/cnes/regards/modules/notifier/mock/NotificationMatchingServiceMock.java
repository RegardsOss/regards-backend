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
package fr.cnes.regards.modules.notifier.mock;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.modules.notifier.dao.INotificationRequestRepository;
import fr.cnes.regards.modules.notifier.service.NotificationMatchingService;
import fr.cnes.regards.modules.notifier.service.conf.NotificationConfigurationProperties;
import lombok.Getter;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Primary bean of {@link NotificationMatchingService} to prevent failure on proxy bean.
 * SpyBean annotation does not handle injection of proxy bean yet.
 *
 * @author Iliana Ghazali
 **/
@Primary
@Service
@Getter
@Profile("test")
public class NotificationMatchingServiceMock extends NotificationMatchingService {

    public NotificationMatchingServiceMock(INotificationRequestRepository notificationRequestRepository,
                                           IPublisher publisher,
                                           IPluginService pluginService,
                                           NotificationConfigurationProperties properties,
                                           INotificationClient notificationClient,
                                           NotificationMatchingService notificationMatchingService) {
        super(notificationRequestRepository,
              publisher,
              pluginService,
              properties,
              notificationClient,
              notificationMatchingService);
    }
}
