/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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


package fr.cnes.regards.modules.ingest.service.notification;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.ingest.dao.IAIPNotificationSettingsRepository;
import fr.cnes.regards.modules.ingest.domain.notification.AIPNotificationSettings;

/**
 * {@link IAIPNotificationSettingsService}
 * @author Iliana Ghazali
 */

@Service
@MultitenantTransactional
public class AIPNotificationSettingsService implements IAIPNotificationSettingsService {

    @Autowired
    private IAIPNotificationSettingsRepository notificationSettingsRepository;

    @Override
    public AIPNotificationSettings retrieve() {
        Optional<AIPNotificationSettings> notificationSettingsOpt = notificationSettingsRepository.findFirstBy();
        AIPNotificationSettings notificationSettings;
        if (!notificationSettingsOpt.isPresent()) {
            // init new settings
            notificationSettings = new AIPNotificationSettings();
            notificationSettings = notificationSettingsRepository.save(notificationSettings);
        } else {
            // get existing settings
            notificationSettings = notificationSettingsOpt.get();
        }
        return notificationSettings;
    }

    @Override
    public AIPNotificationSettings update(AIPNotificationSettings aipNotificationSettings) throws EntityNotFoundException {
        if (!notificationSettingsRepository.existsById(aipNotificationSettings.getId())) {
            throw new EntityNotFoundException(aipNotificationSettings.getId().toString(), AIPNotificationSettings.class);
        } return notificationSettingsRepository.save(aipNotificationSettings);
    }

    @Override
    public AIPNotificationSettings getCurrentNotificationSettings() {
        return notificationSettingsRepository.findFirstBy().orElse(null);
    }

}
