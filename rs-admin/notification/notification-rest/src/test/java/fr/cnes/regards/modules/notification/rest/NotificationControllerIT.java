package fr.cnes.regards.modules.notification.rest;

import java.util.Arrays;
import java.util.HashSet;

import org.assertj.core.util.Lists;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.notification.dao.INotificationRepository;
import fr.cnes.regards.modules.notification.dao.INotificationSettingsRepository;
import fr.cnes.regards.modules.notification.domain.Notification;
import fr.cnes.regards.modules.notification.domain.NotificationStatus;
import fr.cnes.regards.modules.notification.service.INotificationService;
import fr.cnes.regards.modules.notification.service.SendingScheduler;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@TestPropertySource(locations = "classpath:application.properties")
@ContextConfiguration(classes = { NotificationControllerIT.Config.class })
public class NotificationControllerIT extends AbstractRegardsTransactionalIT {

    @Configuration
    public static class Config {

        @Bean
        @Primary
        public IRoleService rolesClient() {
            return Mockito.mock(IRoleService.class);
        }

        @Bean
        public IEmailClient emailClient() {
            return Mockito.mock(IEmailClient.class);
        }
    }

    @Autowired
    private SendingScheduler sendingScheduler;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private INotificationService notificationService;

    @Autowired
    private IRoleService roleService;

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
        ProjectUser pu = new ProjectUser("project.admin@test.fr",
                                         new Role(roleName),
                                         Lists.newArrayList(),
                                         Lists.newArrayList());
        Mockito.when(roleService.retrieveRoleProjectUserList(roleName, PageRequest.of(0, 100)))
                .thenReturn(new PageImpl<>(Lists.newArrayList(pu)));
        NotificationDTO notif = new NotificationDtoBuilder("Lets test", "test", NotificationLevel.INFO, "microservice")
                .toRoles(new HashSet<>(Arrays.asList(roleName)));
        Mockito.when(roleService.getDescendants(roleService.retrieveRole(roleName)))
                .thenReturn(Sets.newHashSet(new Role(roleName)));

