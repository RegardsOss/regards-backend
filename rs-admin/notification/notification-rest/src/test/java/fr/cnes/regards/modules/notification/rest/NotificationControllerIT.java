package fr.cnes.regards.modules.notification.rest;

import java.util.List;

import org.assertj.core.util.Lists;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.client.IRolesClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.notification.domain.Notification;
import fr.cnes.regards.modules.notification.domain.NotificationStatus;
import fr.cnes.regards.modules.notification.domain.NotificationType;
import fr.cnes.regards.modules.notification.domain.dto.NotificationDTO;
import fr.cnes.regards.modules.notification.service.INotificationService;
import fr.cnes.regards.modules.notification.service.SendingScheduler;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@TestPropertySource(locations = "classpath:application.properties")
@MultitenantTransactional
public class NotificationControllerIT extends AbstractRegardsTransactionalIT {

    @Autowired
    private SendingScheduler sendingScheduler;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IProjectUsersClient projectUsersClient;

    @Autowired
    private INotificationService notificationService;

    @Autowired
    private IRolesClient rolesClient;

    @Override
    protected Logger getLogger() {
        return LoggerFactory.getLogger(this.getClass());
    }

    @Test
    public void testCreateNotification() throws EntityNotFoundException {
        String roleName = DefaultRole.PROJECT_ADMIN.name();
        ProjectUser pu = new ProjectUser("project.admin@test.fr",
                                         new Role(roleName),
                                         Lists.newArrayList(),
                                         Lists.newArrayList());
        Mockito.when(projectUsersClient.retrieveRoleProjectUsersList(roleName, 0, 100)).thenReturn(new ResponseEntity<>(
                HateoasUtils.wrapToPagedResources(Lists.newArrayList(pu)),
                HttpStatus.OK));
        NotificationDTO notif = new NotificationDTO("Lets test",
                                                    Sets.newHashSet(),
                                                    Sets.newHashSet(roleName),
                                                    "microservice",
                                                    "test",
                                                    NotificationType.INFO);
        Mockito.when(rolesClient.retrieveRoleAscendants(roleName))
                .thenReturn(new ResponseEntity<>(Sets.newHashSet(new Role(roleName)), HttpStatus.OK));

        List<ResultMatcher> expectations = Lists.newArrayList();
        expectations.add(MockMvcResultMatchers.status().isCreated());

        performDefaultPost(NotificationController.NOTIFICATION_PATH, notif, expectations, "error");
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        sendingScheduler.sendDaily();
    }

