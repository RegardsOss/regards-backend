/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.event.RoleEvent;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.client.IRolesClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUserAction;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUserEvent;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.notification.dao.INotificationRepository;
import fr.cnes.regards.modules.notification.domain.Notification;
import fr.cnes.regards.modules.notification.domain.NotificationMode;
import fr.cnes.regards.modules.notification.domain.NotificationStatus;
import fr.cnes.regards.modules.notification.domain.NotificationToSendEvent;
import fr.cnes.regards.modules.notification.domain.NotificationType;
import fr.cnes.regards.modules.notification.domain.dto.NotificationDTO;

/**
 * {@link INotificationService} implementation
 *
 * @author Xavier-Alexandre Brochard
 * @author Sébastien Binda
 * @author Sylvain Vissiere-Guerinet
 */
@Service
@RegardsTransactional
public class NotificationService implements INotificationService, ApplicationListener<ApplicationReadyEvent> {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(NotificationService.class);

    /**
     * CRUD repository managing notifications. Autowired by Spring.
     */
    private final INotificationRepository notificationRepository;

    /**
     * CRUD repository managing roles. Autowired by Spring.
     */
    private final IRolesClient rolesClient;

    /**
     * Feign client for {@link Role}s. Autowired by Spring.
     */
    private final IProjectUsersClient projectUserClient;

    /**
     * Application event publisher
     */
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Notification mode:
     */
    private final NotificationMode notificationMode;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final IAuthenticationResolver authenticationResolver;

    private final ISubscriber subscriber;

    /**
     * Creates a {@link NotificationService} wired to the given {@link INotificationRepository}.
     *  @param notificationRepository
     *            Autowired by Spring. Must not be {@literal null}.
     * @param rolesClient
     *            Autowired by Spring. Must not be {@literal null}.
     * @param projectUserClient
     *            Autowired by Spring. Must not be {@literal null}.
     */
    public NotificationService(final INotificationRepository notificationRepository, final IRolesClient rolesClient,
            final IProjectUsersClient projectUserClient, final ApplicationEventPublisher applicationEventPublisher,
            IRuntimeTenantResolver runtimeTenantResolver, IAuthenticationResolver authenticationResolver,
            ISubscriber subscriber,
            @Value("${regards.notification.mode:MULTITENANT}") NotificationMode notificationMode) {
        super();
        this.notificationRepository = notificationRepository;
        this.rolesClient = rolesClient;
        this.projectUserClient = projectUserClient;
        this.applicationEventPublisher = applicationEventPublisher;
        this.notificationMode = notificationMode;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.authenticationResolver = authenticationResolver;
        this.subscriber = subscriber;
    }

    @Override
    public List<Notification> retrieveNotifications() throws EntityNotFoundException {
        if (notificationMode == NotificationMode.MULTITENANT) {
            return notificationRepository
                    .findByRecipientsContaining(authenticationResolver.getUser(), authenticationResolver.getRole());
        } else {
            return notificationRepository.findAll();
        }
    }

    @Override
    public Notification createNotification(final NotificationDTO pDto) {
        Notification notification = new Notification();
        notification.setDate(OffsetDateTime.now());
        notification.setMessage(pDto.getMessage());
        notification.setTitle(pDto.getTitle());
        notification.setSender(pDto.getSender());
        notification.setStatus(NotificationStatus.UNREAD);
        notification.setType(pDto.getType());
        notification.setProjectUserRecipients(pDto.getProjectUserRecipients());

        notification.setRoleRecipients(getAllRecipientRoles(pDto.getRoleRecipients()));

        // check the notification type and send it immediately if FATAL or ERROR
        if (notification.getType() == NotificationType.FATAL || notification.getType() == NotificationType.ERROR) {
            applicationEventPublisher.publishEvent(new NotificationToSendEvent(notification));
        }

        // Save it in db
        notification = notificationRepository.save(notification);

        // TODO Trigger NOTIFICATION event on message broker

        return notification;
    }

    /**
     * Lets get all roles that should have the notification through the role hierarchy
     * @param roleRecipients
     * @return all recipient role names
     */
    private Set<String> getAllRecipientRoles(Set<String> roleRecipients) {
        Set<String> allRecipientRoleNames = new HashSet<>();
        for (String roleName : roleRecipients) {
            ResponseEntity<Set<Role>> response = rolesClient.retrieveRoleDescendants(roleName);
            if (response.getStatusCode().is2xxSuccessful()) {
                response.getBody().forEach(r -> allRecipientRoleNames.add(r.getName()));
            }
        }
        return allRecipientRoleNames;
    }

