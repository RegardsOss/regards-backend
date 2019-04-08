/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import fr.cnes.regards.framework.amqp.IInstanceSubscriber;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.notification.NotificationEvent;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.notification.NotificationDTO;
import fr.cnes.regards.framework.notification.NotificationLevel;
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

/**
 * {@link INotificationService} implementation
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
     * @param notificationRepository Autowired by Spring. Must not be {@literal null}.
     * @param rolesClient Autowired by Spring. Must not be {@literal null}.
     * @param projectUserClient Autowired by Spring. Must not be {@literal null}.
     */
    public NotificationService(INotificationRepository notificationRepository, IRolesClient rolesClient,
            IProjectUsersClient projectUserClient, ApplicationEventPublisher applicationEventPublisher,
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
    public Notification createNotification(NotificationDTO dto) {
        Notification notification = new Notification();
        notification.setDate(OffsetDateTime.now());
        notification.setMessage(dto.getMessage());
        notification.setTitle(dto.getTitle());
        notification.setSender(dto.getSender());
        notification.setStatus(NotificationStatus.UNREAD);
        notification.setLevel(dto.getLevel());
        notification.setMimeType(dto.getMimeType());
        notification.setProjectUserRecipients(dto.getProjectUserRecipients());

        notification.setRoleRecipients(getAllRecipientRoles(dto.getRoleRecipients()));

        // check the notification type and send it immediately if FATAL or ERROR
        if (notification.getLevel() == NotificationLevel.FATAL || notification.getLevel() == NotificationLevel.ERROR) {
            applicationEventPublisher.publishEvent(new NotificationToSendEvent(notification));
        }

        // Save it in db
        notification = notificationRepository.save(notification);

        return notification;
    }

    @Override
    public Page<Notification> retrieveNotifications(Pageable page) {
        if (notificationMode == NotificationMode.MULTITENANT) {
            return notificationRepository.findByRecipientsContaining(authenticationResolver.getUser(),
                                                                     authenticationResolver.getRole(),
                                                                     page);
        } else {
            return notificationRepository.findAll(page);
        }
    }

    /**
     * Lets get all roles that should have the notification through the role hierarchy
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
    public Notification retrieveNotification(Long pId) throws EntityNotFoundException {
        Optional<Notification> notifOpt = notificationRepository.findById(pId);
        if (!notifOpt.isPresent()) {
            throw new EntityNotFoundException(pId.toString(), Notification.class);
        }
        return notifOpt.get();
    }

    @Override
    public Notification updateNotificationStatus(Long pId, NotificationStatus pStatus) throws EntityNotFoundException {
        Optional<Notification> notifOpt = notificationRepository.findById(pId);
        if (!notifOpt.isPresent()) {
            throw new EntityNotFoundException(pId.toString(), Notification.class);
        }
        Notification notification = notifOpt.get();
        notification.setStatus(pStatus);
        return notificationRepository.save(notification);
    }

    @Override
    public void markAllNotificationAs(NotificationStatus status) {
        Assert.notNull(status, "Notification status is required");
        notificationRepository.updateAllNotificationStatusByRole(status.toString(), authenticationResolver.getRole());
        notificationRepository.updateAllNotificationStatusByUser(status.toString(), authenticationResolver.getUser());
    }

    @Override
    public void deleteNotification(Long pId) throws EntityNotFoundException {
        if (!notificationRepository.existsById(pId)) {
            throw new EntityNotFoundException(pId.toString(), Notification.class);
        }
        notificationRepository.deleteById(pId);
    }

    @Override
    public Page<Notification> retrieveNotificationsToSend(Pageable page) {
        return notificationRepository.findByStatus(NotificationStatus.UNREAD, page);
    }

    @Override
    public Stream<String> findRecipients(Notification pNotification) {
        // With the stream of role recipients and project users recipients
        try (Stream<String> rolesStream = pNotification.getRoleRecipients().stream();
                Stream<String> usersStream = pNotification.getProjectUserRecipients().stream()) {

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
        ResponseEntity<PagedResources<Resource<ProjectUser>>> response = projectUserClient
                .retrieveRoleProjectUsersList(pRole, pPageable.getPageNumber(), pPageable.getPageSize());

        if (!response.getStatusCode().equals(HttpStatus.OK) || response.getBody() == null) {
            LOG.warn("Error retrieving projet users for role {}. Remote administration response is {}",
                     pRole,
                     response.getStatusCode());
        }
        return response;
    }

    @Override
    public void removeReceiver(String email) {
        Pageable page = PageRequest.of(0, 500);
        Page<Notification> notifications;
        do {
            notifications = notificationRepository.findAllByProjectUserRecipientsContaining(email, page);
            notifications.forEach(notification -> notification.getProjectUserRecipients().remove(email));
            notificationRepository.saveAll(notifications);
            page = notifications.nextPageable();
        } while (notifications.hasNext());
    }

    @Override
    public void removeRoleReceiver(String role) {
        Pageable page = PageRequest.of(0, 500);
        Page<Notification> notifications;
        do {
            notifications = notificationRepository.findAllByRoleRecipientsContaining(role, page);
            notifications.forEach(notification -> notification.getRoleRecipients().remove(role));
            notificationRepository.saveAll(notifications);
            page = notifications.nextPageable();
        } while (notifications.hasNext());
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (notificationMode == NotificationMode.MULTITENANT) {
            subscriber.subscribeTo(ProjectUserEvent.class, new ProjectUserEventListener());
            subscriber.subscribeTo(RoleEvent.class, new RoleEventListener());
        }
    }

    @Override
    public Page<Notification> retrieveNotifications(Pageable page, NotificationStatus state)
            throws EntityNotFoundException {
        if (state != null) {
            if (notificationMode == NotificationMode.MULTITENANT) {
                return notificationRepository.findByStatusAndRecipientsContaining(state,
                                                                                  authenticationResolver.getUser(),
                                                                                  authenticationResolver.getRole(),
                                                                                  page);
            } else {
                return notificationRepository.findByStatus(state, page);
            }
        } else {
            return retrieveNotifications(page);
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
