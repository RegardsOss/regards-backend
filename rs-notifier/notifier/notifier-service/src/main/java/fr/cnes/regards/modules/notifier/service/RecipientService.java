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

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.notifier.dao.IRecipientErrorRepository;
import fr.cnes.regards.modules.notifier.domain.plugin.IRecipientNotifier;
import fr.cnes.regards.modules.notifier.dto.RecipientDto;
import fr.cnes.regards.modules.notifier.dto.internal.NotifierClearCacheEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
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

    private final IRecipientErrorRepository recipientErrorRepository;

    private final NotificationProcessingService notificationProcessingService;

    private final IPublisher publisher;

    public RecipientService(IPluginService pluginService,
                            IRecipientErrorRepository recipientErrorRepository,
                            NotificationProcessingService notificationProcessingService,
                            IPublisher publisher) {
        this.pluginService = pluginService;
        this.recipientErrorRepository = recipientErrorRepository;
        this.notificationProcessingService = notificationProcessingService;
        this.publisher = publisher;
    }

    @Override
    public Set<PluginConfiguration> getRecipients() {
        return new HashSet<>(pluginService.getPluginConfigurationsByType(IRecipientNotifier.class));
    }

    @Override
    public Set<PluginConfiguration> getRecipients(@Nullable Collection<String> businessIds) {
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

    @Override
    public Set<RecipientDto> findRecipients(@Nullable Boolean directNotificationEnabled) {
        Set<RecipientDto> resultRecipientDtos = new HashSet<>();
        // Get all plugins configuration of IRecipientNotifier type from the database
        for (PluginConfiguration recipientPluginConfiguration : getRecipients()) {
            Optional<IRecipientNotifier> recipientNotifierPlugin = getRecipientNotifierPlugin(
                recipientPluginConfiguration);

            if (recipientNotifierPlugin.isPresent()) {
                if (directNotificationEnabled == null) {
                    // Add all recipients
                    resultRecipientDtos.add(new RecipientDto(recipientPluginConfiguration.getBusinessId(),
                                                             recipientNotifierPlugin.get().getRecipientLabel(),
                                                             recipientNotifierPlugin.get().getDescription()));

                } else {
                    if (directNotificationEnabled.equals(Boolean.TRUE)) {
                        if (recipientNotifierPlugin.get().isDirectNotificationEnabled()) {
                            // Add only recipients which enable the direct notification
                            resultRecipientDtos.add(new RecipientDto(recipientPluginConfiguration.getBusinessId(),
                                                                     recipientNotifierPlugin.get().getRecipientLabel(),
                                                                     recipientNotifierPlugin.get().getDescription()));
                        }
                    } else if (!recipientNotifierPlugin.get().isDirectNotificationEnabled()) {
                        // Add only recipients which do not enable the direct notification
                        resultRecipientDtos.add(new RecipientDto(recipientPluginConfiguration.getBusinessId(),
                                                                 recipientNotifierPlugin.get().getRecipientLabel(),
                                                                 recipientNotifierPlugin.get().getDescription()));
                    }
                }
            }
        }
        return resultRecipientDtos;
    }

    private Optional<IRecipientNotifier> getRecipientNotifierPlugin(PluginConfiguration pluginConfiguration) {
        try {
            return Optional.ofNullable(pluginService.getPlugin(pluginConfiguration));
        } catch (ModuleException | NotAvailablePluginConfigurationException e) {
            LOGGER.error("No plugin of IRecipientNotifier type instantiated for plugin configuration[id:{}]",
                         pluginConfiguration.getPluginId());
        }
        return Optional.empty();
    }

    private Optional<PluginConfiguration> retrievePluginConfiguration(String businessId) {
        // TODO make PluginService return optional
        //  rather than throwing an exception
        try {
            return Optional.of(pluginService.getPluginConfiguration(businessId));
        } catch (EntityNotFoundException e) {
            LOGGER.debug("Configuration plugin[id:{}] does not exist!", businessId, e);
            return Optional.empty();
        }
    }

    @Override
    public PluginConfiguration createOrUpdate(@Valid PluginConfiguration recipient) throws ModuleException {
        return recipient.getId() == null ? create(recipient) : update(recipient);
    }

    private PluginConfiguration create(PluginConfiguration newRecipient) throws ModuleException {
        clearCache();
        return pluginService.savePluginConfiguration(newRecipient);
    }

    private PluginConfiguration update(PluginConfiguration recipient) throws ModuleException {
        clearCache();
        return pluginService.updatePluginConfiguration(recipient);
    }

    @Override
    public void delete(String fromId) throws ModuleException {
        doDelete(fromId);
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
        clearCache();
    }

    private void clearCache() {
        publisher.publish(new NotifierClearCacheEvent());
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
        Set<Long> requestScheduledIds = new HashSet<>();
        for (PluginConfiguration recipient : getRecipients()) {
            requestScheduledIds.addAll(notificationProcessingService.scheduleJobForOneRecipient(recipient));
        }
        return requestScheduledIds.size();
    }
}
