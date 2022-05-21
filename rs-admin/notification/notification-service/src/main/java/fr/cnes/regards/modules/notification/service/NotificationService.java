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

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.notification.NotificationDTO;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;
import fr.cnes.regards.modules.notification.dao.INotificationRepository;
import fr.cnes.regards.modules.notification.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@link INotificationService} implementation
 *
 * @author Xavier-Alexandre Brochard
 * @author Sébastien Binda
 * @author Sylvain Vissiere-Guerinet
 */
@Service
@RegardsTransactional
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class NotificationService implements INotificationService {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(NotificationService.class);

    /**
     * CRUD repository managing notifications. Autowired by Spring.
     */
    private final INotificationRepository notificationRepository;

    private final IRoleService roleService;

    /**
     * Application event publisher
     */
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Notification mode:
     */
    private final NotificationMode notificationMode;

    private final IAuthenticationResolver authenticationResolver;

    private INotificationService self;

    /**
     * Creates a {@link NotificationService} wired to the given {@link INotificationRepository}.
     *
     * @param notificationRepository Autowired by Spring. Must not be {@literal null}.
     * @param roleService            Autowired by Spring. Must not be {@literal null}.
     * @param projectUserClient      Autowired by Spring. Must not be {@literal null}.
     */
    public NotificationService(INotificationRepository notificationRepository,
                               IRoleService roleService,
                               IProjectUserService projectUserClient,
                               ApplicationEventPublisher applicationEventPublisher,
                               IAuthenticationResolver authenticationResolver,
                               @Value("${regards.notification.mode:MULTITENANT}") NotificationMode notificationMode,
                               INotificationService notificationService) {
        super();
        this.notificationRepository = notificationRepository;
        this.roleService = roleService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.notificationMode = notificationMode;
        this.authenticationResolver = authenticationResolver;
        this.self = notificationService;
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

        // check the notification type and send it immediately if FATAL
        if ((notification.getLevel() == NotificationLevel.FATAL)) {
            applicationEventPublisher.publishEvent(new NotificationToSendEvent(notification));
        }

        // Save it in db
        notification = notificationRepository.save(notification);

        return notification;
    }

    @Override
    public Page<INotificationWithoutMessage> retrieveNotifications(Pageable page) {
        if (notificationMode == NotificationMode.MULTITENANT) {
            return notificationRepository.findByRecipientsContaining(authenticationResolver.getUser(),
                                                                     authenticationResolver.getRole(),
                                                                     page);
        } else {
            return notificationRepository.findAllNotificationsWithoutMessage(page);
        }
    }

    /**
     * Lets get all roles that should have the notification through the role hierarchy
     *
     * @return all recipient role names
     */
    private Set<String> getAllRecipientRoles(Set<String> roleRecipients) {
        return roleRecipients.stream()
                             .map(roleName -> {
                                 try {
                                     return roleService.retrieveRole(roleName);
                                 } catch (EntityNotFoundException e) {
                                     LOG.error(String.format(
                                         "Notification should have been sent to %s but that role does not exist anymore. Sending it to PROJECT_ADMIN",
                                         roleName), e);
                                     return new Role(DefaultRole.PROJECT_ADMIN.toString());
                                 }
                             })
                             .filter(Objects::nonNull)
                             .flatMap(role -> roleService.getDescendants(role).stream())
                             .map(Role::getName)
                             .collect(Collectors.toSet());
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
    public Set<String> findRecipients(Notification notification) {
        Set<String> roleUsers = new HashSet<>();
        for (String roleName : notification.getRoleRecipients()) {
            try {
                boolean hasNext;
                Pageable page = PageRequest.of(0, 100);
                do {
                    Page<ProjectUser> roleProjectUsers = roleService.retrieveRoleProjectUserList(roleName, page);
                    roleProjectUsers.getContent().forEach(pu -> roleUsers.add(pu.getEmail()));
                    hasNext = roleProjectUsers.getNumber() < (roleProjectUsers.getTotalPages() - 1);
                    page = page.next();
                } while (hasNext);
            } catch (EntityNotFoundException e) {
                LOG.error(String.format(
                    "Notification should have been sent to %s but that role does not exist anymore. Silently skipping part of the recipients",
                    roleName), e);
            }
        }
        // Merge the two streams
        roleUsers.addAll(notification.getProjectUserRecipients());
        return roleUsers;
    }

    @Override
    public Page<INotificationWithoutMessage> retrieveNotifications(Pageable page, NotificationStatus state) {
        if (state != null) {
            if (notificationMode == NotificationMode.MULTITENANT) {
                return notificationRepository.findByStatusAndRecipientsContaining(state,
                                                                                  authenticationResolver.getUser(),
                                                                                  authenticationResolver.getRole(),
                                                                                  page);
            } else {
                return notificationRepository.findAllNotificationsWithoutMessageByStatus(state, page);
            }
        } else {
            return retrieveNotifications(page);
        }
    }

    /* (non-Javadoc)
     * @see fr.cnes.regards.modules.notification.service.INotificationService#countUnreadNotifications()
     */
    @Override
    public Long countUnreadNotifications() {
        if (notificationMode == NotificationMode.MULTITENANT) {
            return notificationRepository.countByStatus(NotificationStatus.UNREAD.toString(),
                                                        authenticationResolver.getUser(),
                                                        authenticationResolver.getRole());
        } else {
            return notificationRepository.countByStatus(NotificationStatus.UNREAD);
        }
    }

    /* (non-Javadoc)
     * @see fr.cnes.regards.modules.notification.service.INotificationService#countReadNotifications()
     */
    @Override
    public Long countReadNotifications() {
        if (notificationMode == NotificationMode.MULTITENANT) {
            return notificationRepository.countByStatus(NotificationStatus.READ.toString(),
                                                        authenticationResolver.getUser(),
                                                        authenticationResolver.getRole());
        } else {
            return notificationRepository.countByStatus(NotificationStatus.READ);
        }
    }

    @Override
    public void deleteReadNotifications() {
        Pageable page = PageRequest.of(0, 100);
        Page<INotificationWithoutMessage> results;
        do {
            // Do delete in one unique transaction, to do so use the self element
            results = self.deleteReadNotificationsPage(page);
        } while (results.hasNext());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Page<INotificationWithoutMessage> deleteReadNotificationsPage(Pageable page) {
        Page<INotificationWithoutMessage> results = this.retrieveNotifications(page, NotificationStatus.READ);
        Set<Long> idsToDelete = results.getContent()
                                       .stream()
                                       .map(INotificationWithoutMessage::getId)
                                       .collect(Collectors.toSet());
        notificationRepository.deleteByIdIn(idsToDelete);
        return results;
    }
}
