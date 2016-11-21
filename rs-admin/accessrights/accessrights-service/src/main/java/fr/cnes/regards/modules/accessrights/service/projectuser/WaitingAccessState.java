/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.projectuser;

import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;

/**
 * State class of the State Pattern implementing the available actions on a {@link ProjectUser} in status
 * WAITING_ACCESS.
 *
 * @author Xavier-Alexandre Brochard
 * @since 1.1-SNAPSHOT
 */
@Component
public class WaitingAccessState extends AbstractDeletableState {

    /**
     * Project User Settings Repository. Autowired by Spring.
     */
    private final IAccessSettingsService accessSettingsService;

    /**
     * Creates a new PENDING state
     *
     * @param pProjectUserRepository
     *            the project user repository
     * @param pAccessSettingsService
     *            the project user settings repository
     */
    public WaitingAccessState(final IProjectUserRepository pProjectUserRepository,
            final IAccessSettingsService pAccessSettingsService) {
        super(pProjectUserRepository);
        accessSettingsService = pAccessSettingsService;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserTransitions#qualifyAccess(fr.cnes.regards.
     * modules.accessrights.domain.projects.ProjectUser)
     */
    @Override
    public void qualifyAccess(final ProjectUser pProjectUser, final AccessQualification pQualification) {
        final AccessSettings settings = accessSettingsService.retrieve();

        if ("auto-accept".equals(settings.getMode())) {
            pProjectUser.setStatus(UserStatus.ACCESS_GRANTED);
            getProjectUserRepository().save(pProjectUser);
        } else {
            switch (pQualification) {
                case GRANTED:
                    pProjectUser.setStatus(UserStatus.ACCESS_GRANTED);
                    getProjectUserRepository().save(pProjectUser);
                    break;
                case DENIED:
                    pProjectUser.setStatus(UserStatus.ACCESS_DENIED);
                    getProjectUserRepository().save(pProjectUser);
                    break;
                case REJECTED:
                    doDelete(pProjectUser);
                    break;
                default:
                    pProjectUser.setStatus(UserStatus.ACCESS_DENIED);
                    getProjectUserRepository().save(pProjectUser);
                    break;
            }
        }
    }

}
