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
package fr.cnes.regards.modules.notification.service;

import java.util.Optional;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.notification.dao.INotificationSettingsRepository;
import fr.cnes.regards.modules.notification.domain.NotificationFrequency;
import fr.cnes.regards.modules.notification.domain.NotificationSettings;
import fr.cnes.regards.modules.notification.domain.dto.NotificationSettingsDTO;

/**
 * Test class for {@link NotificationSettingsService}.
 *
 * @author Xavier-Alexandre Brochard
 */
public class NotificationSettingsServiceTest {

    /**
     * Tested service
     */
    private NotificationSettingsService notificationSettingsService;

    /**
     * Service handling CRUD operations on project users. Autowired by Spring.
     */
    private IAuthenticationResolver authenticationResolver;

    /**
     * CRUD repository managing notification settings. Autowired by Spring.
     */
    private INotificationSettingsRepository notificationSettingsRepository;

    /**
     * Do some setup before each test
     */
    @Before
    public void setUp() {
        // Mock services
        authenticationResolver = Mockito.mock(IAuthenticationResolver.class);
        notificationSettingsRepository = Mockito.mock(INotificationSettingsRepository.class);

        // Instanciate the tested service
        notificationSettingsService = new NotificationSettingsService(authenticationResolver,
                                                                      notificationSettingsRepository);
    }

    /**
     * Check that the system allows to retrieve the notification settings of currently logged user.
     *
     * @throws EntityNotFoundException
     *             thrown when no current user could be found
     */
    @Test
    @Requirement("REGARDS_DSL_DAM_CQA_040")
    @Purpose("Check that the system allows to retrieve the notification settings of currently logged user.")
    public void retrieveNotificationsAlreadyExists() throws EntityNotFoundException {
        // Define expected
        final NotificationSettings expected = new NotificationSettings();

        // Mock methods
        Mockito.when(authenticationResolver.getUser()).thenReturn("");
        Mockito.when(notificationSettingsRepository.findOneByProjectUserEmail("")).thenReturn(expected);

        // Call tested method
        final NotificationSettings actual = notificationSettingsService.retrieveNotificationSettings();

        // Check that expected is equel to acutal
        Assert.assertThat(actual, CoreMatchers.is(CoreMatchers.equalTo(expected)));

        // Check that the repository's method was called with right arguments
        Mockito.verify(notificationSettingsRepository).findOneByProjectUserEmail("");
    }

    /**
     * Check that the system creates empty settings when trying to retrieve not existing settings.
     *
     * @throws EntityNotFoundException
     *             thrown when no current user could be found
     */
    @Test
    @Requirement("REGARDS_DSL_DAM_CQA_040")
    @Purpose("Check that the system creates empty settings when trying to retrieve not existing settings.")
    public void retrieveNotificationsNotExists() throws EntityNotFoundException {
        // Defined expected
        final NotificationSettings expected = new NotificationSettings();

        // Mock methods
        Mockito.when(authenticationResolver.getUser()).thenReturn("");
        Mockito.when(notificationSettingsRepository.findOneByProjectUserEmail("")).thenReturn(null);
        Mockito.when(notificationSettingsRepository.save(Mockito.any(NotificationSettings.class))).thenReturn(expected);

        // Call tested method
        final NotificationSettings actual = notificationSettingsService.retrieveNotificationSettings();

        // Check that expected is equel to acutal
        Assert.assertThat(actual, CoreMatchers.is(CoreMatchers.equalTo(expected)));

        // Check that the repository's method was called with right arguments
        Mockito.verify(notificationSettingsRepository).findOneByProjectUserEmail("");
    }

    /**
     * Check that the system allows to update notification settings.
     *
     * @throws EntityNotFoundException
     *             thrown when no current user could be found
     */
    @Test
    @Requirement("REGARDS_DSL_DAM_CQA_040")
    @Purpose("Check that the system allows to update notification settings.")
    public void updateNotificationSettings() throws EntityNotFoundException {

        // Define initial
        final Long id = 0L;
        final NotificationSettings initial = new NotificationSettings();
        initial.setId(id);
        initial.setDays(2);
        initial.setHours(1);
        initial.setFrequency(NotificationFrequency.DAILY);

        // Define passed DTO and corresponding expected pojo
        final NotificationSettingsDTO dto = new NotificationSettingsDTO();
        dto.setDays(0);
        dto.setHours(2);
        dto.setFrequency(NotificationFrequency.MONTHLY);
        final NotificationSettings expected = new NotificationSettings();
        initial.setId(id);
        initial.setDays(0);
        initial.setHours(2);
        initial.setFrequency(NotificationFrequency.MONTHLY);

        // Mock the repository returned value
        Mockito.when(notificationSettingsRepository.existsById(id)).thenReturn(true);
        Mockito.when(notificationSettingsRepository.findById(id)).thenReturn(Optional.of(initial));
        Mockito.when(notificationSettingsRepository.findOneByProjectUserEmail("")).thenReturn(expected);
        Mockito.when(authenticationResolver.getUser()).thenReturn("");

        // Perform the update
        notificationSettingsService.updateNotificationSettings(dto);

        // Check that the repository's method was called with right arguments
        Mockito.verify(notificationSettingsRepository).save(Mockito.refEq(expected));
    }

