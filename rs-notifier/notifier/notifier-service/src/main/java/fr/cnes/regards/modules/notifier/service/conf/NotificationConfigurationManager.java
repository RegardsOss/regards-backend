/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.module.manager.AbstractModuleManager;
import fr.cnes.regards.framework.module.manager.ModuleConfiguration;
import fr.cnes.regards.framework.module.manager.ModuleConfigurationItem;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.notifier.dto.RuleDTO;
import fr.cnes.regards.modules.notifier.dto.conf.RuleRecipientsAssociation;
import fr.cnes.regards.modules.notifier.service.INotificationRuleService;
import fr.cnes.regards.modules.notifier.service.IRecipientService;
import fr.cnes.regards.modules.notifier.service.IRuleService;

/**
 * Configuration manager for current module
 * @author Sébastien Binda
 */
@Component
public class NotificationConfigurationManager extends AbstractModuleManager<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationConfigurationManager.class);

    @Autowired
    private INotificationRuleService notifService;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IRuleService ruleService;

    @Autowired
    private IRecipientService recipientService;

    @Override
    public Set<String> resetConfiguration() {
        Set<String> errors = Sets.newHashSet();
        recipientService.deleteAll(errors);
        ruleService.deleteAll(errors);
        return errors;
    }

    @Override
    protected Set<String> importConfiguration(ModuleConfiguration configuration) {

        Set<String> importErrors = new HashSet<>();
        Set<PluginConfiguration> configurations = getPluginConfs(configuration.getConfiguration());
        Set<RuleRecipientsAssociation> rules = getRulesRecipientsAssoc(configuration.getConfiguration());

        // Clear cache
        notifService.cleanCache();

        // import plugin configurations
        for (PluginConfiguration plgConf : configurations) {
            try {
                Optional<PluginConfiguration> existingOne = loadPluginConfiguration(plgConf.getBusinessId());
                if (existingOne.isPresent()) {
                    // if override configuration we have to delete existing rule using this configuration
                    if (configuration.isResetBeforeImport()) {
                        this.ruleService.cleanRulesUsingConfiguration(plgConf);
                        this.notifService.cleanNotificationErrorsUsingConfiguration(plgConf);
                    }
                    LOGGER.info("Updating existing plugin configuration {}", plgConf.getBusinessId());
                    existingOne.get().setLabel(plgConf.getLabel());
                    existingOne.get().setParameters(plgConf.getParameters());
                    pluginService.updatePluginConfiguration(existingOne.get());
                } else {
                    LOGGER.info("Creating new plugin configuration {}", plgConf.getBusinessId());
                    pluginService.savePluginConfiguration(plgConf);
                }
            } catch (ModuleException e) {
                LOGGER.warn(IMPORT_FAIL_MESSAGE, e);
                importErrors.add(e.getMessage());
            }
        }

        // Now import rule recipients associations
        for (RuleRecipientsAssociation rule : rules) {
            try {
                PluginConfiguration ruleConf = pluginService.getPluginConfiguration(rule.getRuleId());
                ruleService.createOrUpdateRule(RuleDTO.build(ruleConf, rule.getRecipientIds()));
            } catch (ModuleException e) {
                LOGGER.warn(IMPORT_FAIL_MESSAGE, e);
                importErrors.add(e.getMessage());
            }
        }

        return importErrors;
    }

    private Optional<PluginConfiguration> loadPluginConfiguration(String businessId) {
        PluginConfiguration existingOne = null;
        try {
            existingOne = pluginService.getPluginConfiguration(businessId);
        } catch (EntityNotFoundException e) { // NOSONAR
            // Nothing to do, plugin configuration does not exists.
        }
        return Optional.ofNullable(existingOne);
    }

    @Override
    public ModuleConfiguration exportConfiguration() throws ModuleException {
        List<ModuleConfigurationItem<?>> configurations = new ArrayList<>();

        // export Recipients
        for (PluginConfiguration factory : pluginService.getAllPluginConfigurations()) {
            // All connection should be active
            PluginConfiguration exportedConf = pluginService.prepareForExport(factory);
            exportedConf.setIsActive(true);
            configurations.add(ModuleConfigurationItem.build(exportedConf));
        }

        // Export rule recipient associations
        Page<RuleDTO> rules = ruleService.getRules(PageRequest.of(0, 100_000));
        configurations.addAll(rules.getContent().stream()
                .map(r -> ModuleConfigurationItem.build(toRuleRecipientsAssoc(r))).collect(Collectors.toSet()));

        return ModuleConfiguration.build(info, true, configurations);
    }

    /**
     * Get all {@link PluginConfiguration}s of the {@link ModuleConfigurationItem}s
     * @param items {@link ModuleConfigurationItem}s
     * @return  {@link PluginConfiguration}s
     */
    private Set<PluginConfiguration> getPluginConfs(Collection<ModuleConfigurationItem<?>> items) {
        return items.stream().filter(i -> PluginConfiguration.class.isAssignableFrom(i.getKey()))
                .map(i -> (PluginConfiguration) i.getTypedValue()).collect(Collectors.toSet());
    }

    private Set<RuleRecipientsAssociation> getRulesRecipientsAssoc(Collection<ModuleConfigurationItem<?>> items) {
        return items.stream().filter(i -> RuleRecipientsAssociation.class.isAssignableFrom(i.getKey()))
                .map(i -> (RuleRecipientsAssociation) i.getTypedValue()).collect(Collectors.toSet());
    }

    private RuleRecipientsAssociation toRuleRecipientsAssoc(RuleDTO rule) {
        return RuleRecipientsAssociation.build(rule.getRulePluginConfiguration().getBusinessId(),
                                               rule.getRecipientsBusinessIds());
    }
}
