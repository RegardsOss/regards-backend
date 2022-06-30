/*
 * Copyright 2017-20XX CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.notifier.service;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.notifier.dao.IRecipientErrorRepository;
import fr.cnes.regards.modules.notifier.domain.plugin.RecipientSender3;
import fr.cnes.regards.modules.notifier.dto.internal.NotifierClearCacheEvent;
import fr.cnes.regards.modules.notifier.mock.InMemoryPluginService;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static fr.cnes.regards.modules.notifier.service.PluginConfigurationTestBuilder.aPlugin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class RecipientServiceTest {

    private static final String RECIPIENT_1 = "recipient1";

    private static final String RECIPIENT_2 = "recipient2";

    private static final String UNKNOWN_RECIPIENT = "unknown";

    private final InMemoryPluginService pluginService;

    private final IRecipientErrorRepository recipientErrors;

    private final IRecipientService recipientService;

    private final IPublisher publisher;

    public RecipientServiceTest() {
        recipientErrors = mock(IRecipientErrorRepository.class);
        pluginService = new InMemoryPluginService();
        publisher = mock(IPublisher.class);
        recipientService = new RecipientService(pluginService, recipientErrors, null, publisher);
    }

    @Test
    public void create_recipient_saves_its_plugin() throws Exception {
        PluginConfiguration firstRecipient = aRecipient(RECIPIENT_1);

        recipientService.createOrUpdate(firstRecipient);

        Set<PluginConfiguration> recipients = recipientService.getRecipients(Collections.singleton(RECIPIENT_1));
        assertThat(recipients).containsExactly(firstRecipient);

        Mockito.verify(publisher, Mockito.times(1)).publish(Mockito.any(NotifierClearCacheEvent.class));
    }

    @Test
    public void update_recipient_updates_its_plugin() throws Exception {
        PluginConfiguration firstRecipient = aRecipient(RECIPIENT_1);
        recipientService.createOrUpdate(firstRecipient);

        PluginConfiguration updatedRecipient = aRecipient(RECIPIENT_1);
        updatedRecipient.setId(1L);
        updatedRecipient.setVersion("2");
        recipientService.createOrUpdate(updatedRecipient);

        Set<PluginConfiguration> recipients = recipientService.getRecipients(Collections.singleton(RECIPIENT_1));
        assertThat(recipients).hasSize(1).map(PluginConfiguration::getVersion).containsExactly("2");

        Mockito.verify(publisher, Mockito.times(2)).publish(Mockito.any(NotifierClearCacheEvent.class));
    }

    @Test
    public void getRecipients_returns_all_recipients() throws Exception {
        PluginConfiguration firstRecipient = aRecipient(RECIPIENT_1);
        PluginConfiguration secondRecipient = aRecipient(RECIPIENT_2);
        recipientService.createOrUpdate(firstRecipient);
        recipientService.createOrUpdate(secondRecipient);

        Set<PluginConfiguration> allRecipients = recipientService.getRecipients();

        assertThat(allRecipients).hasSize(2).contains(firstRecipient, secondRecipient);
    }

    @Test
    public void get_recipients_by_ids_ignore_unknown_ids() throws Exception {
        PluginConfiguration firstRecipient = aRecipient(RECIPIENT_1);
        PluginConfiguration secondRecipient = aRecipient(RECIPIENT_2);
        recipientService.createOrUpdate(firstRecipient);
        recipientService.createOrUpdate(secondRecipient);

        List<String> recipients = Arrays.asList(UNKNOWN_RECIPIENT, RECIPIENT_1, RECIPIENT_2);
        Set<PluginConfiguration> allRecipients = recipientService.getRecipients(recipients);

        assertThat(allRecipients).hasSize(2).contains(firstRecipient, secondRecipient);
    }

    @Test
    public void delete_all_recipients_returns_all_plugins_to_delete() throws Exception {
        PluginConfiguration firstRecipient = aRecipient(RECIPIENT_1);
        PluginConfiguration secondRecipient = aRecipient(RECIPIENT_2);
        recipientService.createOrUpdate(firstRecipient);
        recipientService.createOrUpdate(secondRecipient);

        recipientService.deleteAll();

        Assertions.assertThatExceptionOfType(EntityNotFoundException.class)
                  .isThrownBy(() -> pluginService.getPluginConfiguration(RECIPIENT_1));
        Assertions.assertThatExceptionOfType(EntityNotFoundException.class)
                  .isThrownBy(() -> pluginService.getPluginConfiguration(RECIPIENT_2));

        Mockito.verify(publisher, Mockito.times(3)).publish(Mockito.any(NotifierClearCacheEvent.class));
    }

    @Test
    public void delete_all_recipients_clear_recipient_errors() throws Exception {
        PluginConfiguration firstRecipient = aRecipient(RECIPIENT_1);
        PluginConfiguration secondRecipient = aRecipient(RECIPIENT_2);
        recipientService.createOrUpdate(firstRecipient);
        recipientService.createOrUpdate(secondRecipient);

        recipientService.deleteAll();

        verify(recipientErrors).deleteByRecipientBusinessId(RECIPIENT_1);
        verify(recipientErrors).deleteByRecipientBusinessId(RECIPIENT_2);

        Mockito.verify(publisher, Mockito.times(3)).publish(Mockito.any(NotifierClearCacheEvent.class));
    }

    @Test
    public void delete_recipient_removes_its_plugin() throws Exception {
        PluginConfiguration firstRecipient = aRecipient(RECIPIENT_1);
        PluginConfiguration secondRecipient = aRecipient(RECIPIENT_2);
        recipientService.createOrUpdate(firstRecipient);
        recipientService.createOrUpdate(secondRecipient);

        recipientService.delete(RECIPIENT_1);

        Set<PluginConfiguration> recipients = recipientService.getRecipients();
        assertThat(recipients).hasSize(1).contains(secondRecipient);

        Mockito.verify(publisher, Mockito.times(2)).publish(Mockito.any(NotifierClearCacheEvent.class));
    }

    @Test
    public void delete_recipient_remove_its_errors() throws Exception {
        PluginConfiguration firstRecipient = aRecipient(RECIPIENT_1);
        recipientService.createOrUpdate(firstRecipient);

        recipientService.delete(RECIPIENT_1);

        // Twice for creations and once for deletion
        verify(recipientErrors).deleteByRecipientBusinessId(RECIPIENT_1);

        Mockito.verify(publisher, Mockito.times(1)).publish(Mockito.any(NotifierClearCacheEvent.class));
    }

    private PluginConfiguration aRecipient(String withName) {
        return aPlugin().identified(withName).named(withName).withPluginId(RecipientSender3.PLUGIN_ID).build();
    }
}
