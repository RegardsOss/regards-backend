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


package fr.cnes.regards.config;

import fr.cnes.regards.framework.amqp.event.notifier.NotificationRequestEvent;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.notifier.client.INotifierClient;
import fr.cnes.regards.modules.notifier.client.INotifierRequestListener;
import fr.cnes.regards.modules.notifier.dto.out.NotificationState;
import fr.cnes.regards.modules.notifier.dto.out.NotifierEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * @author Iliana Ghazali
 */
@Configuration
public class FeaturePerformanceITConfig {

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Bean
    @Primary
    public INotifierClient notifierClient(INotifierRequestListener notifierRequestListener) {
        return new INotifierClient() {

            @Override
            public void sendNotifications(List<NotificationRequestEvent> notification) {
                ExecutorService executorToMockSubscribeThread = Executors.newSingleThreadExecutor();
                executorToMockSubscribeThread.submit(() -> {
                    runtimeTenantResolver.forceTenant("PROJECT");
                    notifierRequestListener.onRequestSuccess(notification.stream()
                                                                         .map(notifEvent -> new NotifierEvent(notifEvent.getRequestId(),
                                                                                                              notifEvent.getRequestOwner(),
                                                                                                              NotificationState.SUCCESS,
                                                                                                              OffsetDateTime.now()))
                                                                         .collect(Collectors.toList()));
                });
            }
        };
    }
}