    @Override
    public Notification retrieveNotification(final Long pId) throws EntityNotFoundException {
        if (!notificationRepository.exists(pId)) {
            throw new EntityNotFoundException(pId.toString(), Notification.class);
        }
        return notificationRepository.findOne(pId);
    }

    @Override
    public Notification updateNotificationStatus(final Long pId, final NotificationStatus pStatus)
            throws EntityNotFoundException {
        if (!notificationRepository.exists(pId)) {
            throw new EntityNotFoundException(pId.toString(), Notification.class);
        }
        final Notification notification = notificationRepository.findOne(pId);
        notification.setStatus(pStatus);
        return notificationRepository.save(notification);
    }

    @Override
    public void deleteNotification(final Long pId) throws EntityNotFoundException {
        if (!notificationRepository.exists(pId)) {
            throw new EntityNotFoundException(pId.toString(), Notification.class);
        }
        notificationRepository.delete(pId);
    }

    @Override
    public List<Notification> retrieveNotificationsToSend() {
        return notificationRepository.findByStatus(NotificationStatus.UNREAD);
    }

    @Override
    public Stream<String> findRecipients(final Notification pNotification) {
        // With the stream of role recipients and project users recipients
        try (final Stream<String> rolesStream = pNotification.getRoleRecipients().stream();
                final Stream<String> usersStream = pNotification.getProjectUserRecipients().stream()) {

            // Merge the two streams
            return Stream.concat(usersStream,
                                 rolesStream.flatMap(// Define a function mapping each role to its project users by
                                                     // calling the roles client
                                                     r -> HateoasUtils.retrieveAllPages(100,
                                                                                        pageable -> retrieveRoleProjectUserList(
                                                                                                r,
                                                                                                pageable)).stream()
                                                             .map(ProjectUser::getEmail))).distinct();
        }
    }

    private ResponseEntity<PagedResources<Resource<ProjectUser>>> retrieveRoleProjectUserList(String pRole,
            Pageable pPageable) {
        final ResponseEntity<PagedResources<Resource<ProjectUser>>> response = projectUserClient
                .retrieveRoleProjectUsersList(pRole, pPageable.getPageNumber(), pPageable.getPageSize());

        if (!response.getStatusCode().equals(HttpStatus.OK) || (response.getBody() == null)) {
            LOG.warn("Error retrieving projet users for role {}. Remote administration response is {}",
                     pRole,
                     response.getStatusCode());
        }
        return response;
    }

    @Override
    public void removeReceiver(String email) {
        Set<Notification> notifications = notificationRepository.findAllByProjectUserRecipientsContaining(email);
        notifications.forEach(notification -> notification.getProjectUserRecipients().remove(email));
        notificationRepository.save(notifications);
    }

    @Override
    public void removeRoleReceiver(String role) {
        Set<Notification> notifications = notificationRepository.findAllByRoleRecipientsContaining(role);
        notifications.forEach(notification -> notification.getRoleRecipients().remove(role));
        notificationRepository.save(notifications);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (notificationMode == NotificationMode.MULTITENANT) {
            subscriber.subscribeTo(ProjectUserEvent.class, new ProjectUserEventListener());
            subscriber.subscribeTo(RoleEvent.class, new RoleEventListener());
        }
    }

    private class ProjectUserEventListener implements IHandler<ProjectUserEvent> {

        @Override
        public void handle(TenantWrapper<ProjectUserEvent> wrapper) {
            ProjectUserEvent event = wrapper.getContent();
            String tenant = wrapper.getTenant();
            try {
                FeignSecurityManager.asSystem();
                runtimeTenantResolver.forceTenant(tenant);
                if (event.getAction() == ProjectUserAction.DELETION) {
                    NotificationService.this.removeReceiver(event.getEmail());
                }
            } finally {
                runtimeTenantResolver.clearTenant();
                FeignSecurityManager.reset();
            }
        }
    }

    private class RoleEventListener implements IHandler<RoleEvent> {

        @Override
        public void handle(TenantWrapper<RoleEvent> wrapper) {
            String tenant = wrapper.getTenant();
            RoleEvent event = wrapper.getContent();
            try {
                FeignSecurityManager.asSystem();
                runtimeTenantResolver.forceTenant(tenant);
                ResponseEntity<Resource<Role>> roleResponse = rolesClient.retrieveRole(event.getRole());
                if (roleResponse.getStatusCode() == HttpStatus.NOT_FOUND) {
                    // then the role was deleted
                    NotificationService.this.removeRoleReceiver(event.getRole());
                }
            } finally {
                runtimeTenantResolver.clearTenant();
                FeignSecurityManager.reset();
            }
        }
    }
}
