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
package fr.cnes.regards.modules.notifier.service.conf;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.dto.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.notifier.domain.plugin.IRecipientNotifier;
import fr.cnes.regards.modules.notifier.domain.plugin.IRuleMatcher;
import fr.cnes.regards.modules.notifier.dto.RuleDTO;
import fr.cnes.regards.modules.notifier.dto.conf.RuleRecipientsAssociation;
import fr.cnes.regards.modules.notifier.service.IRuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RegardsTransactional
public class NotificationConfigurationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationConfigurationManager.class);

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IRuleService ruleService;

    public void importConfiguration(Set<PluginConfiguration> configurations,
                                    Set<RuleRecipientsAssociation> associations) throws ModuleException {

        Map<String, PluginConfiguration> rulePluginConfs = new HashMap<>();
        Class<IRuleMatcher> ruleClass = IRuleMatcher.class;
        List<PluginConfiguration> existingRulePluginConfs = pluginService.getPluginConfigurationsByType(ruleClass);

        Map<String, PluginConfiguration> recipientPluginConfs = new HashMap<>();
        Class<IRecipientNotifier> recipientClass = IRecipientNotifier.class;
        List<PluginConfiguration> existingRecipientPluginConfs = pluginService.getPluginConfigurationsByType(
            recipientClass);

        // Dispatch existing configurations
        existingRulePluginConfs.forEach(p -> rulePluginConfs.put(p.getBusinessId(), p));
        existingRecipientPluginConfs.forEach(p -> recipientPluginConfs.put(p.getBusinessId(), p));

        // Check type and create or update configuration
        if (configurations != null) {
            for (PluginConfiguration conf : configurations) {
                PluginMetaData pluginMeta = PluginUtils.getPluginMetadata(conf.getPluginId());
                if (pluginMeta == null) {
                    String errorMessage = String.format("Unknown plugin id %s", conf.getPluginId());
                    LOGGER.error(errorMessage);
                    throw new ModuleException(errorMessage);
                }
                if (pluginMeta.getInterfaceNames().contains(ruleClass.getName())) {
                    // Manage rules
                    createOrUpdate(rulePluginConfs, conf);
                } else if (pluginMeta.getInterfaceNames().contains(recipientClass.getName())) {
                    // Manage recipients
                    createOrUpdate(recipientPluginConfs, conf);
                } else {
                    String errorMessage = String.format(
                        "Expecting %s or %s plugin type but found %s for plugin with business id %s",
                        ruleClass.getName(),
                        recipientClass.getName(),
                        pluginMeta.getInterfaceNames(),
                        conf.getBusinessId());
                    LOGGER.error(errorMessage);
                    throw new ModuleException(errorMessage);
                }
            }
        }

        // All rules and recipients are saved, now check and import associations
        if (associations != null) {
            for (RuleRecipientsAssociation asso : associations) {

                // Check target rule
                PluginConfiguration ruleConf = rulePluginConfs.get(asso.getRuleId());
                if (ruleConf == null) {
                    String errorMessage = String.format(
                        "Unknown RULE plugin business id %s in rule/recipient association",
                        asso.getRuleId());
                    LOGGER.error(errorMessage);
                    throw new ModuleException(errorMessage);
                }

                // Check target recipient(s)
                for (String recipientLabel : asso.getRecipientIds()) {
                    PluginConfiguration recipientConf = recipientPluginConfs.get(recipientLabel);
                    if (recipientConf == null) {
                        String errorMessage = String.format(
                            "Unknown RECIPIENT plugin business id %s in association with RULE %s",
                            recipientLabel,
                            asso.getRuleId());
                        LOGGER.error(errorMessage);
                        throw new ModuleException(errorMessage);
                    }
                }

                // Create or update association
                ruleService.createOrUpdate(RuleDTO.build(ruleConf, asso.getRecipientIds()));
            }
        }
    }

    private void createOrUpdate(Map<String, PluginConfiguration> existing, PluginConfiguration conf)
        throws ModuleException {
        PluginConfiguration existingOne = existing.get(conf.getBusinessId());
        if (existingOne != null) {
            LOGGER.info("Updating existing plugin configuration {}", conf.getBusinessId());
            existingOne.setLabel(conf.getLabel());
            existingOne.setParameters(conf.getParameters());
            existingOne.setIsActive(conf.isActive());
            pluginService.updatePluginConfiguration(existingOne);
        } else {
            // Add new configuration to existing ones
            existing.put(conf.getBusinessId(), pluginService.savePluginConfiguration(conf));
            LOGGER.info("New plugin configuration {} created of type {}",
                        conf.getBusinessId(),
                        conf.getInterfaceNames());
        }
    }
}