    /**
     * Check that the system allows to update notification settings.
     *
     * @throws EntityNotFoundException
     *             thrown when no current user could be found
     */
    @Test
    @Requirement("REGARDS_DSL_DAM_CQA_040")
    @Purpose("Check that the system allows to update notification settings.")
    public void updateNotificationSettingsNullDays() throws EntityNotFoundException {

        // Define initial
        final Long id = 0L;
        final NotificationSettings initial = new NotificationSettings();
        initial.setId(id);
        initial.setDays(2);
        initial.setHours(1);
        initial.setFrequency(NotificationFrequency.DAILY);

        // Define passed DTO and corresponding expected pojo
        final NotificationSettingsDTO dto = new NotificationSettingsDTO();
        dto.setDays(null);
        dto.setHours(2);
        dto.setFrequency(NotificationFrequency.MONTHLY);
        final NotificationSettings expected = new NotificationSettings();
        initial.setId(id);
        initial.setDays(null);
        initial.setHours(2);
        initial.setFrequency(NotificationFrequency.MONTHLY);

        // Mock the repository returned value
        Mockito.when(notificationSettingsRepository.existsById(id)).thenReturn(true);
        Mockito.when(notificationSettingsRepository.findById(id)).thenReturn(Optional.of(initial));
        Mockito.when(notificationSettingsRepository.findOneByProjectUserEmail("")).thenReturn(expected);
        Mockito.when(authenticationResolver.getUser()).thenReturn("");

        // Perform the update
        notificationSettingsService.updateNotificationSettings(dto);

        // Check that the repository's method was called with right arguments
        Mockito.verify(notificationSettingsRepository).save(Mockito.refEq(expected));
    }

    /**
     * Check that the system allows to update notification settings.
     *
     * @throws EntityNotFoundException
     *             thrown when no current user could be found
     */
    @Test
    @Requirement("REGARDS_DSL_DAM_CQA_040")
    @Purpose("Check that the system allows to update notification settings.")
    public void updateNotificationSettingsNullHours() throws EntityNotFoundException {

        // Define initial
        final Long id = 0L;
        final NotificationSettings initial = new NotificationSettings();
        initial.setId(id);
        initial.setDays(2);
        initial.setHours(1);
        initial.setFrequency(NotificationFrequency.DAILY);

        // Define passed DTO and corresponding expected pojo
        final NotificationSettingsDTO dto = new NotificationSettingsDTO();
        dto.setDays(0);
        dto.setHours(null);
        dto.setFrequency(NotificationFrequency.MONTHLY);
        final NotificationSettings expected = new NotificationSettings();
        initial.setId(id);
        initial.setDays(0);
        initial.setHours(null);
        initial.setFrequency(NotificationFrequency.MONTHLY);

        // Mock the repository returned value
        Mockito.when(notificationSettingsRepository.existsById(id)).thenReturn(true);
        Mockito.when(notificationSettingsRepository.findById(id)).thenReturn(Optional.of(initial));
        Mockito.when(notificationSettingsRepository.findOneByProjectUserEmail("")).thenReturn(expected);
        Mockito.when(authenticationResolver.getUser()).thenReturn("");

        // Perform the update
        notificationSettingsService.updateNotificationSettings(dto);

        // Check that the repository's method was called with right arguments
        Mockito.verify(notificationSettingsRepository).save(Mockito.refEq(expected));
    }

    /**
     * Check that the system allows to update notification settings.
     *
     * @throws EntityNotFoundException
     *             thrown when no current user could be found
     */
    @Test
    @Requirement("REGARDS_DSL_DAM_CQA_040")
    @Purpose("Check that the system allows to update notification settings.")
    public void updateNotificationSettingsNullFrequency() throws EntityNotFoundException {

        // Define initial
        final Long id = 0L;
        final NotificationSettings initial = new NotificationSettings();
        initial.setId(id);
        initial.setDays(1);
        initial.setHours(0);
        initial.setFrequency(NotificationFrequency.MONTHLY);

        // Define passed DTO and corresponding expected pojo
        final NotificationSettingsDTO dto = new NotificationSettingsDTO();
        dto.setDays(0);
        dto.setHours(2);
        dto.setFrequency(null);
        final NotificationSettings expected = new NotificationSettings();
        initial.setId(id);
        initial.setDays(0);
        initial.setHours(2);
        initial.setFrequency(null);

        // Mock the repository returned value
        Mockito.when(notificationSettingsRepository.existsById(id)).thenReturn(true);
        Mockito.when(notificationSettingsRepository.findById(id)).thenReturn(Optional.of(initial));
        Mockito.when(notificationSettingsRepository.findOneByProjectUserEmail("")).thenReturn(expected);
        Mockito.when(authenticationResolver.getUser()).thenReturn("");

        // Perform the update
        notificationSettingsService.updateNotificationSettings(dto);

        // Check that the repository's method was called with right arguments
        Mockito.verify(notificationSettingsRepository).save(Mockito.refEq(expected));
    }
}
