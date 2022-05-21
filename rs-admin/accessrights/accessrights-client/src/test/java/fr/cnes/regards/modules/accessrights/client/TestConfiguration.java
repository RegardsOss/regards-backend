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
package fr.cnes.regards.modules.accessrights.client;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author sbinda
 */
@Configuration
public class TestConfiguration {

    @Bean
    IAuthenticationResolver resolver() {
        return Mockito.mock(IAuthenticationResolver.class);
    }

    @Bean
    IRuntimeTenantResolver tenantResolver() {
        return Mockito.mock(IRuntimeTenantResolver.class);
    }

    @Bean
    ISubscriber subscriber() {
        return Mockito.mock(ISubscriber.class);
    }

    @Bean
    MockCounter mockCounter() {
        return new MockCounter();
    }

    @Bean
    IRolesClient rolesClient(MockCounter mockCounter) {
        return new RoleClientMock(mockCounter);
    }

}
