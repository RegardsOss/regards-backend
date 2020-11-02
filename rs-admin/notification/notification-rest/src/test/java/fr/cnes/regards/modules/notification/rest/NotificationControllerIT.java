package fr.cnes.regards.modules.notification.rest;

import java.util.Arrays;
import java.util.HashSet;

import javax.mail.internet.MimeMessage;

import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.notification.NotificationDTO;
import fr.cnes.regards.framework.notification.NotificationDtoBuilder;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountSettingsClient;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.notification.dao.INotificationRepository;
import fr.cnes.regards.modules.notification.dao.INotificationSettingsRepository;
import fr.cnes.regards.modules.notification.domain.Notification;
import fr.cnes.regards.modules.notification.domain.NotificationStatus;
import fr.cnes.regards.modules.notification.service.INotificationService;
import fr.cnes.regards.modules.notification.service.SendingScheduler;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@TestPropertySource(locations = "classpath:application.properties")
@ContextConfiguration(classes = { NotificationControllerIT.Config.class })
public class NotificationControllerIT extends AbstractRegardsTransactionalIT {

    @Configuration
    public static class Config {

        @Bean
        public JavaMailSender mockSender() {
            final JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
            Mockito.when(mailSender.createMimeMessage()).thenReturn(Mockito.mock(MimeMessage.class));
            return mailSender;
        }

        @Bean
        public IAccountsClient accountClient() {
            return Mockito.mock(IAccountsClient.class);
        }

        @Bean
        public IStorageRestClient storageRestClient() {
            return Mockito.mock(IStorageRestClient.class);
        }

        @Bean
        public IAccountSettingsClient settingClient() {
            return Mockito.mock(IAccountSettingsClient.class);
        }

        @Bean
        public IProjectsClient projectsClient() {
            return Mockito.mock(IProjectsClient.class);
        }
    }

    @Autowired
    private SendingScheduler sendingScheduler;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private INotificationService notificationService;

    @Autowired
    private INotificationRepository notificationRepo;

    @Autowired
    private INotificationSettingsRepository notificationSettingsRepo;

    @Override
    protected Logger getLogger() {
        return LoggerFactory.getLogger(this.getClass());
    }

    @Before
    public void init() {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
    }

    @After
    public void cleanUp() {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        notificationRepo.deleteAll();
        notificationSettingsRepo.deleteAll();
    }

    @Test
    public void testCreateNotification() throws EntityNotFoundException {
        String roleName = DefaultRole.PROJECT_ADMIN.name();
        NotificationDTO notif = new NotificationDtoBuilder("Lets test", "test", NotificationLevel.INFO, "microservice")
                .toRoles(new HashSet<>(Arrays.asList(roleName)));

        performDefaultPost(NotificationController.NOTIFICATION_PATH, notif, customizer().expectStatusCreated(),
                           "error");
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        sendingScheduler.sendDaily();
    }

