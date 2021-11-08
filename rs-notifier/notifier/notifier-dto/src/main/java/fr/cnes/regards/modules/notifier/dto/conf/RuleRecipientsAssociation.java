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
package fr.cnes.regards.modules.notifier.dto.conf;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;

import java.util.Collection;
import java.util.Set;

/**
 * Associates a {@link IRuleMatcher}/{@link PluginConfiguration} to a list of {@link IRecipientNotifier}/{@link PluginConfiguration}
 * This DTO is used by import/export module configuration manager
 *
 * @author Sébastien Binda
 */
public class RuleRecipientsAssociation {

    /**
     * Business Id of the {@link IRuleMatcher}/{@link PluginConfiguration}
     */
    private String ruleId;

    /**
     * Recipient labels of the {@link IRecipientNotifier}/{@link PluginConfiguration}s
     */
    private Set<String> recipientLabels;

    public static RuleRecipientsAssociation build(String ruleId, Collection<String> recipientLabels) {
        RuleRecipientsAssociation assoc = new RuleRecipientsAssociation();
        assoc.recipientLabels = Sets.newHashSet(recipientLabels);
        assoc.ruleId = ruleId;
        return assoc;
    }

    public String getRuleId() {
        return ruleId;
    }

    public Set<String> getRecipientLabels() {
        return recipientLabels;
    }

}
