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
package fr.cnes.regards.modules.notifier.service;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.notifier.dao.INotificationRequestRepository;
import fr.cnes.regards.modules.notifier.dao.IRuleRepository;
import fr.cnes.regards.modules.notifier.domain.plugin.RecipientSender3;
import fr.cnes.regards.modules.notifier.dto.RuleDTO;
import fr.cnes.regards.modules.notifier.mock.InMemoryPluginService;
import fr.cnes.regards.modules.notifier.mock.InMemoryRuleRepoBuilder;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.*;
import java.util.stream.Collectors;

import static fr.cnes.regards.modules.notifier.service.PluginConfigurationTestBuilder.aPlugin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class RuleServiceTest {

    private static final String RECIPIENT_1 = "recipient1";

    private static final String RECIPIENT_2 = "recipient2";

    private static final String RULE_1 = "rule1";

    private static final String RULE_2 = "rule2";

    private final IRuleService ruleService;

    private final IRuleRepository ruleRepo;

    private final IRecipientService recipientService;

    private final IPluginService pluginService;

    private final RuleCache ruleCache;

    private final INotificationRequestRepository notifRepo;

    public RuleServiceTest() {
        ruleRepo = new InMemoryRuleRepoBuilder().get();
        recipientService = mockRecipientService();
        pluginService = new InMemoryPluginService();
        ruleCache = mock(RuleCache.class);
        notifRepo = mock(INotificationRequestRepository.class);

        ruleService = new RuleService(notifRepo, ruleRepo, recipientService, pluginService, ruleCache);
    }

    private IRecipientService mockRecipientService() {
        IRecipientService recipientService = mock(IRecipientService.class);
        List<PluginConfiguration> recipients = Arrays.asList(aRecipient(RECIPIENT_1), aRecipient(RECIPIENT_2));
        ArgumentCaptor<Collection> recipientIds = ArgumentCaptor.forClass(Collection.class);
        Mockito.when(recipientService.getRecipients(recipientIds.capture()))
               .thenAnswer(i -> recipients.stream()
                                          .filter(r -> recipientIds.getValue().contains(r.getBusinessId()))
                                          .collect(Collectors.toSet()));
        return recipientService;
    }

    private PluginConfiguration aRecipient(String withName) {
        return aPlugin().identified(withName).named(withName).withPluginId(RecipientSender3.PLUGIN_ID).build();
    }

    @Test
    public void create_rule_saves_rule_in_repository() throws Exception {
        PluginConfiguration firstRule = aRule(RULE_1);
        Set<String> recipients = Collections.singleton(RECIPIENT_1);

        ruleService.createOrUpdate(RuleDTO.build(firstRule, recipients));

        Optional<RuleDTO> actualRule = ruleService.getRule(RULE_1);
        assertThat(actualRule).isPresent();
        assertThat(actualRule.get().getRulePluginConfiguration()).isEqualTo(firstRule);
        assertThat(actualRule.get().getRecipientsBusinessIds()).containsExactly(RECIPIENT_1);
    }

    @Test
    public void create_rule_saves_its_plugin() throws Exception {
        PluginConfiguration firstRule = aRule(RULE_1);
        Set<String> recipients = Collections.singleton(RECIPIENT_1);

        RuleDTO ruleToCreate = RuleDTO.build(firstRule, recipients);
        ruleService.createOrUpdate(ruleToCreate);

        PluginConfiguration actualRulePlugin = pluginService.getPluginConfiguration(RULE_1);
        assertThat(actualRulePlugin).isEqualTo(firstRule);
    }

    @Test
    public void update_rule_update_the_rule_in_repository() throws Exception {
        PluginConfiguration firstRule = aRule(RULE_1);
        Set<String> recipients = Collections.singleton(RECIPIENT_1);
        ruleService.createOrUpdate(RuleDTO.build(firstRule, recipients));

        PluginConfiguration updatedRule = aRule(RULE_1);
        List<String> updatedRecipients = Arrays.asList(RECIPIENT_1, RECIPIENT_2);
        ruleService.createOrUpdate(RuleDTO.build(updatedRule, updatedRecipients));

        Optional<RuleDTO> actualRule = ruleService.getRule(RULE_1);
        assertThat(actualRule).isPresent();
        assertThat(actualRule.get().getRecipientsBusinessIds())//
                                                               .hasSize(2) //
                                                               .contains(RECIPIENT_1, RECIPIENT_2);
    }

    @Test
    public void update_rule_update_its_plugin() throws Exception {
        PluginConfiguration firstRule = aRule(RULE_1);
        Set<String> recipients = Collections.singleton(RECIPIENT_1);
        ruleService.createOrUpdate(RuleDTO.build(firstRule, recipients));

        PluginConfiguration updatedRule = aRule(RULE_1);
        updatedRule.setVersion("2");
        ruleService.createOrUpdate(RuleDTO.build(updatedRule, recipients));

        PluginConfiguration actualRulePlugin = pluginService.getPluginConfiguration(RULE_1);
        assertThat(actualRulePlugin.getVersion()).isEqualTo("2");
    }

    @Test
    public void update_rule_clear_rule_cache() throws Exception {
        PluginConfiguration firstRule = aRule(RULE_1);
        Set<String> recipients = Collections.singleton(RECIPIENT_1);
        ruleService.createOrUpdate(RuleDTO.build(firstRule, recipients));

        PluginConfiguration updatedRule = aRule(RULE_1);
        updatedRule.setVersion("2");
        ruleService.createOrUpdate(RuleDTO.build(updatedRule, recipients));

        verify(ruleCache).clear();
    }

    @Test
    public void getRules_returns_all_saved_rules() throws Exception {
        PluginConfiguration firstRule = aRule(RULE_1);
        PluginConfiguration secondRule = aRule(RULE_2);
        Set<String> recipients = Collections.singleton(RECIPIENT_1);
        ruleService.createOrUpdate(RuleDTO.build(firstRule, recipients));
        ruleService.createOrUpdate(RuleDTO.build(secondRule, recipients));

        Page<RuleDTO> rules = ruleService.getRules(PageRequest.of(0, 100_000));

        assertThat(rules).hasSize(2).map(RuleDTO::getRulePluginConfiguration).contains(firstRule, secondRule);
    }

    @Test
    public void returns_empty_if_rule_is_not_found_in_repository() throws Exception {
        Optional<RuleDTO> rule = ruleService.getRule(RULE_1);
        assertThat(rule).isEmpty();
    }

    @Test
    public void deleteRule_remove_rule_from_repository() throws Exception {
        PluginConfiguration firstRule = aRule(RULE_1);
        PluginConfiguration secondRule = aRule(RULE_2);
        Set<String> recipients = Collections.singleton(RECIPIENT_1);
        ruleService.createOrUpdate(RuleDTO.build(firstRule, recipients));
        ruleService.createOrUpdate(RuleDTO.build(secondRule, recipients));

        ruleService.delete(RULE_1);

        assertThat(ruleService.getRule(RULE_1)).isEmpty();
    }

    @Test
    public void deleteRule_remove_its_plugin() throws Exception {
        PluginConfiguration firstRule = aRule(RULE_1);
        PluginConfiguration secondRule = aRule(RULE_2);
        Set<String> recipients = Collections.singleton(RECIPIENT_1);
        ruleService.createOrUpdate(RuleDTO.build(firstRule, recipients));
        ruleService.createOrUpdate(RuleDTO.build(secondRule, recipients));

        ruleService.delete(RULE_1);

        Assertions.assertThatExceptionOfType(EntityNotFoundException.class)
                  .isThrownBy(() -> pluginService.getPluginConfiguration(RULE_1));
    }

    @Test
    public void deleteRule_clear_cache() throws Exception {
        PluginConfiguration firstRule = aRule(RULE_1);
        Set<String> recipients = Collections.singleton(RECIPIENT_1);
        ruleService.createOrUpdate(RuleDTO.build(firstRule, recipients));

        ruleService.delete(RULE_1);

        verify(ruleCache).clear();
    }

    @Test
    public void deleteAll_clear_repository() throws Exception {
        PluginConfiguration firstRule = aRule(RULE_1);
        PluginConfiguration secondRule = aRule(RULE_2);
        Set<String> recipients = Collections.singleton(RECIPIENT_1);
        ruleService.createOrUpdate(RuleDTO.build(firstRule, recipients));
        ruleService.createOrUpdate(RuleDTO.build(secondRule, recipients));

        ruleService.deleteAll();

        Page<RuleDTO> rules = ruleService.getRules(PageRequest.of(0, 100_000));
        assertThat(rules.getTotalElements()).isZero();
    }

    @Test
    public void deleteAll_delete_associated_plugins() throws Exception {
        PluginConfiguration firstRule = aRule(RULE_1);
        PluginConfiguration secondRule = aRule(RULE_2);
        Set<String> recipients = Collections.singleton(RECIPIENT_1);
        ruleService.createOrUpdate(RuleDTO.build(firstRule, recipients));
        ruleService.createOrUpdate(RuleDTO.build(secondRule, recipients));

        ruleService.deleteAll();

        Assertions.assertThatExceptionOfType(EntityNotFoundException.class)
                  .isThrownBy(() -> pluginService.getPluginConfiguration(RULE_1));
        Assertions.assertThatExceptionOfType(EntityNotFoundException.class)
                  .isThrownBy(() -> pluginService.getPluginConfiguration(RULE_2));
    }

    @Test
    public void deleteAll_remove_all_notifications() throws Exception {
        PluginConfiguration firstRule = aRule(RULE_1);
        PluginConfiguration secondRule = aRule(RULE_2);
        Set<String> recipients = Collections.singleton(RECIPIENT_1);
        ruleService.createOrUpdate(RuleDTO.build(firstRule, recipients));
        ruleService.createOrUpdate(RuleDTO.build(secondRule, recipients));

        ruleService.deleteAll();

        verify(notifRepo).deleteAll();
    }

    @Test
    public void deleteAll_clear_cache() throws Exception {
        PluginConfiguration firstRule = aRule(RULE_1);
        PluginConfiguration secondRule = aRule(RULE_2);
        Set<String> recipients = Collections.singleton(RECIPIENT_1);
        ruleService.createOrUpdate(RuleDTO.build(firstRule, recipients));
        ruleService.createOrUpdate(RuleDTO.build(secondRule, recipients));

        ruleService.deleteAll();

        verify(ruleCache).clear();
    }

    @Test
    public void recipientDeleted_remove_recipient_from_rules() throws Exception {
        PluginConfiguration firstRule = aRule(RULE_1);
        List<String> firstRecipients = Arrays.asList(RECIPIENT_1, RECIPIENT_2);
        ruleService.createOrUpdate(RuleDTO.build(firstRule, firstRecipients));
        PluginConfiguration secondRule = aRule(RULE_2);
        List<String> secondRecipients = Collections.singletonList(RECIPIENT_1);
        ruleService.createOrUpdate(RuleDTO.build(secondRule, secondRecipients));

        ruleService.recipientDeleted(RECIPIENT_1);

        Optional<RuleDTO> actualRule1 = ruleService.getRule(RULE_1);
        assertThat(actualRule1).isPresent();
        assertThat(actualRule1.get().getRecipientsBusinessIds()).hasSize(1).contains(RECIPIENT_2);
        Optional<RuleDTO> actualRule2 = ruleService.getRule(RULE_2);
        assertThat(actualRule2).isPresent();
        assertThat(actualRule2.get().getRecipientsBusinessIds()).isEmpty();
    }

    @Test
    public void recipientDeleted_clear_rule_cache() throws Exception {
        PluginConfiguration firstRule = aRule(RULE_1);
        List<String> firstRecipients = Arrays.asList(RECIPIENT_1, RECIPIENT_2);
        ruleService.createOrUpdate(RuleDTO.build(firstRule, firstRecipients));

        ruleService.recipientDeleted(RECIPIENT_1);

        verify(ruleCache).clear();
    }

    @Test
    public void recipientUpdated_clear_cache() throws Exception {
        PluginConfiguration firstRule = aRule(RULE_1);
        List<String> firstRecipients = Arrays.asList(RECIPIENT_1, RECIPIENT_2);
        ruleService.createOrUpdate(RuleDTO.build(firstRule, firstRecipients));

        ruleService.recipientDeleted(RECIPIENT_1);

        verify(ruleCache).clear();
    }

    private PluginConfiguration aRule(String withName) {
        return aPlugin().identified(withName)
                        .named(withName)
                        .withPluginId("DefaultRuleMatcher")
                        .parameterized_by(IPluginParam.build("attributeToSeek", "nature"))
                        .parameterized_by(IPluginParam.build("attributeValueToSeek", "TM"))
                        .build();
    }
}