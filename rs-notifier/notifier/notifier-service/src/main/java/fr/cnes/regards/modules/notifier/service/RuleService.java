/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.notifier.dao.INotificationRequestRepository;
import fr.cnes.regards.modules.notifier.dao.IRuleRepository;
import fr.cnes.regards.modules.notifier.domain.Rule;
import fr.cnes.regards.modules.notifier.dto.RuleDTO;
import fr.cnes.regards.modules.notifier.dto.internal.NotifierClearCacheEvent;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation for rule service
 *
 * @author Kevin Marchois
 */
@Service
@MultitenantTransactional
public class RuleService implements IRuleService {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(RuleService.class);

    private final INotificationRequestRepository notifRepo;

    private final IRuleRepository ruleRepo;

    private final IRecipientService recipientService;

    private final IPluginService pluginService;

    private final IPublisher publisher;

    public RuleService(INotificationRequestRepository notifRepo,
                       IRuleRepository ruleRepo,
                       IRecipientService recipientService,
                       IPluginService pluginService,
                       IPublisher publisher) {
        this.notifRepo = notifRepo;
        this.ruleRepo = ruleRepo;
        this.recipientService = recipientService;
        this.pluginService = pluginService;
        this.publisher = publisher;
    }

    @Override
    public Page<RuleDTO> getRules(Pageable page) {
        Page<Rule> rules = ruleRepo.findAll(page);
        return new PageImpl<>(rules.get().map(this::toRuleDTO).toList());
    }

    @Override
    public Optional<RuleDTO> getRule(String fromBusinessId) {
        return ruleRepo.findByRulePluginBusinessId(fromBusinessId).map(this::toRuleDTO);
    }

    @Override
    public RuleDTO createOrUpdate(@Valid RuleDTO newRule) throws ModuleException {
        Optional<Rule> currentRule = ruleRepo.findByRulePluginBusinessId(newRule.getId());
        return currentRule.isPresent() ? update(currentRule.get(), newRule) : create(newRule);
    }

    private RuleDTO create(RuleDTO newRule) throws ModuleException {
        PluginConfiguration newPlugin = pluginService.savePluginConfiguration(newRule.getRulePluginConfiguration());
        Set<PluginConfiguration> newRecipients = recipientService.getRecipients(newRule.getRecipientsBusinessIds());

        clearCache();

        Rule toSave = Rule.build(newPlugin, newRecipients);
        return toRuleDTO(ruleRepo.save(toSave));
    }

    private RuleDTO update(Rule current, RuleDTO newRule) throws ModuleException {
        PluginConfiguration newPlugin = pluginService.updatePluginConfiguration(newRule.getRulePluginConfiguration());
        Set<PluginConfiguration> newRecipients = recipientService.getRecipients(newRule.getRecipientsBusinessIds());

        clearCache();

        Rule toSave = Rule.build(newPlugin, newRecipients);
        toSave.setId(current.getId());
        return toRuleDTO(ruleRepo.save(toSave));
    }

    @Override
    public void recipientUpdated(String recipientId) {
        clearCache();
    }

    @Override
    public void recipientDeleted(String recipientId) {
        for (Rule rule : ruleRepo.findByRecipientsBusinessId(recipientId)) {
            //TODO maybe better to use update method
            rule.getRecipients().removeIf(c -> c.getBusinessId().equals(recipientId));
            ruleRepo.save(rule);
        }
        clearCache();
    }

    @Override
    public void delete(String fromId) throws ModuleException {
        // TODO Should we remove notification for this Rule ?
        // It is done in deleteAll
        ruleRepo.deleteByRulePluginBusinessId(fromId);
        pluginService.deletePluginConfiguration(fromId);
        clearCache();
    }

    @Override
    public void deleteAll() throws ModuleException {
        Set<String> businessIdToDelete = ruleRepo.findAll()
                                                 .stream()
                                                 .map(Rule::getRulePlugin)
                                                 .map(PluginConfiguration::getBusinessId)
                                                 .collect(Collectors.toSet());
        notifRepo.deleteAll();
        ruleRepo.deleteAll();
        for (String businessId : businessIdToDelete) {
            pluginService.deletePluginConfiguration(businessId);
        }
        clearCache();
    }

    private void clearCache() {
        publisher.publish(new NotifierClearCacheEvent());
    }

    private RuleDTO toRuleDTO(Rule rule) {
        return RuleDTO.build(rule.getRulePlugin(),
                             rule.getRecipients()
                                 .stream()
                                 .map(PluginConfiguration::getBusinessId)
                                 .collect(Collectors.toSet()));
    }
}