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

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.notification.NotificationDTO;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.notification.dao.INotificationRepository;
import fr.cnes.regards.modules.notification.domain.INotificationWithoutMessage;
import fr.cnes.regards.modules.notification.domain.Notification;
import fr.cnes.regards.modules.notification.domain.NotificationStatus;
import fr.cnes.regards.modules.notification.domain.NotificationToSendEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@link IInstanceNotificationService} implementation
 * @author Xavier-Alexandre Brochard
 * @author Sébastien Binda
 * @author Sylvain Vissiere-Guerinet
 */
@Service
@RegardsTransactional
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class InstanceNotificationService implements IInstanceNotificationService {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceNotificationService.class);

    /**
     * CRUD repository managing notifications. Autowired by Spring.
     */
    private INotificationRepository notificationRepository;

    /**
     * Application event publisher
     */
    private ApplicationEventPublisher applicationEventPublisher;

    private IAuthenticationResolver authenticationResolver;

    private IInstanceNotificationService self;

    public InstanceNotificationService(INotificationRepository notificationRepository, ApplicationEventPublisher applicationEventPublisher,
                                       IAuthenticationResolver authenticationResolver, IInstanceNotificationService instanceNotificationService) {
        this.notificationRepository = notificationRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.authenticationResolver = authenticationResolver;
        this.self = instanceNotificationService;
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

        notification.setRoleRecipients(Sets.newHashSet(DefaultRole.INSTANCE_ADMIN.toString()));

        // check the notification type and send it immediately if FATAL or ERROR
        if ((notification.getLevel() == NotificationLevel.FATAL)
                || (notification.getLevel() == NotificationLevel.ERROR)) {
            applicationEventPublisher.publishEvent(new NotificationToSendEvent(notification));
        }

        // Save it in db
        notification = notificationRepository.save(notification);

        return notification;
    }

    @Override
    public Page<INotificationWithoutMessage> retrieveNotifications(Pageable page) {
        return notificationRepository.findAllNotificationsWithoutMessage(page);
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
        Set<Long> idsToDelete = results.getContent().stream().map(INotificationWithoutMessage::getId)
                .collect(Collectors.toSet());
        notificationRepository.deleteByIdIn(idsToDelete);
        return results;
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
    public Page<INotificationWithoutMessage> retrieveNotifications(Pageable page, NotificationStatus state) {
        if (state != null) {
            return notificationRepository.findAllNotificationsWithoutMessageByStatus(state, page);
        } else {
            return retrieveNotifications(page);
        }
    }
}
