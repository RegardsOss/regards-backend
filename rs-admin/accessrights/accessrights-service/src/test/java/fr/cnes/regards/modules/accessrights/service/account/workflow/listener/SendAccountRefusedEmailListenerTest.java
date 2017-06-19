/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.account.workflow.listener;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mail.SimpleMailMessage;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.service.account.workflow.events.OnRefuseAccountEvent;
import fr.cnes.regards.modules.accessrights.service.account.workflow.listeners.SendAccountRefusedEmailListener;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.templates.service.ITemplateService;

/**
 *
 * @author Xavier-Alexandre Brochard
 */
public class SendAccountRefusedEmailListenerTest {

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.service.account.workflow.listeners.SendAccountRefusedEmailListener#onApplicationEvent(fr.cnes.regards.modules.accessrights.service.account.workflow.events.OnRefuseAccountEvent)}.
     * @throws EntityNotFoundException
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void testOnApplicationEvent_templateNotFound() throws EntityNotFoundException {
        Account account = new Account("email@test.com", "firstname", "lastname", "password");
        OnRefuseAccountEvent event = new OnRefuseAccountEvent(account);

        ITemplateService templateService = Mockito.mock(ITemplateService.class);
        IEmailClient emailClient = Mockito.mock(IEmailClient.class);
        Mockito.when(templateService.writeToEmail(Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
                .thenThrow(EntityNotFoundException.class);

        SendAccountRefusedEmailListener listener = new SendAccountRefusedEmailListener(templateService, emailClient);
        listener.onApplicationEvent(event);

        Mockito.verify(emailClient).sendEmail(Mockito.any());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.service.account.workflow.listeners.SendAccountRefusedEmailListener#onApplicationEvent(fr.cnes.regards.modules.accessrights.service.account.workflow.events.OnRefuseAccountEvent)}.
     * @throws EntityNotFoundException
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void testOnApplicationEvent() throws EntityNotFoundException {
        Account account = new Account("email@test.com", "firstname", "lastname", "password");
        OnRefuseAccountEvent event = new OnRefuseAccountEvent(account);

        ITemplateService templateService = Mockito.mock(ITemplateService.class);
        IEmailClient emailClient = Mockito.mock(IEmailClient.class);
        Mockito.when(templateService.writeToEmail(Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
                .thenReturn(new SimpleMailMessage());

        SendAccountRefusedEmailListener listener = new SendAccountRefusedEmailListener(templateService, emailClient);
        listener.onApplicationEvent(event);

        Mockito.verify(emailClient).sendEmail(Mockito.any());
    }

}
