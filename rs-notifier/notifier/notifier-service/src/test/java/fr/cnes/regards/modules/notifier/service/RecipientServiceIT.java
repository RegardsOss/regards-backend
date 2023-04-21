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
package fr.cnes.regards.modules.notifier.service;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.notifier.domain.plugin.AbstractRecipientSender;
import fr.cnes.regards.modules.notifier.domain.plugin.RecipientSender10;
import fr.cnes.regards.modules.notifier.domain.plugin.RecipientSender3;
import fr.cnes.regards.modules.notifier.domain.plugin.RecipientSender5;
import fr.cnes.regards.modules.notifier.dto.RecipientDto;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.Set;

import static fr.cnes.regards.modules.notifier.service.PluginConfigurationTestBuilder.aPlugin;

/**
 * @author Stephane Cortine
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=notifier_recipients",
                                   "regards.amqp.enabled=false",
                                   "spring.jpa.properties.hibernate.jdbc.batch_size=1024",
                                   "spring.jpa.properties.hibernate.order_inserts=true" })
public class RecipientServiceIT extends AbstractNotificationMultitenantServiceIT {

    private static final String RECIPIENT_1 = "recipient1";

    private static final String RECIPIENT_2 = "recipient2";

    private static final String RECIPIENT_3 = "recipient10";

    private PluginConfiguration firstRecipient;

    private PluginConfiguration secondRecipient;

    private PluginConfiguration thirdRecipient;

    @Before
    public void initializeData() throws Exception {
        firstRecipient = aPlugin().identified(RECIPIENT_1)
                                  .named(RECIPIENT_1)
                                  .withPluginId(RecipientSender3.PLUGIN_ID)
                                  .build();

        secondRecipient = aPlugin().identified(RECIPIENT_2)
                                   .named(RECIPIENT_2)
                                   .withPluginId(RecipientSender5.PLUGIN_ID)
                                   .build();

        thirdRecipient = aPlugin().identified(RECIPIENT_3)
                                  .named(RECIPIENT_3)
                                  .withPluginId(RecipientSender10.PLUGIN_ID)
                                  .build();

        pluginService.savePluginConfiguration(firstRecipient);
        pluginService.savePluginConfiguration(secondRecipient);
        pluginService.savePluginConfiguration(thirdRecipient);
    }

    @Test
    public void test_find_all_recipients() {
        Set<RecipientDto> recipientDtos = recipientService.findRecipients(null);

        Assert.assertNotNull(recipientDtos);
        Assert.assertEquals(3, recipientDtos.size());
    }

    @Test
    public void test_find_all_recipients_with_direct_notification() {
        Set<RecipientDto> recipientDtos = recipientService.findRecipients(Boolean.TRUE);

        Assert.assertNotNull(recipientDtos);
        Assert.assertEquals(1, recipientDtos.size());
        Assert.assertTrue(recipientDtos.contains(new RecipientDto(RECIPIENT_3,
                                                                  AbstractRecipientSender.RECIPIENT_LABEL,
                                                                  RecipientSender10.DESCRIPTION)));
    }

    @Test
    public void test_find_all_recipients_without_direct_notification() {
        Set<RecipientDto> recipientDtos = recipientService.findRecipients(Boolean.FALSE);

        Assert.assertNotNull(recipientDtos);
        Assert.assertEquals(2, recipientDtos.size());
        Assert.assertTrue(recipientDtos.contains(new RecipientDto(RECIPIENT_1,
                                                                  AbstractRecipientSender.RECIPIENT_LABEL,
                                                                  "")));
        Assert.assertTrue(recipientDtos.contains(new RecipientDto(RECIPIENT_2,
                                                                  AbstractRecipientSender.RECIPIENT_LABEL,
                                                                  "")));
    }
}
