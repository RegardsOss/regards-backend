/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Valid;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of recipient service
 *
 * @author Kevin Marchois
 */
@Service
@MultitenantTransactional
public class RecipientService implements IRecipientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipientService.class);

    private final IPluginService pluginService;

    private final IRuleRepository ruleRepository;

    private final IRecipientErrorRepository recipientErrorRepository;

    private final NotificationProcessingService notificationProcessingService;

    private final RuleCache ruleCache;

    public RecipientService(IPluginService pluginService,
                            IRuleRepository ruleRepository,
                            IRecipientErrorRepository recipientErrorRepository,
                            NotificationProcessingService notificationProcessingService,
                            RuleCache ruleCache) {
        this.pluginService = pluginService;
        this.ruleRepository = ruleRepository;
        this.recipientErrorRepository = recipientErrorRepository;
        this.notificationProcessingService = notificationProcessingService;
        this.ruleCache = ruleCache;
    }

    @Override
    public Set<PluginConfiguration> getRecipients() {
        return new HashSet<>(pluginService.getPluginConfigurationsByType(IRecipientNotifier.class));
    }

    @Override
    public Set<PluginConfiguration> getRecipients(Collection<String> businessIds) {
        if (businessIds == null || businessIds.isEmpty()) {
            return getRecipients();
        }
        // TODO use flatMap(Optional::stream) with upgraded jdk
        return businessIds.stream()
                          .map(this::retrievePluginConfiguration)
                          .filter(Optional::isPresent)
                          .map(Optional::get)
                          .collect(Collectors.toSet());
    }

    private Optional<PluginConfiguration> retrievePluginConfiguration(String id) {
        // TODO make PluginService return optional
        //  rather than throwing an exception
        try {
            return Optional.of(pluginService.getPluginConfiguration(id));
        } catch (EntityNotFoundException e) {
            LOGGER.debug("Configuration does not exist!", e);
            return Optional.empty();
        }
    }

    @Override
    public PluginConfiguration createOrUpdate(@Valid PluginConfiguration newRecipient) throws ModuleException {
        // TODO remove createOrUpdate and make create and update visible
        return newRecipient.getId() == null ? create(newRecipient) : update(newRecipient);
    }

    private PluginConfiguration create(PluginConfiguration newRecipient) throws ModuleException {
        return pluginService.savePluginConfiguration(newRecipient);
    }

    private PluginConfiguration update(PluginConfiguration newRecipient) throws ModuleException {
        ruleCache.clear();
        return pluginService.updatePluginConfiguration(newRecipient);
    }

    @Override
    public void delete(String fromId) throws ModuleException {
        for (Rule rule : ruleRepository.findByRecipientsBusinessId(fromId)) {
            // Remove  recipient to delete
            rule.setRecipients(rule.getRecipients()
                                   .stream()
                                   .filter(c -> !c.getBusinessId().equals(fromId))
                                   .collect(Collectors.toSet()));
        }
        doDelete(fromId);
        ruleCache.clear();
    }

    @Override
    public void deleteAll() throws ModuleException {
        // FIXME We don't update Rule when deleteAll Recipient
        // In consequence, Rules are not consistent anymore
        // Currently, it is not an issue because
        // we delete all rules juste before this deletion
        Set<String> pluginToDelete = pluginService.getPluginConfigurationsByType(IRecipientNotifier.class)
                                                  .stream()
                                                  .map(PluginConfiguration::getBusinessId)
                                                  .collect(Collectors.toSet());
        for (String businessId : pluginToDelete) {
            doDelete(businessId);
        }
        ruleCache.clear();
    }

    private void doDelete(String businessId) throws ModuleException {
        recipientErrorRepository.deleteByRecipientBusinessId(businessId);
        pluginService.deletePluginConfiguration(businessId);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public int scheduleNotificationJobs() {
        // TODO Warning : bad responsibility separation
        // Analyze more precisely to identify the good modification to make

        // Let's schedule a notification job per recipient. This ensures that each recipient will have to process an
        // optimum batch of requests at once. Moreover, it also ensures that there will be no influence between each recipient errors
        Set<PluginConfiguration> recipients = getRecipients();
        Set<Long> requestScheduledIds = new HashSet<>();
        for (PluginConfiguration recipient : recipients) {
            requestScheduledIds.addAll(notificationProcessingService.scheduleJobForOneRecipient(recipient));
        }
        return requestScheduledIds.size();
    }
}