        performDefaultPost(NotificationController.NOTIFICATION_PATH,
                           notif,
                           customizer().expectStatusCreated(),
                           "error");
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        sendingScheduler.sendDaily();
    }

    @Test
    public void testListNotif() throws EntityNotFoundException {
        String roleName = DefaultRole.PROJECT_ADMIN.name();
        Role pa = new Role(roleName);
        ProjectUser pu = new ProjectUser("project.admin@test.fr", pa, Lists.newArrayList(), Lists.newArrayList());
        Mockito.when(roleService.retrieveRoleProjectUserList(pa.getId(), PageRequest.of(0, 100)))
                .thenReturn(new PageImpl<>(Lists.newArrayList(pu)));
        Mockito.when(roleService.getDescendants(roleService.retrieveRole(roleName)))
                .thenReturn(Sets.newHashSet(new Role(roleName)));
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
                "test",
                NotificationLevel.INFO,
                "microservice").toRoles(new HashSet<>(Arrays.asList(roleName)));
        notificationService.createNotification(notif);
        String token = jwtService.generateToken(getDefaultTenant(), "project.admin@test.fr", roleName);
        //        performGet(NotificationController.NOTIFICATION_PATH, token,
        //                   customizer().expectStatusOk().expectToHaveSize(JSON_PATH_CONTENT, 0)
        //                           .addParameter("state", NotificationStatus.READ.toString()),
        //                   "Could not retrieve notifications");
        //
        //        performGet(NotificationController.NOTIFICATION_PATH, token,
        //                   customizer().expectStatusOk().expectToHaveSize(JSON_PATH_CONTENT, 4),
        //                   "Could not retrieve notifications");

        performGet(NotificationController.NOTIFICATION_PATH,
                   token,
                   customizer().expectStatusOk().expectToHaveSize(JSON_PATH_CONTENT, 4)
                           .addParameter("state", NotificationStatus.UNREAD.toString()),
                   "Could not retrieve notifications");

        performGet(NotificationController.NOTIFICATION_PATH,
                   token,
                   customizer().expectStatusOk().expectToHaveSize(JSON_PATH_CONTENT, 2)
                           .addParameter("state", NotificationStatus.UNREAD.toString()).addParameter("page", "0")
                           .addParameter("size", "2"),
                   "Could not retrieve notifications");
        Assert.assertEquals(0, 0);
    }

    @Test
    public void testSetNotifRead() throws EntityNotFoundException {
        String roleName = DefaultRole.PROJECT_ADMIN.name();
        Role pa = new Role(roleName);
        ProjectUser pu = new ProjectUser("project.admin@test.fr", pa, Lists.newArrayList(), Lists.newArrayList());
        Mockito.when(roleService.retrieveRoleProjectUserList(pa.getId(), PageRequest.of(0, 100)))
                .thenReturn(new PageImpl<>(Lists.newArrayList(pu)));
        NotificationDTO notificationDTO = new NotificationDtoBuilder("Bonne",
                                                                     "test",
                                                                     NotificationLevel.INFO,
                                                                     "microservice")
                .toRoles(new HashSet<>(Arrays.asList(roleName)));
        Mockito.when(roleService.getDescendants(roleService.retrieveRole(roleName)))
                .thenReturn(Sets.newHashSet(new Role(roleName)));
        Notification notification = notificationService.createNotification(notificationDTO);
        performDefaultPut(NotificationController.NOTIFICATION_PATH + NotificationController.NOTIFICATION_READ_PATH,
                          null,
                          customizer().expectStatusOk(),
                          "Could not set the notification to READ",
                          notification.getId());
    }

    @Test
    public void testSetNotifUnread() throws EntityNotFoundException {
        //create a notification
        String roleName = DefaultRole.PROJECT_ADMIN.name();
        Role pa = new Role(roleName);
        ProjectUser pu = new ProjectUser("project.admin@test.fr", pa, Lists.newArrayList(), Lists.newArrayList());
        Mockito.when(roleService.retrieveRoleProjectUserList(pa.getId(), PageRequest.of(0, 100)))
                .thenReturn(new PageImpl<>(Lists.newArrayList(pu)));
        NotificationDTO notificationDTO = new NotificationDtoBuilder("Bonne",
                                                                     "test",
                                                                     NotificationLevel.INFO,
                                                                     "microservice")
                .toRoles(new HashSet<>(Arrays.asList(roleName)));
        Mockito.when(roleService.getDescendants(roleService.retrieveRole(roleName)))
                .thenReturn(Sets.newHashSet(new Role(roleName)));
        Notification notification = notificationService.createNotification(notificationDTO);
        // set the notification to read
        notificationService.updateNotificationStatus(notification.getId(), NotificationStatus.READ);
        // ask to set the notification to unread
        performDefaultPut(NotificationController.NOTIFICATION_PATH + NotificationController.NOTIFICATION_UNREAD_PATH,
                          null,
                          customizer().expectStatusOk(),
                          "Could not set the notification to UNREAD",
                          notification.getId());
    }

    @Test
    public void testRetrieveNotifSetting() {
        performDefaultGet(NotificationController.NOTIFICATION_PATH + NotificationController.NOTIFICATION_SETTINGS,
                          customizer().expectStatusOk()
                                  .expect(MockMvcResultMatchers.jsonPath("$.id", Matchers.notNullValue(Long.class)))
                                  .expect(MockMvcResultMatchers
                                                  .jsonPath("$.projectUserEmail", Matchers.notNullValue(Long.class)))
                                  .expect(MockMvcResultMatchers
                                                  .jsonPath("$.frequency", Matchers.notNullValue(Long.class))),
                          "could not retrieve notification settings");
    }
}
