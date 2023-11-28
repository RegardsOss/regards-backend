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
package fr.cnes.regards.modules.notifier.service;

import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.cnes.regards.framework.amqp.configuration.AmqpChannel;
import fr.cnes.regards.framework.amqp.configuration.IAmqpAdmin;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.notifier.dao.INotificationRequestRepository;
import fr.cnes.regards.modules.notifier.dao.IRecipientErrorRepository;
import fr.cnes.regards.modules.notifier.dao.IRuleRepository;
import fr.cnes.regards.modules.notifier.domain.plugin.RecipientSender3;
import fr.cnes.regards.modules.notifier.domain.plugin.RecipientSenderFail;
import fr.cnes.regards.modules.notifier.dto.RuleDTO;
import fr.cnes.regards.modules.notifier.dto.out.Recipient;
import fr.cnes.regards.modules.notifier.mock.NotificationProcessingServiceMock;
import fr.cnes.regards.modules.notifier.service.conf.NotificationConfigurationProperties;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpIOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.HashSet;

import static fr.cnes.regards.modules.notifier.service.PluginConfigurationTestBuilder.aPlugin;
import static org.junit.Assert.fail;

public abstract class AbstractNotificationMultitenantServiceIT extends AbstractMultitenantServiceIT {

    protected static final int RECIPIENTS_PER_RULE = 10;

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractNotificationMultitenantServiceIT.class);

    // used to param if the test Recipient will fail
    public static boolean RECIPIENT_FAIL = true;

    protected final String REQUEST_OWNER = this.getClass().getSimpleName();

    @Autowired
    protected IRuleRepository ruleRepo;

    @Autowired
    protected IRuleService ruleService;

    @Autowired
    protected IPluginConfigurationRepository pluginConfRepo;

    @Autowired
    protected INotificationRequestRepository notificationRequestRepository;

    @Autowired
    protected IRecipientErrorRepository recipientErrorRepo;

    @Autowired
    protected IJobInfoRepository jobInforepo;

    @Autowired
    protected IPluginService pluginService;

    @Autowired
    protected IRecipientService recipientService;

    @Autowired
    protected IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired(required = false)
    protected IAmqpAdmin amqpAdmin;

    @Autowired(required = false)
    protected IRabbitVirtualHostAdmin vhostAdmin;

    @Autowired
    protected NotificationConfigurationProperties configuration;

    @Autowired
    protected Gson gson;

    @Autowired
    protected RuleCache ruleCache;

    @SpyBean
    protected NotificationProcessingServiceMock notificationProcessingService;

    @SpyBean
    protected NotificationMatchingService notificationMatchingService;

    @SpyBean
    protected NotificationRegistrationService notificationRegistrationService;

    @Before
    public void before() throws Exception {
        RECIPIENT_FAIL = true;
        this.ruleCache.clear();
        this.recipientErrorRepo.deleteAll();
        this.notificationRequestRepository.deleteAll();
        this.ruleRepo.deleteAll();
        this.pluginConfRepo.deleteAll();
        this.jobInforepo.deleteAll();
        simulateApplicationReadyEvent();
        initMockProxyBeans();
    }

    public void initMockProxyBeans() {
        ReflectionTestUtils.setField(notificationProcessingService, "self", notificationProcessingService);
        ReflectionTestUtils.setField(notificationMatchingService, "self", notificationMatchingService);
        ReflectionTestUtils.setField(notificationRegistrationService, "self", notificationRegistrationService);
    }

    /**
     * Internal method to clean AMQP queues, if actives
     */
    public void cleanAMQPQueues(Class<? extends IHandler<?>> handler, Target target) {
        if (vhostAdmin != null) {
            // Re-set tenant because above simulation clear it!

            // Purge event queue
            try {
                vhostAdmin.bind(AmqpChannel.AMQP_MULTITENANT_MANAGER);
                amqpAdmin.purgeQueue(amqpAdmin.getSubscriptionQueueName(handler, target), false);
            } catch (AmqpIOException e) {
                //todo
            } finally {
                vhostAdmin.unbind();
            }
        }
    }

    /**
     * Wait data creation on a {@link JpaRepository}
     *
     * @param repo           {@link JpaRepository} where we wait data
     * @param expectedNumber number of data waited
     * @param timeout        in seconds to throw exception
     */
    public void waitDatabaseCreation(JpaRepository<?, ?> repo, int expectedNumber, int timeout)
        throws InterruptedException {
        long notificationActionNumber = 0;
        int cpt = 0;
        do {
            notificationActionNumber = repo.count();
            cpt++;
            Thread.sleep(1000);
        } while ((notificationActionNumber != expectedNumber) && (cpt != timeout));

        if (notificationActionNumber != expectedNumber) {
            fail(String.format("Wrong notifications number in database after timeout expected %s got %s",
                               expectedNumber,
                               notificationActionNumber));
        }
    }

    /**
     * Init 1 rule and RECIPIENTS_PER_RULE {@link Recipient}, one of the {@link Recipient} will fail
     * if the param fail is set to true
     */
    protected void initPlugins(boolean fail) throws ModuleException {
        // configuration of the rule plugin
        // configuration of the default recipient sender plugin
        String id = fail ? "failRecipient" : "testRecipient";
        String pluginId = fail ? RecipientSenderFail.PLUGIN_ID : RecipientSender3.PLUGIN_ID;
        PluginConfiguration recipientPlugin = aPlugin().identified(id)
                                                       .named("test recipient")
                                                       .withPluginId(pluginId)
                                                       .build();
        pluginService.savePluginConfiguration(recipientPlugin);
        Collection<String> recipientIds = new HashSet<>();
        recipientIds.add(recipientPlugin.getBusinessId());

        // configuration of the fake recipient sender (for test)
        // TODO Pourquoi créer autant de Recipients ? 2 suffiraient a priori
        for (int i = 1; i < RECIPIENTS_PER_RULE; i++) {
            PluginConfiguration rp = aPlugin().identified("testRecipient" + (i + 1))
                                              .named("test recipient")
                                              .inVersion("1.0.0")
                                              .withPluginId("RecipientSender" + (i + 1))
                                              .build();
            pluginService.savePluginConfiguration(rp);
            recipientIds.add(rp.getBusinessId());
        }

        PluginConfiguration rulePlugin = aPlugin().identified("testRule")
                                                  .named("test")
                                                  .withPluginId("DefaultRuleMatcher")
                                                  .parameterized_by(IPluginParam.build("attributeToSeek", "nature"))
                                                  .parameterized_by(IPluginParam.build("attributeValueToSeek", "TM"))
                                                  .build();
        ruleService.createOrUpdate(RuleDTO.build(rulePlugin, recipientIds));
    }

    /**
     * load {@link JsonElement} from a resource test
     *
     * @return initialised {@link JsonElement}
     */
    protected JsonObject initElement(String name) {
        try (InputStream input = this.getClass().getResourceAsStream(name);
            Reader reader = new InputStreamReader(input)) {

            return gson.fromJson(CharStreams.toString(reader), JsonObject.class);
        } catch (IOException e) {
            String errorMessage = "Cannot import element";
            LOGGER.debug(errorMessage);
            throw new AssertionError(errorMessage);
        }
    }
}

