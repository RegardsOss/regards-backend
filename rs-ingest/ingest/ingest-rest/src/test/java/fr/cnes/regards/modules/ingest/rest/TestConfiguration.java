/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.rest;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.modules.storage.client.IAipClient;
import fr.cnes.regards.modules.storage.client.IAipEntityClient;

@Configuration
@ComponentScan(basePackages = { "fr.cnes.regards.modules" })
public class TestConfiguration {

    @Bean
    public IAipClient mockAIPClient() {
        return Mockito.mock(IAipClient.class);
    }

    @Bean
    public IAipEntityClient mockAIPEntityClient() {
        return Mockito.mock(IAipEntityClient.class);
    }

    @Bean
    public INotificationClient notificationClient() {
        return Mockito.mock(INotificationClient.class);
    }
}
