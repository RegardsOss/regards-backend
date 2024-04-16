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

import fr.cnes.regards.framework.encryption.IEncryptionService;
import fr.cnes.regards.framework.encryption.exception.EncryptionException;
import fr.cnes.regards.framework.module.manager.ModuleConfiguration;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.IPluginParam;
import fr.cnes.regards.modules.notifier.domain.plugin.PluginWithSensitiveParam;
import fr.cnes.regards.modules.notifier.domain.plugin.RecipientSender3;
import fr.cnes.regards.modules.notifier.domain.plugin.RecipientSender5;
import fr.cnes.regards.modules.notifier.dto.RuleDTO;
import fr.cnes.regards.modules.notifier.service.conf.NotificationConfigurationManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static fr.cnes.regards.modules.notifier.service.PluginConfigurationTestBuilder.aPlugin;
import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=rules",
                                   "regards.amqp.enabled=false",
                                   "spring.jpa.properties.hibernate.jdbc.batch_size=1024",
                                   "spring.jpa.properties.hibernate.order_inserts=true" })
public class ImportConfigurationIT extends AbstractNotificationMultitenantServiceIT {

    public static final String RECIPIENT_1 = "recipient1";

    public static final String RECIPIENT_2 = "recipient2";

    public static final String RULE_1 = "rule1";

    @Autowired
    private NotificationConfigurationManager manager;

    private PluginConfiguration firstRecipient;

    private PluginConfiguration secondRecipient;

    private PluginConfiguration firstRule;

    @Autowired
    private IEncryptionService encryptionService;

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

        firstRule = aPlugin().identified(RULE_1)
                             .named("createConfiguration")
                             .withPluginId("DefaultRuleMatcher")
                             .parameterized_by(IPluginParam.build("attributeToSeek", "nature"))
                             .parameterized_by(IPluginParam.build("attributeValueToSeek", "TM"))
                             .build();
    }

    @Test
    public void import_configuration_create_elements_in_repository() throws Exception {
        pluginService.savePluginConfiguration(firstRecipient);
        pluginService.savePluginConfiguration(secondRecipient);
        List<String> recipients = Arrays.asList(RECIPIENT_1, RECIPIENT_2);
        RuleDTO ruleToCreate = RuleDTO.build(firstRule, new HashSet<>(recipients));
        ruleService.createOrUpdate(ruleToCreate);

        ModuleConfiguration exportedConf = manager.exportConfiguration();
        ruleService.deleteAll();
        recipientService.deleteAll();
        manager.importConfigurationAndLog(exportedConf);

        assertThat(recipientService.getRecipients()).map(PluginConfiguration::getBusinessId)
                                                    .contains(RECIPIENT_1, RECIPIENT_2);
        Optional<RuleDTO> actualRule = ruleService.getRule(RULE_1);
        assertThat(actualRule).isPresent();
        assertThat(actualRule.get().getRecipientsBusinessIds()).containsExactly(RECIPIENT_1, RECIPIENT_2);
    }

    @Test
    public void import_configuration_update_existing_elements_in_repository() throws Exception {
        pluginService.savePluginConfiguration(firstRecipient);
        pluginService.savePluginConfiguration(secondRecipient);
        List<String> recipients = Arrays.asList(RECIPIENT_1, RECIPIENT_2);
        RuleDTO ruleToCreate = RuleDTO.build(firstRule, new HashSet<>(recipients));
        ruleService.createOrUpdate(ruleToCreate);
        ModuleConfiguration exportedConf = manager.exportConfiguration();
        RuleDTO toUpdate = ruleService.getRule(RULE_1)
                                      .orElseThrow(() -> new AssertionError("rule is not found in repository"));
        toUpdate.getRecipientsBusinessIds().clear();
        toUpdate.getRecipientsBusinessIds().add(RECIPIENT_2);
        ruleService.createOrUpdate(toUpdate);
        ModuleConfiguration exportedConf2 = manager.exportConfiguration();
        ruleService.deleteAll();
        recipientService.deleteAll();

        manager.importConfigurationAndLog(exportedConf);
        manager.importConfigurationAndLog(exportedConf2);

        Optional<RuleDTO> actualRule = ruleService.getRule(RULE_1);
        assertThat(actualRule).isPresent();
        assertThat(actualRule.get().getRecipientsBusinessIds()).containsExactly(RECIPIENT_2);
    }

    @Test
    public void test_sensitive_information_well_encrypted() throws ModuleException {
        // GIVEN a plugin with a sensitive param
        String pluginLabel = "myLabel";
        String sensitiveValue = "password";
        PluginConfiguration sensitivePlugin = new PluginConfiguration();
        sensitivePlugin.setLabel(pluginLabel);
        sensitivePlugin.setPluginId(PluginWithSensitiveParam.PLUGIN_ID);
        sensitivePlugin.getParameters()
                       .add(IPluginParam.build(PluginWithSensitiveParam.SENSITIVE_PARAM_NAME, sensitiveValue));

        // WHEN creating this plugin
        pluginService.createOrUpdatePluginConfiguration(sensitivePlugin);
        // THEN retrieve the plugin conf
        Optional<PluginConfiguration> pluginConfGetByServiceOpt = pluginService.findPluginConfigurationByLabel(
            pluginLabel);
        Assertions.assertTrue(pluginConfGetByServiceOpt.isPresent());
        // THEN the sensitive param is well encrypted
        PluginConfiguration pluginConfigurationAfterSave = pluginConfGetByServiceOpt.get();
        Assertions.assertEquals(sensitiveValue,
                                encryptionService.decrypt(pluginConfigurationAfterSave.getParameter(
                                    PluginWithSensitiveParam.SENSITIVE_PARAM_NAME).getValue().toString()));

        // GIVEN modified sensitive param
        String sensitiveValueChanged = sensitiveValue + "_modified";
        // GIVEN moduleConfiguration modified with new sensitive param
        ModuleConfiguration moduleConfiguration = manager.exportConfiguration();
        moduleConfiguration.setResetBeforeImport(false); // don't reset, keep existing
        PluginConfiguration pluginConfiguration = (PluginConfiguration) moduleConfiguration.getConfiguration()
                                                                                           .get(0)
                                                                                           .getValue();
        pluginConfiguration.getParameters().clear();
        pluginConfiguration.getParameters()
                           .add(IPluginParam.build(PluginWithSensitiveParam.SENSITIVE_PARAM_NAME,
                                                   sensitiveValueChanged));

        // WHEN importing module configuration
        manager.importConfigurationAndLog(moduleConfiguration);
        // THEN retrieve the plugin conf
        pluginConfGetByServiceOpt = pluginService.findPluginConfigurationByLabel(pluginLabel);
        Assertions.assertTrue(pluginConfGetByServiceOpt.isPresent());
        // THEN The sensitive param is well encrypted and is different than before
        PluginConfiguration pluginConfigurationAfterUpdate = pluginConfGetByServiceOpt.get();
        String newPasswordStored = pluginConfigurationAfterUpdate.getParameter(PluginWithSensitiveParam.SENSITIVE_PARAM_NAME)
                                                                 .getValue()
                                                                 .toString();
        try {
            encryptionService.decrypt(newPasswordStored);
        } catch (EncryptionException e) {
            Assertions.fail("Decrypt operation should be possible");
        }
        Assertions.assertEquals(sensitiveValueChanged, encryptionService.decrypt(newPasswordStored));
    }

}
