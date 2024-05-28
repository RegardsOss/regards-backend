/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.modules.emails.dao.EmailRequestRepository;
import fr.cnes.regards.modules.emails.domain.EmailRequest;
import fr.cnes.regards.modules.notification.service.IInstanceNotificationService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import nl.altindag.log.LogCaptor;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test class for {@link EmailRequestService}.
 *
 * @author Stephane Cortine
 */
@RunWith(MockitoJUnitRunner.class)
public class EmailRequestServiceTest {

    private final static int FIRST_RANGE_DELAY_TRY_SEND = 3600;

    private final static int SECOND_RANGE_DELAY_TRY_SEND = 86400;

    private final static int THIRD_RANGE_DELAY_TRY_SEND = 259200;

    @InjectMocks
    private EmailRequestService emailRequestService;

    @Mock
    private EmailRequestRepository emailRequestRepository;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private IInstanceNotificationService instanceNotificationService;

    @Before
    public void setup() {
        ReflectionTestUtils.setField(emailRequestService, "firstRangeDelayTrySend", FIRST_RANGE_DELAY_TRY_SEND);
        ReflectionTestUtils.setField(emailRequestService, "secondRangeDelayTrySend", SECOND_RANGE_DELAY_TRY_SEND);
        ReflectionTestUtils.setField(emailRequestService, "thirdRangeDelayTrySend", THIRD_RANGE_DELAY_TRY_SEND);

        ReflectionTestUtils.setField(emailRequestService, "defaultSender", "regardsTest@noreply.fr");
    }

    @Test
    public void sendEmail_throw_MailSendException_first_attempt() {
        // Given
        EmailRequest initialEmailRequest = createEmailRequest(0);
        int initialNbUnsuccessfullTry = initialEmailRequest.getNbUnsuccessfullTry();
        OffsetDateTime initialNextTryDate = initialEmailRequest.getNextTryDate();
        when(emailRequestRepository.findByNextTryDateBefore(any())).thenReturn(List.of(initialEmailRequest));

        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        doThrow(MailSendException.class).when(mailSender).send((MimeMessage) any());

        ArgumentCaptor<EmailRequest> emailRequestCaptor = ArgumentCaptor.forClass(EmailRequest.class);

        // When
        emailRequestService.sendEmail();

        // Then
        verify(emailRequestRepository, never()).delete(any());
        verify(emailRequestRepository, times(1)).save(emailRequestCaptor.capture());
        verify(instanceNotificationService, times(1)).createNotification(any());

        EmailRequest finalEmailRequest = emailRequestCaptor.getValue();

        Assert.assertEquals(initialNbUnsuccessfullTry + 1, finalEmailRequest.getNbUnsuccessfullTry());
        Assert.assertEquals(initialNextTryDate.plusSeconds(FIRST_RANGE_DELAY_TRY_SEND),
                            finalEmailRequest.getNextTryDate());
    }

    @Test
    public void sendEmail_throw_MailSendException_third_attempt() {
        // Given
        EmailRequest initialEmailRequest = createEmailRequest(3);
        int initialNbUnsuccessfullTry = initialEmailRequest.getNbUnsuccessfullTry();
        OffsetDateTime initialNextTryDate = initialEmailRequest.getNextTryDate();
        when(emailRequestRepository.findByNextTryDateBefore(any())).thenReturn(List.of(initialEmailRequest));

        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        doThrow(MailSendException.class).when(mailSender).send((MimeMessage) any());

        ArgumentCaptor<EmailRequest> emailRequestCaptor = ArgumentCaptor.forClass(EmailRequest.class);

        // When
        emailRequestService.sendEmail();

        // Then
        verify(emailRequestRepository, never()).delete(any());
        verify(emailRequestRepository, times(1)).save(emailRequestCaptor.capture());
        verify(instanceNotificationService, times(1)).createNotification(any());

        EmailRequest finalEmailRequest = emailRequestCaptor.getValue();

        Assert.assertEquals(initialNbUnsuccessfullTry + 1, finalEmailRequest.getNbUnsuccessfullTry());
        Assert.assertEquals(initialNextTryDate.plusSeconds(SECOND_RANGE_DELAY_TRY_SEND),
                            finalEmailRequest.getNextTryDate());
    }

