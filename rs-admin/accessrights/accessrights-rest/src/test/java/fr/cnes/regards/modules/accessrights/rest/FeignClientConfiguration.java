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
package fr.cnes.regards.modules.accessrights.rest;

import javax.mail.internet.MimeMessage;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

import fr.cnes.regards.modules.accessrights.instance.client.IAccountSettingsClient;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;

/**
 * @author Marc Sordi
 *
 */
@Configuration
public class FeignClientConfiguration {

    @Bean
    public JavaMailSender mockSender() {
        final JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
        Mockito.when(mailSender.createMimeMessage()).thenReturn(Mockito.mock(MimeMessage.class));
        return mailSender;
    }

    @Bean
    public IAccountsClient accountsClient() {
        return Mockito.mock(IAccountsClient.class);
    }

    @Bean
    public IAccountSettingsClient accountSettingsClient() {
        return Mockito.mock(IAccountSettingsClient.class);
    }

    @Bean
    public IProjectsClient projectsClient() {
        return Mockito.mock(IProjectsClient.class);
    }
}
