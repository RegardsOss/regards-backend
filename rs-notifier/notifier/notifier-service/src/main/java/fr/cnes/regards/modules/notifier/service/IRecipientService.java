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

import javax.validation.Valid;
import java.util.Collection;
import java.util.Set;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.notifier.domain.NotificationRequest;
import fr.cnes.regards.modules.notifier.domain.plugin.IRecipientNotifier;

/**
 * Service for recipient({@link PluginConfiguration}) manipulation
 * @author Kevin Marchois
 *
 */
public interface IRecipientService {

    Set<PluginConfiguration> getRecipients(Collection<String> businessId);

    Set<PluginConfiguration> getRecipients();

    /**
     * Create or update a recipient({@link PluginConfiguration}) from a recipient({@link PluginConfiguration})
     * @return recipient({@link PluginConfiguration}) from the created recipient({@link PluginConfiguration})
     * @throws ModuleException if during an update id is unknow
     */
    PluginConfiguration createOrUpdateRecipient(@Valid PluginConfiguration toCreate) throws ModuleException;

    /**
     * Delete a recipient({@link PluginConfiguration}) by its id
     */
    void deleteRecipient(String id) throws ModuleException;

    /**
     * Delete all plugin configurations for {@link IRecipientNotifier} plugin type
     * @return plugin businessIds to delete
     */
    Set<String> deleteAll(Collection<String> deletionErrors);

    /**
     * schedule {@link fr.cnes.regards.modules.notifier.service.job.NotificationJob} for each recipient
     * @return number of {@link fr.cnes.regards.modules.notifier.service.job.NotificationJob} scheduled
     */
    int scheduleNotificationJobs();
}
