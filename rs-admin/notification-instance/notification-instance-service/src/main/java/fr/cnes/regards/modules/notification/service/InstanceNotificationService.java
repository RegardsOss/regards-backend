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

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.notification.NotificationDTO;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.notification.dao.INotificationRepository;
import fr.cnes.regards.modules.notification.dao.NotificationLightCustomNativeQueryRepository;
import fr.cnes.regards.modules.notification.dao.NotificationReadAuthorizer;
import fr.cnes.regards.modules.notification.domain.Notification;
import fr.cnes.regards.modules.notification.domain.NotificationLight;
import fr.cnes.regards.modules.notification.domain.NotificationStatus;
import fr.cnes.regards.modules.notification.domain.NotificationToSendEvent;
import fr.cnes.regards.modules.notification.domain.dto.SearchNotificationParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * {@link IInstanceNotificationService} implementation
 *
 * @author Xavier-Alexandre Brochard
 * @author Sébastien Binda
 * @author Sylvain Vissiere-Guerinet
 */
@Service
public class InstanceNotificationService implements IInstanceNotificationService {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceNotificationService.class);

    /**
     * CRUD repository managing notifications. Autowired by Spring.
     */
    private final INotificationRepository notificationRepository;

    /**
     * Application event publisher
     */
    private final ApplicationEventPublisher applicationEventPublisher;

    private final NotificationReadAuthorizer notificationReadAuthorizer;

    private final NotificationLightCustomNativeQueryRepository notificationLightCustomNativeQueryRepository;

    public InstanceNotificationService(INotificationRepository notificationRepository,
                                       ApplicationEventPublisher applicationEventPublisher,
                                       NotificationReadAuthorizer notificationReadAuthorizer,
                                       NotificationLightCustomNativeQueryRepository notificationLightCustomNativeQueryRepository) {
        this.notificationRepository = notificationRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.notificationReadAuthorizer = notificationReadAuthorizer;
        this.notificationLightCustomNativeQueryRepository = notificationLightCustomNativeQueryRepository;
    }

    @Override
    @RegardsTransactional
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
        if ((notification.getLevel() == NotificationLevel.FATAL) || (notification.getLevel()
                                                                     == NotificationLevel.ERROR)) {
            applicationEventPublisher.publishEvent(new NotificationToSendEvent(notification));
        }

        // Save it in db
        notification = notificationRepository.save(notification);

        return notification;
    }

    @Override
    @RegardsTransactional
    public Notification retrieveNotification(Long pId) throws ModuleException {
        notificationReadAuthorizer.checkAuthorization();
        Optional<Notification> notifOpt = notificationRepository.findById(pId);
        if (!notifOpt.isPresent()) {
            throw new EntityNotFoundException(pId.toString(), Notification.class);
        }
        return notifOpt.get();
    }

    @Override
    @RegardsTransactional
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
    @RegardsTransactional
    public void deleteNotification(Long pId) throws EntityNotFoundException {
        if (!notificationRepository.existsById(pId)) {
            throw new EntityNotFoundException(pId.toString(), Notification.class);
        }
        notificationRepository.deleteById(pId);
    }

    @Override
    public Page<NotificationLight> findAllOrderByDateDesc(SearchNotificationParameters filters, int page, int pageSize)
        throws ModuleException {
        notificationReadAuthorizer.checkAuthorization();
        return notificationLightCustomNativeQueryRepository.findAll(filters, null, null, page, pageSize);
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
        notificationLightCustomNativeQueryRepository.deleteAll(filters, null, null);
    }
}
