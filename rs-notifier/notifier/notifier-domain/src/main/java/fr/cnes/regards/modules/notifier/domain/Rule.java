/*
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
package fr.cnes.regards.modules.notifier.domain;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedEntityGraphs;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;

/**
 * {@link IRuleMatcher}/{@link PluginConfiguration} and  association with {@link IRecipientNotifier}/{@link PluginConfiguration}
 * @author kevin
 *
 */
@Entity
@Table(name = "t_rule")
@NamedEntityGraphs({ @NamedEntityGraph(name = "Rule.recipients", attributeNodes = @NamedAttributeNode("recipients")),
        @NamedEntityGraph(name = "Rule.rulePlugin", attributeNodes = @NamedAttributeNode("rulePlugin")) })
public class Rule {

    @Id
    @SequenceGenerator(name = "ruleSequence", initialValue = 1, sequenceName = "seq_rule")
    @GeneratedValue(generator = "ruleSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne
    @NotNull(message = "Rule plugin is required")
    @JoinColumn(name = "rule_plugin_id", nullable = false, foreignKey = @ForeignKey(name = "fk_rule_plugin_id"))
    private PluginConfiguration rulePlugin;

    @OneToMany
    @JoinTable(name = "ta_rule_recipients", joinColumns = @JoinColumn(name = "rule_id"),
            inverseJoinColumns = @JoinColumn(name = "recipient_id"))
    private final Set<PluginConfiguration> recipients = new HashSet<PluginConfiguration>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PluginConfiguration getRulePlugin() {
        return rulePlugin;
    }

    public void setRulePlugin(PluginConfiguration rulePlugin) {
        this.rulePlugin = rulePlugin;
    }

    public Set<PluginConfiguration> getRecipients() {
        return recipients;
    }

    public void setRecipients(Collection<PluginConfiguration> recipients) {
        this.recipients.clear();
        this.recipients.addAll(recipients);
    }

    public static Rule build(PluginConfiguration pluginConf, Collection<PluginConfiguration> recipients) {
        Rule rule = new Rule();
        rule.setRulePlugin(pluginConf);
        rule.recipients.addAll(recipients);
        return rule;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Rule rule = (Rule) o;
        return Objects.equals(rulePlugin, rule.rulePlugin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rulePlugin);
    }
}
