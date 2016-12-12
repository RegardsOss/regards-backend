/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.notification.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.client.IRolesClient;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;
import fr.cnes.regards.modules.notification.dao.INotificationRepository;
import fr.cnes.regards.modules.notification.domain.Notification;
import fr.cnes.regards.modules.notification.domain.NotificationStatus;
import fr.cnes.regards.modules.notification.domain.dto.NotificationDTO;

/**
 * {@link INotificationService} implementation
 *
 * @author Xavier-Alexandre Brochard
 * @author Sébastien Binda
 *
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
    private final IRolesClient rolesClient;

    /**
     * Creates a {@link NotificationService} wired to the given {@link INotificationRepository}.
     *
     * @param pProjectUserService
     *            Autowired by Spring. Must not be {@literal null}.
     * @param pNotificationRepository
     *            Autowired by Spring. Must not be {@literal null}.
     * @param pProjectUserRepository
     *            Autowired by Spring. Must not be {@literal null}.
     * @param pRoleRepository
     *            Autowired by Spring. Must not be {@literal null}.
     * @param pRolesClient
     *            Autowired by Spring. Must not be {@literal null}.
     */
    public NotificationService(final IProjectUserService pProjectUserService,
            final INotificationRepository pNotificationRepository, final IProjectUserRepository pProjectUserRepository,
            final IRoleRepository pRoleRepository, final IRolesClient pRolesClient) {
        super();
        projectUserService = pProjectUserService;
        notificationRepository = pNotificationRepository;
        projectUserRepository = pProjectUserRepository;
        roleRepository = pRoleRepository;
        rolesClient = pRolesClient;
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
        final Notification notification = new Notification();
        notification.setDate(LocalDateTime.now());
        notification.setMessage(pDto.getMessage());
        notification.setTitle(pDto.getTitle());
        notification.setSender(pDto.getSender());
        notification.setStatus(NotificationStatus.UNREAD);

        final List<ProjectUser> projectUserRecipients = projectUserRepository
                .findByEmailIn(pDto.getProjectUserRecipients());
        notification.setProjectUserRecipients(projectUserRecipients);

        final List<Role> roleRecipients = roleRepository.findByNameIn(pDto.getRoleRecipients());
        notification.setRoleRecipients(roleRecipients);

        // Save it in db
        notificationRepository.save(notification);

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

            // Define a function mapping each role to its project users by calling the roles client
            final Function<Role, Stream<Resource<ProjectUser>>> toProjectUsers = r -> {
                List<Resource<ProjectUser>> result = new ArrayList<>();
                final ResponseEntity<List<Resource<ProjectUser>>> response = rolesClient
                        .retrieveRoleProjectUserList(r.getId());

                if (response.getStatusCode().equals(HttpStatus.OK) && (response.getBody() != null)) {
                    result = response.getBody();
                } else {
                    LOG.warn("Error retrieving projet users for role {}. Remote administration response is {}",
                             r.getName(), response.getStatusCode());
                }

                return result.stream();
            };

            // Merge the two streams
            return Stream.concat(usersStream, rolesStream.flatMap(toProjectUsers).map(HateoasUtils::unwrap).distinct());
        }
    }

}
