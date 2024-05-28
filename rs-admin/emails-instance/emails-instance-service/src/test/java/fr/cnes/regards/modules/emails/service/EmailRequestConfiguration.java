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
package fr.cnes.regards.modules.emails.service;

import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.notification.dao.INotificationLightRepository;
import fr.cnes.regards.modules.notification.dao.INotificationRepository;
import jakarta.mail.internet.MimeMessage;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Mock {@link JavaMailSender}
 *
 * @author Marc Sordi
 */
@Configuration
public class EmailRequestConfiguration {

    @Bean
    public JavaMailSender mockJavaMailSender() {
        final JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
        Mockito.when(mailSender.createMimeMessage()).thenReturn(Mockito.mock(MimeMessage.class));

        return mailSender;
    }

    @Bean
    public INotificationLightRepository mockNotificationLightRepository() {
        return Mockito.mock(INotificationLightRepository.class);
    }

    @Bean
    public INotificationRepository mockNotificationRepository() {
        return Mockito.mock(INotificationRepository.class);
    }

    @Bean
    public IEmailClient mockEmailClient() {
        return Mockito.mock(IEmailClient.class);
    }
}
