/*
 *
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
package fr.cnes.regards.modules.notifier.service.conf;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.notifier.dto.conf.RuleRecipientsAssociation;
import fr.cnes.regards.modules.notifier.service.AbstractNotificationMultitenantServiceTest;
import fr.cnes.regards.modules.notifier.service.plugin.DefaultRuleMatcher;
import fr.cnes.regards.modules.notifier.service.plugin.RabbitMQSender;

@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=notifier_conf" })
public class NotificationConfigurationServiceTest extends AbstractNotificationMultitenantServiceTest {

    @Autowired
    private NotificationConfigurationService confService;

    @Autowired
    private IPluginService pluginService;

    @Test
    public void importNothing() throws ModuleException {
        confService.importConfiguration(null, null);
    }

    @Test(expected = EntityInvalidException.class)
    public void importInvalidRule() throws ModuleException {
        Set<PluginConfiguration> configurations = new HashSet<>();
        configurations.add(PluginConfiguration.build(DefaultRuleMatcher.PLUGIN_ID, "RULE1", IPluginParam.set()));
        confService.importConfiguration(configurations, null);
    }

    @Test
    public void importValidRule() throws ModuleException {
        Set<PluginConfiguration> configurations = new HashSet<>();
        String seek = "target";
        String seek_val = "val";
        PluginConfiguration rule1 = PluginConfiguration.build(DefaultRuleMatcher.PLUGIN_ID, "RULE1", IPluginParam
                .set(IPluginParam.build(DefaultRuleMatcher.ATTRIBUTE_TO_SEEK_FIELD_NAME, seek),
                     IPluginParam.build(DefaultRuleMatcher.ATTRIBUTE_VALUE_TO_SEEK_FIELD_NAME, seek_val)));
        configurations.add(rule1);
        confService.importConfiguration(configurations, null);

        PluginConfiguration result = pluginService.getPluginConfiguration(rule1.getBusinessId());
        Assert.assertEquals(seek, result.getParameterValue(DefaultRuleMatcher.ATTRIBUTE_TO_SEEK_FIELD_NAME));
        Assert.assertEquals(seek_val, result.getParameterValue(DefaultRuleMatcher.ATTRIBUTE_VALUE_TO_SEEK_FIELD_NAME));

        // Update
        seek = "targetUpdate";
        seek_val = "update";
        PluginConfiguration updateRule1 = PluginConfiguration
                .build(DefaultRuleMatcher.PLUGIN_ID, "RULE1 update", IPluginParam
                        .set(IPluginParam.build(DefaultRuleMatcher.ATTRIBUTE_TO_SEEK_FIELD_NAME, seek),
                             IPluginParam.build(DefaultRuleMatcher.ATTRIBUTE_VALUE_TO_SEEK_FIELD_NAME, seek_val)));
        updateRule1.setBusinessId(rule1.getBusinessId());
        configurations.clear();
        configurations.add(updateRule1);
        confService.importConfiguration(configurations, null);

        result = pluginService.getPluginConfiguration(rule1.getBusinessId());
        Assert.assertEquals(seek, result.getParameterValue(DefaultRuleMatcher.ATTRIBUTE_TO_SEEK_FIELD_NAME));
        Assert.assertEquals(seek_val, result.getParameterValue(DefaultRuleMatcher.ATTRIBUTE_VALUE_TO_SEEK_FIELD_NAME));
    }

    @Test
    public void importValidConfiguration() throws ModuleException {
        Set<PluginConfiguration> configurations = new HashSet<>();
        Set<RuleRecipientsAssociation> associations = new HashSet<>();

        PluginConfiguration rule1 = PluginConfiguration
                .build(DefaultRuleMatcher.PLUGIN_ID, "RULE1",
                       IPluginParam
                               .set(IPluginParam.build(DefaultRuleMatcher.ATTRIBUTE_TO_SEEK_FIELD_NAME, "target"),
                                    IPluginParam.build(DefaultRuleMatcher.ATTRIBUTE_VALUE_TO_SEEK_FIELD_NAME, "val")));
        configurations.add(rule1);

        PluginConfiguration recipient1 = PluginConfiguration
                .build(RabbitMQSender.PLUGIN_ID, "RECIPIENT1",
                       IPluginParam.set(IPluginParam.build(RabbitMQSender.EXCHANGE_PARAM_NAME, "exchange.test.name1"),
                                        IPluginParam.build(RabbitMQSender.QUEUE_PARAM_NAME, "queue.test.name1")));
        configurations.add(recipient1);

        PluginConfiguration recipient2 = PluginConfiguration
                .build(RabbitMQSender.PLUGIN_ID, "RECIPIENT2",
                       IPluginParam.set(IPluginParam.build(RabbitMQSender.EXCHANGE_PARAM_NAME, "exchange.test.name2"),
                                        IPluginParam.build(RabbitMQSender.QUEUE_PARAM_NAME, "queue.test.name2")));
        configurations.add(recipient2);

        // Association
        associations.add(RuleRecipientsAssociation
                .build(rule1.getBusinessId(), Sets.newHashSet(recipient1.getBusinessId(), recipient2.getBusinessId())));

        confService.importConfiguration(configurations, associations);
    }

    @Test(expected = ModuleException.class)
    public void importInvalidAssociation() throws ModuleException {
        Set<RuleRecipientsAssociation> associations = new HashSet<>();

        // Association
        associations.add(RuleRecipientsAssociation.build("source_rule", Sets.newHashSet("target_recipient")));

        confService.importConfiguration(null, associations);
    }

    @Test(expected = ModuleException.class)
    public void importInvalidAssociation2() throws ModuleException {
        Set<PluginConfiguration> configurations = new HashSet<>();
        Set<RuleRecipientsAssociation> associations = new HashSet<>();

        PluginConfiguration rule1 = PluginConfiguration
                .build(DefaultRuleMatcher.PLUGIN_ID, "RULE1",
                       IPluginParam
                               .set(IPluginParam.build(DefaultRuleMatcher.ATTRIBUTE_TO_SEEK_FIELD_NAME, "target"),
                                    IPluginParam.build(DefaultRuleMatcher.ATTRIBUTE_VALUE_TO_SEEK_FIELD_NAME, "val")));
        configurations.add(rule1);

        // Association
        associations.add(RuleRecipientsAssociation.build(rule1.getBusinessId(), Sets.newHashSet("target_recipient")));

        confService.importConfiguration(configurations, associations);
    }
}