    @Test
    public void testListNotif() throws EntityNotFoundException {
        String roleName = DefaultRole.PROJECT_ADMIN.name();
        NotificationDTO notif = new NotificationDtoBuilder("Bonne", "test", NotificationLevel.INFO, "microservice")
                .toRoles(Sets.newHashSet(roleName));
        notificationService.createNotification(notif);
        notif = new NotificationDtoBuilder("Année", "test", NotificationLevel.INFO, "microservice")
                .toRoles(Sets.newHashSet(roleName));
        notificationService.createNotification(notif);
        notif = new NotificationDtoBuilder("2018", "test", NotificationLevel.INFO, "microservice")
                .toRoles(Sets.newHashSet(roleName));
        notificationService.createNotification(notif);
        //some lorem ipsum so we have a notification with content
        notif = new NotificationDtoBuilder(
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Phasellus sed magna turpis. Curabitur ultrices scelerisque magna pretium mollis. Sed suscipit, ligula eu tempus pretium, lorem quam vehicula urna, vel efficitur leo mauris quis mauris. Pellentesque ac ullamcorper lectus. Aliquam sed tempor massa. Proin ex massa, sodales vel turpis non, sodales rhoncus lacus. Maecenas a convallis nisi. Aliquam felis justo, pellentesque id vestibulum id, tempus sit amet dui. Quisque quis lacus vehicula, gravida lectus a, elementum erat. In vitae venenatis turpis, et venenatis lacus. Phasellus facilisis pellentesque elit, in lacinia enim placerat quis.",
                "test", NotificationLevel.INFO, "microservice").toRoles(new HashSet<>(Arrays.asList(roleName)));
        notificationService.createNotification(notif);
        String token = jwtService.generateToken(getDefaultTenant(), "project.admin@test.fr", roleName);

        performGet(NotificationController.NOTIFICATION_PATH, token,
                   customizer().expectStatusOk().expectToHaveSize(JSON_PATH_CONTENT, 4).expect(MockMvcResultMatchers.jsonPath("$.content[0].content.message").doesNotExist())
                           .addParameter("state", NotificationStatus.UNREAD.toString()),
                   "Could not retrieve notifications");

        performGet(NotificationController.NOTIFICATION_PATH, token,
                   customizer().expectStatusOk().expectToHaveSize(JSON_PATH_CONTENT, 2).expect(MockMvcResultMatchers.jsonPath("$.content[0].content.message").doesNotExist())
                           .addParameter("state", NotificationStatus.UNREAD.toString()).addParameter("page", "0")
                           .addParameter("size", "2"),
                   "Could not retrieve notifications");
    }

    @Test
    public void testNotifSummary() {
        String token = jwtService.generateToken(getDefaultTenant(), "project.admin@test.fr", DefaultRole.PROJECT_ADMIN.toString());
        performGet(NotificationController.NOTIFICATION_PATH + NotificationController.SUMMARY_PATH, token,
                   customizer().expectStatusOk(), "Could not retrieve notification summary");
    }

    @Test
    public void testSetNotifRead() throws EntityNotFoundException {
        String roleName = DefaultRole.PROJECT_ADMIN.name();
        NotificationDTO notificationDTO = new NotificationDtoBuilder("Bonne", "test", NotificationLevel.INFO,
                "microservice").toRoles(new HashSet<>(Arrays.asList(roleName)));
        Notification notification = notificationService.createNotification(notificationDTO);
        performDefaultPut(NotificationController.NOTIFICATION_PATH + NotificationController.NOTIFICATION_READ_PATH,
                          null, customizer().expectStatusOk(), "Could not set the notification to READ",
                          notification.getId());
    }

    @Test
    public void testSetNotifUnread() throws EntityNotFoundException {
        //create a notification
        String roleName = DefaultRole.PROJECT_ADMIN.name();
        NotificationDTO notificationDTO = new NotificationDtoBuilder("Bonne", "test", NotificationLevel.INFO,
                "microservice").toRoles(new HashSet<>(Arrays.asList(roleName)));
        Notification notification = notificationService.createNotification(notificationDTO);
        // set the notification to read
        notificationService.updateNotificationStatus(notification.getId(), NotificationStatus.READ);
        // ask to set the notification to unread
        performDefaultPut(NotificationController.NOTIFICATION_PATH + NotificationController.NOTIFICATION_UNREAD_PATH,
                          null, customizer().expectStatusOk(), "Could not set the notification to UNREAD",
                          notification.getId());
    }

    @Test
    public void testRetrieveNotifSetting() {
        performDefaultGet(NotificationController.NOTIFICATION_PATH + NotificationController.NOTIFICATION_SETTINGS,
                          customizer().expectStatusOk()
                                  .expect(MockMvcResultMatchers.jsonPath("$.id", Matchers.notNullValue(Long.class)))
                                  .expect(MockMvcResultMatchers.jsonPath("$.projectUserEmail",
                                                                         Matchers.notNullValue(Long.class)))
                                  .expect(MockMvcResultMatchers.jsonPath("$.frequency",
                                                                         Matchers.notNullValue(Long.class))),
                          "could not retrieve notification settings");
    }
}
