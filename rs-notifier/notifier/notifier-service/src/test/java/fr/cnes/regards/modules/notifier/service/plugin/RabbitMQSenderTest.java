/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.notifier.service.plugin;

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.notifier.domain.NotificationRequest;
import fr.cnes.regards.modules.notifier.domain.plugin.IRecipientNotifier;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.*;

/**
 * @author Léo Mieulet
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=rabbitmq_sender" })
@ActiveProfiles({ "noscheduler" })
public class RabbitMQSenderTest extends AbstractMultitenantServiceTest {

    @SpyBean
    protected IPublisher publisher;

    @Captor
    private ArgumentCaptor<String> exchangeNameCaptor;

    @Captor
    private ArgumentCaptor<Optional<String>> queueNameCaptor;

    @Captor
    private ArgumentCaptor<Optional<String>> routingKeyCaptor;

    @Captor
    private ArgumentCaptor<Optional<String>> dlkCaptor;

    @Captor
    private ArgumentCaptor<Integer> priorityCaptor;

    @Captor
    private ArgumentCaptor<Collection<?>> messagesCaptor;

    @Captor
    private ArgumentCaptor<Map<String, Object>> headersCaptor;

    @Test
    public void testRabbitMQSender() throws NotAvailablePluginConfigurationException {
        Mockito.clearInvocations(publisher);

        PluginUtils.setup();
        String exchange = "exchange";
        String queueName = "queueName";
        String recipientLabel = "recipientLabel";
        boolean ackRequired = true;
        // Plugin parameters
        Set<IPluginParam> parameters = IPluginParam.set(
                IPluginParam.build(AbstractRabbitMQSender.EXCHANGE_PARAM_NAME, exchange),
                IPluginParam.build(AbstractRabbitMQSender.QUEUE_PARAM_NAME, queueName),
                IPluginParam.build(AbstractRabbitMQSender.RECIPIENT_LABEL_PARAM_NAME, recipientLabel),
                IPluginParam.build(RabbitMQSender.ACK_REQUIRED_PARAM_NAME, ackRequired));

        // Instantiate plugin
        IRecipientNotifier plugin = PluginUtils.getPlugin(
                PluginConfiguration.build(RabbitMQSender.class, UUID.randomUUID().toString(), parameters),
                new HashMap<>());
        Assert.assertNotNull(plugin);

        // Run plugin
        Collection<NotificationRequest> requests = Lists.newArrayList(new NotificationRequest());
        plugin.send(requests);

        Assert.assertEquals("should retrieve ack", ackRequired, plugin.isAckRequired());
        Mockito.verify(publisher, Mockito.times(1))
                .broadcastAll(exchangeNameCaptor.capture(), queueNameCaptor.capture(), routingKeyCaptor.capture(), dlkCaptor.capture(),
                              priorityCaptor.capture(), messagesCaptor.capture(), headersCaptor.capture());
        Assert.assertEquals("should retrieve good exchange", exchange, exchangeNameCaptor.getValue());
        Assert.assertEquals("should retrieve good queue name", Optional.of(queueName), queueNameCaptor.getValue());
        Assert.assertFalse("should not override routing key", routingKeyCaptor.getValue().isPresent());
        Assert.assertFalse("should not override DLK", dlkCaptor.getValue().isPresent());
        Assert.assertEquals("should retrieve default priority", Integer.valueOf(0), priorityCaptor.getValue());
        Assert.assertFalse("should send a message", messagesCaptor.getValue().isEmpty());
        Assert.assertTrue("should don't override headers", headersCaptor.getValue().isEmpty());
    }
}
