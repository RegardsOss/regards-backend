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
package fr.cnes.regards.modules.notifier.service;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.notifier.dao.IRuleRepository;
import fr.cnes.regards.modules.notifier.domain.Rule;
import fr.cnes.regards.modules.notifier.dto.RuleDTO;

/**
 * Implementation for rule service
 * @author Kevin Marchois
 *
 */
@Service
@MultitenantTransactional
public class RuleService implements IRuleService {

    @Autowired
    private IRuleRepository ruleRepo;

    @Autowired
    private IRecipientService recipientService;

    @Autowired
    private IPluginService pluginService;

    @Override
    public Page<RuleDTO> getRules(Pageable page) {
        Page<Rule> rules = ruleRepo.findAll(page);
        return new PageImpl<>(rules.get().map(this::toRuleDTO).collect(Collectors.toList()));
    }

    @Override
    public Optional<RuleDTO> getRule(String businessId) {
        Optional<Rule> rule = ruleRepo.findByRulePluginBusinessId(businessId);
        if (rule.isPresent()) {
            return Optional.of(this.toRuleDTO(rule.get()));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public RuleDTO createOrUpdateRule(@Valid RuleDTO dto) throws ModuleException {
        Optional<Rule> oRule = ruleRepo.findByRulePluginBusinessId(dto.getId());
        Set<PluginConfiguration> recipients = recipientService.getRecipients(dto.getRecipientsBusinessIds());
        Rule toSave;
        if (oRule.isPresent()) {
            toSave = oRule.get();
            toSave.setRulePlugin(pluginService.updatePluginConfiguration(dto.getRulePluginConfiguration()));
            toSave.setRecipients(recipients);
        } else {
            PluginConfiguration pluginConf = pluginService.savePluginConfiguration(dto.getRulePluginConfiguration());
            toSave = Rule.build(pluginConf, recipients);
        }
        return toRuleDTO(ruleRepo.save(toSave));
    }

    @Override
    public void deleteRule(String businessId) throws ModuleException {
        ruleRepo.deleteByRulePluginBusinessId(businessId);
        pluginService.deletePluginConfiguration(businessId);
    }

    private RuleDTO toRuleDTO(Rule rule) {
        return RuleDTO.build(rule.getRulePlugin(), rule.getRecipients().stream().map(PluginConfiguration::getBusinessId)
                .collect(Collectors.toSet()));
    }

}
