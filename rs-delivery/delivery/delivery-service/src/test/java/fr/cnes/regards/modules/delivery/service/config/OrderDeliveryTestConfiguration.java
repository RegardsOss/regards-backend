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
package fr.cnes.regards.modules.delivery.service.config;

import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.delivery.service.config.mock.OrderDataFileAvailableClientMock;
import fr.cnes.regards.modules.delivery.service.config.mock.OrderDataFileClientMock;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for IT tests
 *
 * @author Iliana Ghazali
 **/
@Configuration
public class OrderDeliveryTestConfiguration {

    @MockBean
    private IProjectUsersClient projectUsersClient;

    @Bean
    public OrderDataFileClientMock dataFileClientMock() {
        return new OrderDataFileClientMock();
    }

    @Bean
    public OrderDataFileAvailableClientMock dataFileAvailableClientMock() {
        return new OrderDataFileAvailableClientMock();
    }

}
