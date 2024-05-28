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

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.notifier.domain.plugin.IRecipientNotifier;
import fr.cnes.regards.modules.notifier.dto.RecipientDto;
import jakarta.validation.Valid;

import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.Set;

/**
 * Service for recipient({@link PluginConfiguration}) manipulation
 *
 * @author Kevin Marchois
 */
public interface IRecipientService {

    Set<PluginConfiguration> getRecipients(Collection<String> businessId);

    /**
     * Get all recipients({@link PluginConfiguration}) with IRecipientNotifier({@link IRecipientNotifier}) type
     */
    Set<PluginConfiguration> getRecipients();

    /**
     * Find all recipients (from {@link PluginConfiguration}), plugins of IRecipientNotifier type
     * <ul>
     *   <li>directNotificationEnabled is null : set of all recipients</li>
     *   <li>directNotificationEnabled is true : set of recipients which enables the direct notification</li>
     *   <li>directNotificationEnabled is false : set of recipients which does not enable the direct notification</li>
     * </ul>
     *
     * @param directNotificationEnabled if the plugin enable the direct notification
     * @return a set of recipients ({@link RecipientDto})
     */
    Set<RecipientDto> findRecipients(@Nullable Boolean directNotificationEnabled);

    /**
     * Create or update a recipient({@link PluginConfiguration}) from a recipient({@link PluginConfiguration})
     *
     * @return recipient({ @ link PluginConfiguration }) from the created recipient({@link PluginConfiguration})
     * @throws ModuleException if during an update id is unknow
     */
    PluginConfiguration createOrUpdate(@Valid PluginConfiguration toCreate) throws ModuleException;

    /**
     * Delete a recipient({@link PluginConfiguration}) by its id
     */
    void delete(String id) throws ModuleException;

    /**
     * Delete all plugin configurations for {@link IRecipientNotifier} plugin type
     *
     * @return plugin businessIds to delete
     */
    void deleteAll() throws ModuleException;

    /**
     * schedule {@link fr.cnes.regards.modules.notifier.service.job.NotificationJob} for each recipient
     *
     * @return number of {@link fr.cnes.regards.modules.notifier.service.job.NotificationJob} scheduled
     */
    int scheduleNotificationJobs();
}
