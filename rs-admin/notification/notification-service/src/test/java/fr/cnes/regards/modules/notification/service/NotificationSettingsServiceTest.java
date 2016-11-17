/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.notification.service;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;
import fr.cnes.regards.modules.notification.dao.INotificationSettingsRepository;
import fr.cnes.regards.modules.notification.domain.NotificationFrequency;
import fr.cnes.regards.modules.notification.domain.NotificationSettings;
import fr.cnes.regards.modules.notification.domain.dto.NotificationSettingsDTO;

/**
 * Test class for {@link notificationSettingsService}.
 *
 * @author CS SI
 */
public class NotificationSettingsServiceTest {

    /**
     * Tested service
     */
    private NotificationSettingsService notificationSettingsService;

    /**
     * Service handling CRUD operations on project users. Autowired by Spring.
     */
    private IProjectUserService projectUserService;

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
        projectUserService = Mockito.mock(IProjectUserService.class);
        notificationSettingsRepository = Mockito.mock(INotificationSettingsRepository.class);

        // Instanciate the tested service
        notificationSettingsService = new NotificationSettingsService(projectUserService,
                notificationSettingsRepository);
    }

    /**
     * Check that the system allows to retrieve the notification settings of currently logged user.
     */
    @Test
    @Requirement("REGARDS_DSL_DAM_CQA_040")
    @Purpose("Check that the system allows to retrieve the notification settings of currently logged user.")
    public void retrieveNotificationsAlreadyExists() {
        // Define expected
        final NotificationSettings expected = new NotificationSettings();

        // Define logged user
        final ProjectUser loggedUser = new ProjectUser();

        // Mock methods
        Mockito.when(projectUserService.retrieveCurrentUser()).thenReturn(loggedUser);
        Mockito.when(notificationSettingsRepository.findOneByProjectUser(loggedUser)).thenReturn(expected);

        // Call tested method
        final NotificationSettings actual = notificationSettingsService.retrieveNotificationSettings();

        // Check that expected is equel to acutal
        Assert.assertThat(actual, CoreMatchers.is(CoreMatchers.equalTo(expected)));

        // Check that the repository's method was called with right arguments
        Mockito.verify(notificationSettingsRepository).findOneByProjectUser(loggedUser);
    }

    /**
     * Check that the system creates empty settings when trying to retrieve not existing settings.
     */
    @Test
    @Requirement("REGARDS_DSL_DAM_CQA_040")
    @Purpose("Check that the system creates empty settings when trying to retrieve not existing settings.")
    public void retrieveNotificationsNotExists() {
        // Defined expected
        final NotificationSettings expected = new NotificationSettings();

        // Define logged user
        final ProjectUser loggedUser = new ProjectUser();

        // Mock methods
        Mockito.when(projectUserService.retrieveCurrentUser()).thenReturn(loggedUser);
        Mockito.when(notificationSettingsRepository.findOneByProjectUser(loggedUser)).thenReturn(null);
        Mockito.when(notificationSettingsRepository.save(Mockito.any(NotificationSettings.class))).thenReturn(expected);

        // Call tested method
        final NotificationSettings actual = notificationSettingsService.retrieveNotificationSettings();

        // Check that expected is equel to acutal
        Assert.assertThat(actual, CoreMatchers.is(CoreMatchers.equalTo(expected)));

        // Check that the repository's method was called with right arguments
        Mockito.verify(notificationSettingsRepository).findOneByProjectUser(loggedUser);
    }

    /**
     * Check that the system allows to update notification settings.
     */
    @Test
    @Requirement("REGARDS_DSL_DAM_CQA_040")
    @Purpose("Check that the system allows to update notification settings.")
    public void updateNotificationSettings() {
        // Define logged user
        final ProjectUser loggedUser = new ProjectUser();

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
        Mockito.when(notificationSettingsRepository.exists(id)).thenReturn(true);
        Mockito.when(notificationSettingsRepository.findOne(id)).thenReturn(initial);
        Mockito.when(notificationSettingsRepository.findOneByProjectUser(loggedUser)).thenReturn(expected);
        Mockito.when(projectUserService.retrieveCurrentUser()).thenReturn(loggedUser);

        // Perform the update
        notificationSettingsService.updateNotificationSettings(dto);

        // Check that the repository's method was called with right arguments
        Mockito.verify(notificationSettingsRepository).save(Mockito.refEq(expected));
    }

    /**
     * Check that the system allows to update notification settings.
     */
    @Test
    @Requirement("REGARDS_DSL_DAM_CQA_040")
    @Purpose("Check that the system allows to update notification settings.")
    public void updateNotificationSettingsNullDays() {
        // Define logged user
        final ProjectUser loggedUser = new ProjectUser();

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
        Mockito.when(notificationSettingsRepository.exists(id)).thenReturn(true);
        Mockito.when(notificationSettingsRepository.findOne(id)).thenReturn(initial);
        Mockito.when(notificationSettingsRepository.findOneByProjectUser(loggedUser)).thenReturn(expected);
        Mockito.when(projectUserService.retrieveCurrentUser()).thenReturn(loggedUser);

        // Perform the update
        notificationSettingsService.updateNotificationSettings(dto);

        // Check that the repository's method was called with right arguments
        Mockito.verify(notificationSettingsRepository).save(Mockito.refEq(expected));
    }

    /**
     * Check that the system allows to update notification settings.
     */
    @Test
    @Requirement("REGARDS_DSL_DAM_CQA_040")
    @Purpose("Check that the system allows to update notification settings.")
    public void updateNotificationSettingsNullHours() {
        // Define logged user
        final ProjectUser loggedUser = new ProjectUser();

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
        Mockito.when(notificationSettingsRepository.exists(id)).thenReturn(true);
        Mockito.when(notificationSettingsRepository.findOne(id)).thenReturn(initial);
        Mockito.when(notificationSettingsRepository.findOneByProjectUser(loggedUser)).thenReturn(expected);
        Mockito.when(projectUserService.retrieveCurrentUser()).thenReturn(loggedUser);

        // Perform the update
        notificationSettingsService.updateNotificationSettings(dto);

        // Check that the repository's method was called with right arguments
        Mockito.verify(notificationSettingsRepository).save(Mockito.refEq(expected));
    }

    /**
     * Check that the system allows to update notification settings.
     */
    @Test
    @Requirement("REGARDS_DSL_DAM_CQA_040")
    @Purpose("Check that the system allows to update notification settings.")
    public void updateNotificationSettingsNullFrequency() {
        // Define logged user
        final ProjectUser loggedUser = new ProjectUser();

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
        Mockito.when(notificationSettingsRepository.exists(id)).thenReturn(true);
        Mockito.when(notificationSettingsRepository.findOne(id)).thenReturn(initial);
        Mockito.when(notificationSettingsRepository.findOneByProjectUser(loggedUser)).thenReturn(expected);
        Mockito.when(projectUserService.retrieveCurrentUser()).thenReturn(loggedUser);

        // Perform the update
        notificationSettingsService.updateNotificationSettings(dto);

        // Check that the repository's method was called with right arguments
        Mockito.verify(notificationSettingsRepository).save(Mockito.refEq(expected));
    }
}
