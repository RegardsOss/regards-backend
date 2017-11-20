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
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.client.IRolesClient;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;
import fr.cnes.regards.modules.notification.dao.INotificationRepository;
import fr.cnes.regards.modules.notification.domain.Notification;
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
@EnableFeignClients(clients = { IRolesClient.class })
@MultitenantTransactional
public class NotificationService implements INotificationService {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(NotificationService.class);

    /**
     * CRUD repository managing notifications. Autowired by Spring.
     */
    private final INotificationRepository notificationRepository;

    /**
     * Service handle CRUD operations on {@link ProjectUser}s. Autowired by Spring.
     */
    private final IProjectUserService projectUserService;

    /**
     * CRUD repository managing project users. Autowired by Spring.
     */
    private final IProjectUserRepository projectUserRepository;

    /**
     * CRUD repository managing roles. Autowired by Spring.
     */
    private final IRoleRepository roleRepository;

    /**
     * Feign client for {@link Role}s. Autowired by Spring.
     */
    private final IProjectUsersClient projectUserClient;

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Creates a {@link NotificationService} wired to the given {@link INotificationRepository}.
     *
     * @param projectUserService
     *            Autowired by Spring. Must not be {@literal null}.
     * @param notificationRepository
     *            Autowired by Spring. Must not be {@literal null}.
     * @param projectUserRepository
     *            Autowired by Spring. Must not be {@literal null}.
     * @param roleRepository
     *            Autowired by Spring. Must not be {@literal null}.
     * @param projectUserClient
     *            Autowired by Spring. Must not be {@literal null}.
     */
    public NotificationService(final IProjectUserService projectUserService,
            final INotificationRepository notificationRepository, final IProjectUserRepository projectUserRepository,
            final IRoleRepository roleRepository, final IProjectUsersClient projectUserClient, final ApplicationEventPublisher applicationEventPublisher) {
        super();
        this.projectUserService = projectUserService;
        this.notificationRepository = notificationRepository;
        this.projectUserRepository = projectUserRepository;
        this.roleRepository = roleRepository;
        this.projectUserClient = projectUserClient;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.notification.service.INotificationService#retrieveNotifications()
     */
    @Override
    public List<Notification> retrieveNotifications() throws EntityNotFoundException {
        final ProjectUser projectUser = projectUserService.retrieveCurrentUser();
        final Role role = projectUser.getRole();
        return notificationRepository.findByRecipientsContaining(projectUser, role);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.notification.service.INotificationService#sendNotification(fr.cnes.regards.modules.
     * notification.domain.NotificationDTO)
     */
    @Override
    public Notification createNotification(final NotificationDTO pDto) {
        Notification notification = new Notification();
        notification.setDate(OffsetDateTime.now());
        notification.setMessage(pDto.getMessage());
        notification.setTitle(pDto.getTitle());
        notification.setSender(pDto.getSender());
        notification.setStatus(NotificationStatus.UNREAD);
        notification.setType(pDto.getType());

        List<ProjectUser> projectUserRecipients = projectUserRepository.findByEmailIn(pDto.getProjectUserRecipients());
        notification.setProjectUserRecipients(projectUserRecipients);

        List<Role> roleRecipients = roleRepository.findByNameIn(pDto.getRoleRecipients());
        notification.setRoleRecipients(roleRecipients);

        // check the notification type and send it immediately if FATAL or ERROR
        if (notification.getType() == NotificationType.FATAL || notification.getType() == NotificationType.ERROR) {
            applicationEventPublisher.publishEvent(new NotificationToSendEvent(notification));
        }

        // Save it in db
        notification = notificationRepository.save(notification);


        // TODO Trigger NOTIFICATION event on message broker

        return notification;
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.notification.service.INotificationService#retrieveNotification(java.lang.Long)
     */
    @Override
    public Notification retrieveNotification(final Long pId) throws EntityNotFoundException {
        if (!notificationRepository.exists(pId)) {
            throw new EntityNotFoundException(pId.toString(), Notification.class);
        }
        return notificationRepository.findOne(pId);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.notification.service.INotificationService#updateNotificationStatus(java.lang.Long)
     */
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

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.notification.service.INotificationService#deleteNotification(java.lang.Long)
     */
    @Override
    public void deleteNotification(final Long pId) throws EntityNotFoundException {
        if (!notificationRepository.exists(pId)) {
            throw new EntityNotFoundException(pId.toString(), Notification.class);
        }
        notificationRepository.delete(pId);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.notification.service.INotificationService#retrieveNotificationsToSend()
     */
    @Override
    public List<Notification> retrieveNotificationsToSend() {
        return notificationRepository.findByStatus(NotificationStatus.UNREAD);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.notification.service.INotificationService#assembleRecipients(fr.cnes.regards.modules.
     * notification.domain.Notification)
     */
    @Override
    public Stream<ProjectUser> findRecipients(final Notification pNotification) {
        // With the stream of role recipients and project users recipients
        try (final Stream<Role> rolesStream = pNotification.getRoleRecipients().stream();
                final Stream<ProjectUser> usersStream = pNotification.getProjectUserRecipients().stream()) {

            // Merge the two streams
            return Stream.concat(usersStream,
                                 rolesStream.flatMap(// Define a function mapping each role to its project users by
                                                     // calling the roles client
                                                     r -> HateoasUtils.retrieveAllPages(100,
                                                                                        pageable -> retrieveRoleProjectUserList(
                                                                                                r,
                                                                                                pageable)).stream())
                                         .distinct());
        }
    }

    public ResponseEntity<PagedResources<Resource<ProjectUser>>> retrieveRoleProjectUserList(Role pRole,
            Pageable pPageable) {
        final ResponseEntity<PagedResources<Resource<ProjectUser>>> response = projectUserClient
                .retrieveRoleProjectUserList(pRole.getId(), pPageable.getPageNumber(), pPageable.getPageSize());

        if (!response.getStatusCode().equals(HttpStatus.OK) || (response.getBody() == null)) {
            LOG.warn("Error retrieving projet users for role {}. Remote administration response is {}",
                     pRole.getName(),
                     response.getStatusCode());
        }
        return response;
    }

}
