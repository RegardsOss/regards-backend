/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.notification.service;

import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;
import fr.cnes.regards.modules.notification.dao.INotificationRepository;
import fr.cnes.regards.modules.notification.dao.INotificationSettingsRepository;
import fr.cnes.regards.modules.notification.domain.NotificationSettings;
import fr.cnes.regards.modules.notification.domain.dto.NotificationSettingsDTO;

/**
 * {@link INotificationSettingsService} implementation.
 *
 * @author xbrochar
 * @author Sébastien Binda
 *
 */
@Service
@MultitenantTransactional
public class NotificationSettingsService implements INotificationSettingsService {

    /**
     * Service handling CRUD operations on project users. Autowired by Spring.
     */
    private final IProjectUserService projectUserService;

    /**
     * CRUD repository managing notification settings. Autowired by Spring.
     */
    private final INotificationSettingsRepository notificationSettingsRepository;

    /**
     * Creates a {@link NotificationSettingsService} wired to the given {@link INotificationRepository}.
     *
     * @param pProjectUserService
     *            Autowired by Spring. Must not be {@literal null}.
     * @param pNotificationSettingsRepository
     *            Autowired by Spring. Must not be {@literal null}.
     */
    public NotificationSettingsService(final IProjectUserService pProjectUserService,
            final INotificationSettingsRepository pNotificationSettingsRepository) {
        super();
        projectUserService = pProjectUserService;
        notificationSettingsRepository = pNotificationSettingsRepository;
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.notification.service.INotificationService#retrieveNotificationSettings()
     */
    @Override
    public NotificationSettings retrieveNotificationSettings() throws EntityNotFoundException {
        final ProjectUser projectUser = projectUserService.retrieveCurrentUser();
        NotificationSettings result = notificationSettingsRepository.findOneByProjectUser(projectUser);
        if (result == null) {
            result = createNotificationSettings(projectUser);
        }
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.notification.service.INotificationService#updateNotificationSettings(fr.cnes.regards.
     * modules.notification.domain.NotificationSettings)
     */
    @Override
    public void updateNotificationSettings(final NotificationSettingsDTO pDto) throws EntityNotFoundException {
        final NotificationSettings notificationSettings = retrieveNotificationSettings();

        if (pDto.getDays() != null) {
            notificationSettings.setDays(pDto.getDays());
        }
        if (pDto.getHours() != null) {
            notificationSettings.setHours(pDto.getHours());
        }
        if (pDto.getFrequency() != null) {
            notificationSettings.setFrequency(pDto.getFrequency());
        }

        notificationSettingsRepository.save(notificationSettings);
    }

    /**
     * Create notification settings for project user
     *
     * @param pProjectUser
     *            The target project user
     * @return The ceated notification settings
     */
    private NotificationSettings createNotificationSettings(final ProjectUser pProjectUser) {
        final NotificationSettings settings = new NotificationSettings();
        settings.setProjectUser(pProjectUser);
        return notificationSettingsRepository.save(settings);
    }

}
