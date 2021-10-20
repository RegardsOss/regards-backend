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
package fr.cnes.regards.modules.notifier.service;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.notifier.dao.IRecipientErrorRepository;
import fr.cnes.regards.modules.notifier.dao.IRuleRepository;
import fr.cnes.regards.modules.notifier.domain.Rule;
import fr.cnes.regards.modules.notifier.domain.plugin.IRecipientNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Valid;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of recipient service
 * @author Kevin Marchois
 *
 */
@Service
@MultitenantTransactional
public class RecipientService implements IRecipientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipientService.class);

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IRuleRepository ruleRepository;

    @Autowired
    private IRecipientErrorRepository recipientErrorRepository;

    @Autowired
    private NotificationProcessingService notificationProcessingService;

    @Autowired
    private RuleCache ruleCache;

    @Override
    public Set<PluginConfiguration> getRecipients() {
        return getRecipients(null);
    }

    @Override
    public Set<PluginConfiguration> getRecipients(Collection<String> businessIds) {
        Set<PluginConfiguration> recipients = Sets.newHashSet();
        if ((businessIds == null) || businessIds.isEmpty()) {
            recipients.addAll(pluginService.getPluginConfigurationsByType(IRecipientNotifier.class));
        } else {
            recipients = businessIds.stream().map(id -> {
                try {
                    return pluginService.getPluginConfiguration(id);
                } catch (EntityNotFoundException e) {
                    LOGGER.debug("Configuration does not exist!", e);
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toSet());
        }
        return recipients;
    }

    @Override
    public PluginConfiguration createOrUpdateRecipient(@Valid PluginConfiguration recipientPluginConf)
            throws ModuleException {
        PluginConfiguration result;
        if (recipientPluginConf.getId() == null) {
            result = pluginService.savePluginConfiguration(recipientPluginConf);
        } else {
            result = pluginService.updatePluginConfiguration(recipientPluginConf);
        }
        // Clean cache
        ruleCache.clear();

        return result;

    }

    @Override
    public void deleteRecipient(String id) throws ModuleException {
        // Check  if a rule is associated to the recipient first
        for (Rule rule : ruleRepository.findByRecipientsBusinessId(id)) {
            // Remove  recipient to delete
            rule.setRecipients(rule.getRecipients().stream().filter(c -> !c.getBusinessId().equals(id))
                                       .collect(Collectors.toSet()));
        }
        // Delete associated errors
        recipientErrorRepository.deleteByRecipientBusinessId(id);
        pluginService.deletePluginConfiguration(id);

        // Clean cache
        ruleCache.clear();
    }

    @Override
    public Set<String> deleteAll(Collection<String> deletionErrors) {
        Set<String> pluginToDelete = new HashSet<>();
        for (PluginConfiguration conf : pluginService.getPluginConfigurationsByType(IRecipientNotifier.class)) {
            recipientErrorRepository.deleteByRecipientBusinessId(conf.getBusinessId());
            pluginToDelete.add(conf.getBusinessId());

        }
        // Clean cache
        ruleCache.clear();
        return pluginToDelete;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public int scheduleNotificationJobs() {
        // lets schedule a notification job per recipient. This ensure us that each recipient will have to process an
        // optimum batch of requests at once. Moreover, it also ensures that there will be no influence between each recipient errors
        Set<PluginConfiguration> recipients = getRecipients();
        Set<Long> requestScheduledIds = new HashSet<>();
        for (PluginConfiguration recipient : recipients) {
            requestScheduledIds.addAll(notificationProcessingService.scheduleJobForOneRecipient(recipient));
        }
        return requestScheduledIds.size();
    }
}
