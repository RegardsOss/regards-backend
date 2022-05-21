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
package fr.cnes.regards.modules.accessrights.instance.service.workflow.listener;

import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.service.workflow.events.OnRefuseAccountEvent;
import fr.cnes.regards.modules.accessrights.instance.service.workflow.listeners.SendAccountRefusedEmailListener;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.templates.service.ITemplateService;
import freemarker.template.TemplateException;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Xavier-Alexandre Brochard
 */
public class SendAccountRefusedEmailListenerTest {

    /**
     * Test method for {@link SendAccountRefusedEmailListener#onApplicationEvent(OnRefuseAccountEvent)}.
     */
    @Test
    public final void testOnApplicationEvent() throws TemplateException {
        Account account = new Account("email@test.com", "firstname", "lastname", "password");
        OnRefuseAccountEvent event = new OnRefuseAccountEvent(account);

        ITemplateService templateService = Mockito.mock(ITemplateService.class);
        IEmailClient emailClient = Mockito.mock(IEmailClient.class);
        Mockito.when(templateService.render(Mockito.anyString(), Mockito.anyMap())).thenReturn("");

        SendAccountRefusedEmailListener listener = new SendAccountRefusedEmailListener(templateService, emailClient);
        listener.onApplicationEvent(event);

        Mockito.verify(emailClient).sendEmail(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

}
