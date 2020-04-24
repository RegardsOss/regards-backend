/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.notifier.dto;

import java.util.Collection;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.notifier.plugin.IRecipientNotifier;
import fr.cnes.regards.modules.notifier.plugin.IRuleMatcher;

/**
 * DTO to handle creation of {@link IRuleMatcher}/{@link PluginConfiguration} and  association with {@link IRecipientNotifier}/{@link PluginConfiguration}
 * for rest controllers.
 *
 * @author Sébastien Binda
 *
 */
public class RuleDTO {

    /**
     * {@link IRuleMatcher}/{@link PluginConfiguration}
     */
    private PluginConfiguration rulePluginConf;

    /**
     * BusinessIds of associated {@link IRecipientNotifier}/{@link PluginConfiguration}s
     */
    private final Collection<String> recipientsBusinessIds = Sets.newHashSet();

    /**
     * @param businessId
     * @param map
     */
    public static RuleDTO build(PluginConfiguration rulePluginConf, Collection<String> recipientsBusinessIds) {
        RuleDTO dto = new RuleDTO();
        dto.rulePluginConf = rulePluginConf;
        dto.recipientsBusinessIds.addAll(recipientsBusinessIds);
        return dto;
    }

    public PluginConfiguration getRulePluginConfiguration() {
        return rulePluginConf;
    }

    public Collection<String> getRecipientsBusinessIds() {
        return recipientsBusinessIds;
    }

    /**
     * @return  businessId of the {@link IRuleMatcher}/{@link PluginConfiguration}
     */
    public String getId() {
        if (rulePluginConf != null) {
            return rulePluginConf.getBusinessId();
        }
        return null;
    }

}