    @Test
    public void sendEmail_throw_MailSendException_sixth_attempt() {
        // Given
        EmailRequest initialEmailRequest = createEmailRequest(6);
        int initialNbUnsuccessfullTry = initialEmailRequest.getNbUnsuccessfullTry();
        OffsetDateTime initialNextTryDate = initialEmailRequest.getNextTryDate();
        when(emailRequestRepository.findByNextTryDateBefore(any())).thenReturn(List.of(initialEmailRequest));

        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        doThrow(MailSendException.class).when(mailSender).send((MimeMessage) any());

        ArgumentCaptor<EmailRequest> emailRequestCaptor = ArgumentCaptor.forClass(EmailRequest.class);

        // When
        emailRequestService.sendEmail();

        // Then
        verify(emailRequestRepository, never()).delete(any());
        verify(emailRequestRepository, times(1)).save(emailRequestCaptor.capture());
        verify(instanceNotificationService, times(1)).createNotification(any());

        EmailRequest finalEmailRequest = emailRequestCaptor.getValue();

        Assert.assertEquals(initialNbUnsuccessfullTry + 1, finalEmailRequest.getNbUnsuccessfullTry());
        Assert.assertEquals(initialNextTryDate.plusSeconds(THIRD_RANGE_DELAY_TRY_SEND),
                            finalEmailRequest.getNextTryDate());
    }

    @Test
    public void sendEmail() {
        // Given
        EmailRequest initialEmailRequest = createEmailRequest(0);
        when(emailRequestRepository.findByNextTryDateBefore(any())).thenReturn(List.of(initialEmailRequest));

        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        ArgumentCaptor<EmailRequest> emailRequestCaptor = ArgumentCaptor.forClass(EmailRequest.class);

        // When
        emailRequestService.sendEmail();

        // Then
        verify(mailSender, times(1)).send((MimeMessage) any());
        verify(emailRequestRepository, times(1)).delete(emailRequestCaptor.capture());
        verify(emailRequestRepository, never()).save(any());
        verify(instanceNotificationService, never()).createNotification(any());

        Assert.assertEquals(initialEmailRequest, emailRequestCaptor.getValue());
    }

    @Test
    public void sendEmail_with_max_nbUnsuccessfullTry() {
        // Given
        EmailRequest initialEmailRequest = createEmailRequest(EmailRequest.MAX_UNSUCCESSFULL_TRY);
        when(emailRequestRepository.findByNextTryDateBefore(any())).thenReturn(List.of(initialEmailRequest));

        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        doThrow(MailSendException.class).when(mailSender).send((MimeMessage) any());

        LogCaptor logCaptor = LogCaptor.forClass(EmailRequestService.class);

        ArgumentCaptor<EmailRequest> emailRequestCaptor = ArgumentCaptor.forClass(EmailRequest.class);

        // When
        emailRequestService.sendEmail();

        // Then
        verify(emailRequestRepository, times(1)).delete(emailRequestCaptor.capture());
        verify(instanceNotificationService, times(1)).createNotification(any());
        verify(emailRequestRepository, never()).save(any());

        Assertions.assertThat(logCaptor.getWarnLogs()).hasSize(1);
        Assertions.assertThat(logCaptor.getErrorLogs()).hasSize(1);

        Assert.assertEquals(initialEmailRequest, emailRequestCaptor.getValue());
    }

    private EmailRequest createEmailRequest(int nbUnsuccessfullTry) {
        EmailRequest emailRequest = new EmailRequest();
        emailRequest.setTo(new String[0]);
        emailRequest.setFrom("regardsTest@cs.fr");
        emailRequest.setNbUnsuccessfullTry(nbUnsuccessfullTry);
        emailRequest.setNextTryDate(OffsetDateTime.now());
        emailRequest.setText("messageTest");

        return emailRequest;
    }

}
