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

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.module.manager.ModuleConfiguration;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.StringPluginParam;
import fr.cnes.regards.modules.notifier.domain.plugin.RecipientSender3;
import fr.cnes.regards.modules.notifier.domain.plugin.RecipientSender5;
import fr.cnes.regards.modules.notifier.dto.RuleDTO;
import fr.cnes.regards.modules.notifier.service.conf.NotificationConfigurationManager;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

/**
 * @author sbinda
 *
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=rules", "regards.amqp.enabled=false",
        "spring.jpa.properties.hibernate.jdbc.batch_size=1024", "spring.jpa.properties.hibernate.order_inserts=true" })
public class RuleServiceTest extends AbstractNotificationMultitenantServiceTest {

    @Autowired
    private NotificationConfigurationManager manager;

    @Test
    public void manageRuleWithRecipientsConfigurationTest() throws ModuleException, IOException {

        String recipient1bid = "recipient1";
        String recipient2bid = "recipient2";
        String rulebid = "rule1";
        // 1. Create new rule configuration with one recipient
        PluginConfiguration rulePlugin = new PluginConfiguration();
        rulePlugin.setBusinessId(rulebid);
        rulePlugin.setVersion("1.0.0");
        rulePlugin.setLabel("createConfiguration");
        rulePlugin.setPluginId("DefaultRuleMatcher");

        StringPluginParam param = IPluginParam.build("attributeToSeek", "nature");
        rulePlugin.getParameters().add(param);
        param = IPluginParam.build("attributeValueToSeek", "TM");
        rulePlugin.getParameters().add(param);

        PluginConfiguration recipientPlugin = new PluginConfiguration();
        recipientPlugin.setBusinessId(recipient1bid);
        recipientPlugin.setVersion("1.0.0");
        recipientPlugin.setLabel("recipient1");
        recipientPlugin.setPluginId(RecipientSender3.PLUGIN_ID);
        recipientService.createOrUpdateRecipient(recipientPlugin);

        PluginConfiguration recipientPlugin2 = new PluginConfiguration();
        recipientPlugin2.setBusinessId(recipient2bid);
        recipientPlugin2.setVersion("1.0.0");
        recipientPlugin2.setLabel("recipient2");
        recipientPlugin2.setPluginId(RecipientSender5.PLUGIN_ID);
        recipientService.createOrUpdateRecipient(recipientPlugin2);

        Assert.assertEquals(1, recipientService.getRecipients(Sets.newHashSet(recipient1bid)).size());
        Assert.assertEquals(1, recipientService.getRecipients(Sets.newHashSet(recipient2bid)).size());
        Assert.assertEquals(2, recipientService.getRecipients().size());

        RuleDTO toCreate = RuleDTO.build(rulePlugin, Sets.newHashSet(recipient1bid));
        ruleService.createOrUpdateRule(toCreate);

        Optional<RuleDTO> created = ruleService.getRule(rulebid);
        Assert.assertTrue(created.isPresent());
        Assert.assertEquals("createConfiguration", created.get().getRulePluginConfiguration().getLabel());
        Assert.assertEquals(1, created.get().getRecipientsBusinessIds().size());

        // Try to update configuration
        RuleDTO toUpdate = created.get();
        toUpdate.getRulePluginConfiguration().setLabel("newOne");
        toUpdate.getRecipientsBusinessIds().add(recipient2bid);
        ruleService.createOrUpdateRule(toUpdate);
        created = ruleService.getRule(rulebid);
        Assert.assertTrue(created.isPresent());
        Assert.assertEquals("newOne", created.get().getRulePluginConfiguration().getLabel());
        Assert.assertEquals(2, created.get().getRecipientsBusinessIds().size());
        // Export configuration
        ModuleConfiguration exportedConf = manager.exportConfiguration();

        toUpdate.getRulePluginConfiguration().setLabel("newOne");
        toUpdate.getRecipientsBusinessIds().clear();
        toUpdate.getRecipientsBusinessIds().add(recipient2bid);
        ruleService.createOrUpdateRule(toUpdate);
        created = ruleService.getRule(rulebid);
        Assert.assertTrue(created.isPresent());
        Assert.assertEquals("newOne", created.get().getRulePluginConfiguration().getLabel());
        Assert.assertEquals(1, created.get().getRecipientsBusinessIds().size());

        // Export configuration
        ModuleConfiguration exportedConf2 = manager.exportConfiguration();

        // Try to delete configuration
        ruleService.deleteRule(rulebid);
        created = ruleService.getRule(rulebid);
        Assert.assertFalse(created.isPresent());
        recipientService.deleteRecipient(recipient1bid);
        recipientService.deleteRecipient(recipient2bid);
        Assert.assertEquals(0, recipientService.getRecipients(Sets.newHashSet(recipient1bid, recipient2bid)).size());

        // Import configuration
        manager.importConfigurationAndLog(exportedConf);
        created = ruleService.getRule(rulebid);
        Assert.assertTrue(created.isPresent());
        Assert.assertEquals(2, created.get().getRecipientsBusinessIds().size());
        Assert.assertEquals(2, recipientService.getRecipients(Sets.newHashSet(recipient1bid, recipient2bid)).size());

        // Import second time should update
        manager.importConfigurationAndLog(exportedConf2);
        created = ruleService.getRule(rulebid);
        Assert.assertTrue(created.isPresent());
        Assert.assertEquals(1, created.get().getRecipientsBusinessIds().size());
        Assert.assertEquals(2, recipientService.getRecipients(Sets.newHashSet(recipient1bid, recipient2bid)).size());

        // Try delete a recipient associated to an existing rule
        recipientService.deleteRecipient(recipient1bid);
        recipientService.deleteRecipient(recipient2bid);
        created = ruleService.getRule(rulebid);
        Assert.assertTrue(created.isPresent());
        Assert.assertEquals(0, created.get().getRecipientsBusinessIds().size());
        Assert.assertEquals(0, recipientService.getRecipients().size());

        ruleService.deleteAll(new ArrayList<>());
        recipientService.deleteAll(new ArrayList<>());

    }

}