    @Test
    public void testListNotif() throws EntityNotFoundException {
        String roleName = DefaultRole.PROJECT_ADMIN.name();
        Role pa = new Role(roleName);
        ProjectUser pu = new ProjectUser("project.admin@test.fr", pa, Lists.newArrayList(), Lists.newArrayList());
        Mockito.when(projectUsersClient.retrieveRoleProjectUserList(pa.getId(), 0, 100))
                .thenReturn(new ResponseEntity<>(HateoasUtils.wrapToPagedResources(Lists.newArrayList(pu)),
                                                 HttpStatus.OK));
        NotificationDTO notif = new NotificationDTO("Bonne",
                                                    Sets.newHashSet(),
                                                    Sets.newHashSet(roleName),
                                                    "microservice",
                                                    "test",
                                                    NotificationType.INFO);
        notificationService.createNotification(notif);
        notif = new NotificationDTO("Année",
                                    Sets.newHashSet(),
                                    Sets.newHashSet(roleName),
                                    "microservice",
                                    "test",
                                    NotificationType.INFO);
        Mockito.when(rolesClient.retrieveRoleAscendants(roleName))
                .thenReturn(new ResponseEntity<>(Sets.newHashSet(new Role(roleName)), HttpStatus.OK));
        notificationService.createNotification(notif);
        notif = new NotificationDTO("2018",
                                    Sets.newHashSet(),
                                    Sets.newHashSet(roleName),
                                    "microservice",
                                    "test",
                                    NotificationType.INFO);
        Mockito.when(rolesClient.retrieveRoleAscendants(roleName))
                .thenReturn(new ResponseEntity<>(Sets.newHashSet(new Role(roleName)), HttpStatus.OK));
        notificationService.createNotification(notif);
        //some lorem ipsum so we have a notification with content
        notif = new NotificationDTO(
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Phasellus sed magna turpis. Curabitur ultrices scelerisque magna pretium mollis. Sed suscipit, ligula eu tempus pretium, lorem quam vehicula urna, vel efficitur leo mauris quis mauris. Pellentesque ac ullamcorper lectus. Aliquam sed tempor massa. Proin ex massa, sodales vel turpis non, sodales rhoncus lacus. Maecenas a convallis nisi. Aliquam felis justo, pellentesque id vestibulum id, tempus sit amet dui. Quisque quis lacus vehicula, gravida lectus a, elementum erat. In vitae venenatis turpis, et venenatis lacus. Phasellus facilisis pellentesque elit, in lacinia enim placerat quis.",
                Sets.newHashSet(),
                Sets.newHashSet(roleName),
                "microservice",
                "test",
                NotificationType.INFO);
        Mockito.when(rolesClient.retrieveRoleAscendants(roleName))
                .thenReturn(new ResponseEntity<>(Sets.newHashSet(new Role(roleName)), HttpStatus.OK));
        notificationService.createNotification(notif);
        RequestBuilderCustomizer requestCustomizer = getNewRequestBuilderCustomizer();
        requestCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT, Matchers.hasSize(4)));
        String token = jwtService.generateToken(DEFAULT_TENANT, "project.admin@test.fr", pa.getName());
        performGet(NotificationController.NOTIFICATION_PATH,
                   token,
                   requestCustomizer,
                   "Could not retrieve notifications");
    }

    @Test
    public void testSetNotifRead() throws EntityNotFoundException {
        String roleName = DefaultRole.PROJECT_ADMIN.name();
        Role pa = new Role(roleName);
        ProjectUser pu = new ProjectUser("project.admin@test.fr", pa, Lists.newArrayList(), Lists.newArrayList());
        Mockito.when(projectUsersClient.retrieveRoleProjectUserList(pa.getId(), 0, 100))
                .thenReturn(new ResponseEntity<>(HateoasUtils.wrapToPagedResources(Lists.newArrayList(pu)),
                                                 HttpStatus.OK));
        NotificationDTO notificationDTO = new NotificationDTO("Bonne",
                                                              Sets.newHashSet(),
                                                              Sets.newHashSet(roleName),
                                                              "microservice",
                                                              "test",
                                                              NotificationType.INFO);
        Mockito.when(rolesClient.retrieveRoleAscendants(roleName))
                .thenReturn(new ResponseEntity<>(Sets.newHashSet(new Role(roleName)), HttpStatus.OK));
        Notification notification = notificationService.createNotification(notificationDTO);
        RequestBuilderCustomizer requestCustomizer = getNewRequestBuilderCustomizer();
        requestCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        performDefaultPut(NotificationController.NOTIFICATION_PATH + NotificationController.NOTIFICATION_READ_PATH,
                          null,
                          requestCustomizer,
                          "Could not set the notification to READ",
                          notification.getId());
    }

    @Test
    public void testSetNotifUnread() throws EntityNotFoundException {
        //create a notification
        String roleName = DefaultRole.PROJECT_ADMIN.name();
        Role pa = new Role(roleName);
        ProjectUser pu = new ProjectUser("project.admin@test.fr", pa, Lists.newArrayList(), Lists.newArrayList());
        Mockito.when(projectUsersClient.retrieveRoleProjectUserList(pa.getId(), 0, 100))
                .thenReturn(new ResponseEntity<>(HateoasUtils.wrapToPagedResources(Lists.newArrayList(pu)),
                                                 HttpStatus.OK));
        NotificationDTO notificationDTO = new NotificationDTO("Bonne",
                                                              Sets.newHashSet(),
                                                              Sets.newHashSet(roleName),
                                                              "microservice",
                                                              "test",
                                                              NotificationType.INFO);
        Mockito.when(rolesClient.retrieveRoleAscendants(roleName))
                .thenReturn(new ResponseEntity<>(Sets.newHashSet(new Role(roleName)), HttpStatus.OK));
        Notification notification = notificationService.createNotification(notificationDTO);
        // set the notification to read
        notificationService.updateNotificationStatus(notification.getId(), NotificationStatus.READ);
        // ask to set the notification to unread
        RequestBuilderCustomizer requestCustomizer = getNewRequestBuilderCustomizer();
        requestCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        performDefaultPut(NotificationController.NOTIFICATION_PATH + NotificationController.NOTIFICATION_UNREAD_PATH,
                          null,
                          requestCustomizer,
                          "Could not set the notification to UNREAD",
                          notification.getId());
    }

    @Configuration
    static class Conf {

        @Bean
        public IProjectUsersClient projectUsersClient() {
            return Mockito.mock(IProjectUsersClient.class);
        }

        @Bean
        public IEmailClient emailClient() {
            return Mockito.mock(IEmailClient.class);
        }

        @Bean
        public IRolesClient rolesClient() {
            return Mockito.mock(IRolesClient.class);
        }

    }
}
