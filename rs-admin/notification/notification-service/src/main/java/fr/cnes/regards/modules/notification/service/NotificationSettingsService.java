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
package fr.cnes.regards.modules.notification.service;

import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.notification.dao.INotificationRepository;
import fr.cnes.regards.modules.notification.dao.INotificationSettingsRepository;
import fr.cnes.regards.modules.notification.domain.NotificationFrequency;
import fr.cnes.regards.modules.notification.domain.NotificationSettings;
import fr.cnes.regards.modules.notification.domain.dto.NotificationSettingsDTO;

/**
 * {@link INotificationSettingsService} implementation.
 *
 * @author Xavier-Alexandre Brochard
 * @author Sébastien Binda
 * @author Christophe Mertz
 *
 */
@Service
@RegardsTransactional
public class NotificationSettingsService implements INotificationSettingsService {

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
     * @param authenticationResolver
     *            Autowired by Spring. Must not be {@literal null}.
     * @param pNotificationSettingsRepository
     *            Autowired by Spring. Must not be {@literal null}.
     */
    public NotificationSettingsService(final IAuthenticationResolver authenticationResolver,
            final INotificationSettingsRepository pNotificationSettingsRepository) {
        super();
        this.authenticationResolver = authenticationResolver;
        notificationSettingsRepository = pNotificationSettingsRepository;
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.notification.service.INotificationService#retrieveNotificationSettings()
     */
    @Override
    public NotificationSettings retrieveNotificationSettings() {
        return retrieveNotificationSettings(authenticationResolver.getUser());
    }

    @Override
    public NotificationSettings retrieveNotificationSettings(String userEmail) {
        NotificationSettings result = notificationSettingsRepository.findOneByProjectUserEmail(userEmail);
        if (result == null) {
            result = createNotificationSettings(userEmail);
        }
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.notification.service.INotificationService#updateNotificationSettings(fr.cnes.regards.
     * modules.notification.domain.NotificationSettings)
     */
    @Override
    public NotificationSettings updateNotificationSettings(final NotificationSettingsDTO pDto)
            throws EntityNotFoundException {
        final NotificationSettings notificationSettings = retrieveNotificationSettings();

        if (pDto.getDays() != null) {
            notificationSettings.setDays(pDto.getDays());
        }
        if (pDto.getHours() != null) {
            notificationSettings.setHours(pDto.getHours());
        }
        if (pDto.getFrequency() != null) {
            notificationSettings.setFrequency(pDto.getFrequency());
        }

        return notificationSettingsRepository.save(notificationSettings);
    }

    /**
     * Create notification settings for project user
     *
     * @param pProjectUser
     *            The target project user
     * @return The created notification settings
     */
    private NotificationSettings createNotificationSettings(final String pProjectUser) {
        final NotificationSettings settings = new NotificationSettings();
        settings.setProjectUserEmail(pProjectUser);
        settings.setFrequency(NotificationFrequency.WEEKLY);
        return notificationSettingsRepository.save(settings);
    }

}
