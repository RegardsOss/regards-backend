/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.notification.NotificationDTO;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;
import fr.cnes.regards.modules.notification.dao.INotificationRepository;
import fr.cnes.regards.modules.notification.dao.NotificationLightCustomNativeQueryRepository;
import fr.cnes.regards.modules.notification.domain.*;
import fr.cnes.regards.modules.notification.domain.dto.SearchNotificationParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
public class NotificationService implements INotificationService {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(NotificationService.class);

    /**
     * CRUD repository managing notifications. Autowired by Spring.
     */
    private final INotificationRepository notificationRepository;

    private final NotificationLightCustomNativeQueryRepository notificationLightCustomNativeQueryRepository;

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

    /**
     * Creates a {@link NotificationService} wired to the given {@link INotificationRepository}.
     */
    public NotificationService(INotificationRepository notificationRepository,
                               IRoleService roleService,
                               ApplicationEventPublisher applicationEventPublisher,
                               IAuthenticationResolver authenticationResolver,
                               NotificationLightCustomNativeQueryRepository notificationLightCustomNativeQueryRepository,
                               @Value("${regards.notification.mode:MULTITENANT}") NotificationMode notificationMode) {
        super();
        this.notificationRepository = notificationRepository;
        this.roleService = roleService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.notificationMode = notificationMode;
        this.authenticationResolver = authenticationResolver;
        this.notificationLightCustomNativeQueryRepository = notificationLightCustomNativeQueryRepository;
    }

    @Override
    @MultitenantTransactional
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
    @MultitenantTransactional
    public Notification retrieveNotification(Long pId) throws EntityNotFoundException {
        Optional<Notification> notificationOpt = notificationRepository.findById(pId);
        if (notificationOpt.isEmpty()) {
            throw new EntityNotFoundException(pId.toString(), Notification.class);
        }
        return notificationOpt.get();
    }

    @Override
    @MultitenantTransactional
    public Notification updateNotificationStatus(Long pId, NotificationStatus pStatus) throws EntityNotFoundException {
        Optional<Notification> notifOpt = notificationRepository.findById(pId);
        if (notifOpt.isEmpty()) {
            throw new EntityNotFoundException(pId.toString(), Notification.class);
        }
        Notification notification = notifOpt.get();
        notification.setStatus(pStatus);
        return notificationRepository.save(notification);
    }

    @Override
    @MultitenantTransactional
    public void deleteNotification(Long pId) throws EntityNotFoundException {
        if (!notificationRepository.existsById(pId)) {
            throw new EntityNotFoundException(pId.toString(), Notification.class);
        }
        notificationRepository.deleteById(pId);
    }

    @Override
    @MultitenantTransactional(readOnly = true)
    public Page<Notification> retrieveNotificationsToSend(Pageable page) {
        return notificationRepository.findByStatus(NotificationStatus.UNREAD, page);
    }

    @Override
    @MultitenantTransactional
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

    /* (non-Javadoc)
     * @see fr.cnes.regards.modules.notification.service.INotificationService#countUnreadNotifications()
     */
    @Override
    @MultitenantTransactional
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
    @MultitenantTransactional
    public Long countReadNotifications() {
        if (notificationMode == NotificationMode.MULTITENANT) {
            return notificationRepository.countByStatus(NotificationStatus.READ.toString(),
                                                        authenticationResolver.getUser(),
                                                        authenticationResolver.getRole());
        } else {
            return notificationRepository.countByStatus(NotificationStatus.READ);
        }
    }

    /**
     * Retrieve a notification light page matching filter and ordered by date
     */
    @Override
    public Page<NotificationLight> findAllOrderByDateDesc(SearchNotificationParameters filters,
                                                          int page,
                                                          int pageSize) {
        return notificationLightCustomNativeQueryRepository.findAll(filters,
                                                                    authenticationResolver.getUser(),
                                                                    authenticationResolver.getRole(),
                                                                    page,
                                                                    pageSize);
    }

    /**
     * Delete notifications matching filters
     *
     * @param filters search parameters
     */
    @Override
    public void deleteNotifications(SearchNotificationParameters filters) {
        // First add a limit date for deletion with current date to avoid remove incoming notifications.
        if (filters.getDates() == null || filters.getDates().getBefore() == null) {
            filters.withDateBefore(OffsetDateTime.now());
        }
        notificationLightCustomNativeQueryRepository.deleteAll(filters,
                                                               authenticationResolver.getUser(),
                                                               authenticationResolver.getRole());
    }
}
