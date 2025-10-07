/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.notification.service;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.modules.notification.dao.INotificationRepository;
import fr.cnes.regards.modules.notification.dao.INotificationSettingsRepository;
import fr.cnes.regards.modules.notification.domain.NotificationSettings;
import fr.cnes.regards.modules.notification.domain.dto.NotificationSettingsDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * {@link INotificationSettingsService} implementation.
 *
 * @author Xavier-Alexandre Brochard
 * @author Sébastien Binda
 * @author Christophe Mertz
 */
@Service
@RegardsTransactional
public class NotificationSettingsService implements INotificationSettingsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationSettingsService.class);

    /**
     * Service handling CRUD operations on project users. Autowired by Spring.
     */
    private final IAuthenticationResolver authenticationResolver;

    /**
     * CRUD repository managing notification settings. Autowired by Spring.
     */
    private final INotificationSettingsRepository notificationSettingsRepository;

    /**
     * Creates a {@link NotificationSettingsService} wired to the given {@link INotificationRepository}.
     *
     * @param authenticationResolver          Autowired by Spring. Must not be {@literal null}.
     * @param pNotificationSettingsRepository Autowired by Spring. Must not be {@literal null}.
     */
    public NotificationSettingsService(final IAuthenticationResolver authenticationResolver,
                                       final INotificationSettingsRepository pNotificationSettingsRepository) {
        this.authenticationResolver = authenticationResolver;
        this.notificationSettingsRepository = pNotificationSettingsRepository;
    }

    @Override
    public NotificationSettings retrieveNotificationSettings() {
        return retrieveNotificationSettings(authenticationResolver.getUser());
    }

    @Override
    public NotificationSettings retrieveNotificationSettings(String projectUserEmail) {
        LOGGER.info("Creating notification settings for {} with WEEKLY frequency", projectUserEmail);
        
        notificationSettingsRepository.insertNotificationSettingsIfNotExistsWeekly(projectUserEmail);
        return notificationSettingsRepository.findOneByProjectUserEmail(projectUserEmail);
    }

    @Override
    public NotificationSettings updateNotificationSettings(final NotificationSettingsDTO notificationSettingsDTO) {
        NotificationSettings notificationSettings = retrieveNotificationSettings();

        if (notificationSettingsDTO.getDays() != null) {
            notificationSettings.setDays(notificationSettingsDTO.getDays());
        }
        if (notificationSettingsDTO.getHours() != null) {
            notificationSettings.setHours(notificationSettingsDTO.getHours());
        }
        if (notificationSettingsDTO.getFrequency() != null) {
            notificationSettings.setFrequency(notificationSettingsDTO.getFrequency());
        }

        return notificationSettingsRepository.save(notificationSettings);
    }

}
