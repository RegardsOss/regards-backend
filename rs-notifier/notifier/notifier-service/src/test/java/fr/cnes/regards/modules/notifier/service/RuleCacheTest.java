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

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.notifier.dao.IRuleRepository;
import fr.cnes.regards.modules.notifier.domain.Rule;
import fr.cnes.regards.modules.notifier.mock.InMemoryRuleRepoBuilder;
import org.junit.Test;

import java.util.Collections;
import java.util.Set;

import static fr.cnes.regards.modules.notifier.service.PluginConfigurationTestBuilder.aPlugin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RuleCacheTest {

    private static final String RULE_1 = "rule1";

    private static final String RULE_2 = "rule2";

    private final IRuleRepository ruleRepository;

    private final RuleCache ruleCache;

    public RuleCacheTest() {
        ruleRepository = new InMemoryRuleRepoBuilder().get();
        ruleCache = new RuleCache(aNominalTenant(), ruleRepository);
    }

    private IRuntimeTenantResolver aNominalTenant() {
        IRuntimeTenantResolver tenantResolver = mock(IRuntimeTenantResolver.class);
        when(tenantResolver.getTenant()).thenReturn("TENANT");
        return tenantResolver;
    }

    @Test
    public void rule_access_read_repo_first_time() throws Exception {
        Rule rule1 = aRule(RULE_1);
        Rule rule2 = aRule(RULE_2);
        ruleRepository.save(rule1);
        ruleRepository.save(rule2);

        Set<Rule> rules = ruleCache.getRules();

        // Data are read in repository
        // Cache is filled with read data
        assertThat(rules).contains(rule1, rule2);
    }

    @Test
    public void rule_access_read_in_cache_second_time() throws Exception {
        Rule rule1 = aRule(RULE_1);
        Rule rule2 = aRule(RULE_2);
        ruleRepository.save(rule1);
        ruleRepository.save(rule2);
        ruleCache.getRules();

        ruleRepository.delete(rule1);

        // Repository is not read but in cache
        Set<Rule> rules = ruleCache.getRules();
        assertThat(rules).contains(rule1, rule2);
    }

    @Test
    public void clear_invalidate_cache() throws Exception {
        Rule rule1 = aRule(RULE_1);
        Rule rule2 = aRule(RULE_2);
        ruleRepository.save(rule1);
        ruleRepository.save(rule2);
        ruleCache.getRules();

        ruleRepository.delete(rule1);
        ruleCache.clear();

        // Repository is read
        // because cache has just been cleared
        Set<Rule> rules = ruleCache.getRules();
        assertThat(rules).containsExactly(rule2);
    }

    private Rule aRule(String withName) {
        PluginConfiguration plugin = aPlugin().identified(withName)
                                              .named(withName)
                                              .withPluginId("DefaultRuleMatcher")
                                              .build();
        return Rule.build(plugin, Collections.emptyList());
    }

}


