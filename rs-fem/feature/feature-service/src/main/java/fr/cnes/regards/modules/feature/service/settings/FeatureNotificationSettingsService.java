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


package fr.cnes.regards.modules.feature.service.settings;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.feature.dao.IFeatureNotificationSettingsRepository;
import fr.cnes.regards.modules.feature.domain.settings.FeatureNotificationSettings;

/**
 * see {@link IFeatureNotificationSettingsService}
 * @author Iliana Ghazali
 */

@Service
@MultitenantTransactional
public class FeatureNotificationSettingsService implements IFeatureNotificationSettingsService {

    @Autowired
    private IFeatureNotificationSettingsRepository notificationSettingsRepository;

    @Override
    public FeatureNotificationSettings retrieve() {
        Optional<FeatureNotificationSettings> notificationSettingsOpt = notificationSettingsRepository.findFirstBy();
        FeatureNotificationSettings notificationSettings;
        if (!notificationSettingsOpt.isPresent()) {
            // init new settings
            notificationSettings = new FeatureNotificationSettings();
            notificationSettings = notificationSettingsRepository.save(notificationSettings);
        } else {
            // get existing settings
            notificationSettings = notificationSettingsOpt.get();
        }
        return notificationSettings;
    }

    @Override
    public FeatureNotificationSettings update(FeatureNotificationSettings featureNotificationSettings) throws
            EntityNotFoundException {
        if (!notificationSettingsRepository.existsById(featureNotificationSettings.getId())) {
            throw new EntityNotFoundException(featureNotificationSettings.getId().toString(), FeatureNotificationSettings.class);
        } return notificationSettingsRepository.save(featureNotificationSettings);
    }

    @Override
    public FeatureNotificationSettings getCurrentNotificationSettings() {
        return notificationSettingsRepository.findFirstBy().orElse(null);
    }
}
