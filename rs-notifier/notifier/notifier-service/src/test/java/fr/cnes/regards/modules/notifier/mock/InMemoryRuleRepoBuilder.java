/*
 * Copyright 2017-20XX CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.notifier.mock;

import fr.cnes.regards.modules.notifier.dao.IRuleRepository;
import fr.cnes.regards.modules.notifier.domain.Rule;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class InMemoryRuleRepoBuilder {

    private final List<Rule> savedRules;

    private final IRuleRepository ruleRepo;

    public InMemoryRuleRepoBuilder() {
        savedRules = new ArrayList<>();
        ruleRepo = mock(IRuleRepository.class);
        setupMock();
    }

    public IRuleRepository get() {
        return ruleRepo;
    }

    private void setupMock() {
        mockFindAll();
        mockFindAllWithPageable();
        mockFindByRulePluginBusinessId();
        mockFindByRecipientsBusinessId();
        mockFindByRulePluginActiveTrue();

        mockSave();

        mockDelete();
        mockDeleteByRulePluginBusinessId();
        mockDeleteAll();
    }

    private void mockFindByRecipientsBusinessId() {
        // No need to filter returned rules for mock.
        when(ruleRepo.findByRecipientsBusinessId(anyString())).thenAnswer(i -> new HashSet<>(savedRules));
    }

    private void mockFindByRulePluginBusinessId() {
        ArgumentCaptor<String> ruleId = ArgumentCaptor.forClass(String.class);
        Mockito.when(ruleRepo.findByRulePluginBusinessId(ruleId.capture()))
               .thenAnswer(i -> savedRules.stream()
                                          .filter(rule -> rule.getRulePlugin()
                                                              .getBusinessId()
                                                              .equals(ruleId.getValue()))
                                          .findFirst());
    }

    private void mockFindByRulePluginActiveTrue() {
        when(ruleRepo.findByRulePluginActiveTrue()).thenAnswer(i -> new HashSet<>(savedRules));
    }

    private void mockFindAllWithPageable() {
        // No need to handle Page precisely
        when(ruleRepo.findAll((Pageable) any())).thenAnswer(i -> new PageImpl(savedRules));
    }

    private void mockFindAll() {
        when(ruleRepo.findAll()).thenReturn(savedRules);
    }

    private void mockSave() {
        ArgumentCaptor<Rule> addedRule = ArgumentCaptor.forClass(Rule.class);
        when(ruleRepo.save(addedRule.capture())).thenAnswer(i -> {
            Rule newRule = addedRule.getValue();
            savedRules.removeIf(rule -> rule.getRulePlugin()
                                            .getBusinessId()
                                            .equals(newRule.getRulePlugin().getBusinessId()));
            savedRules.add(newRule);
            return newRule;
        });
    }

    private void mockDelete() {
        ArgumentCaptor<Rule> deletedRule = ArgumentCaptor.forClass(Rule.class);
        doAnswer(i -> {
            savedRules.removeIf(rule -> rule.getRulePlugin()
                                            .getBusinessId()
                                            .equals(deletedRule.getValue().getRulePlugin().getBusinessId()));
            return null;
        }).when(ruleRepo).delete(deletedRule.capture());
    }

    private void mockDeleteAll() {
        doAnswer(i -> {
            savedRules.clear();
            return null;
        }).when(ruleRepo).deleteAll();
    }

    private void mockDeleteByRulePluginBusinessId() {
        ArgumentCaptor<String> idToDelete = ArgumentCaptor.forClass(String.class);
        doAnswer(i -> {
            savedRules.removeIf(rule -> rule.getRulePlugin().getBusinessId().equals(idToDelete.getValue()));
            return null;
        }).when(ruleRepo).deleteByRulePluginBusinessId(idToDelete.capture());
    }
}
