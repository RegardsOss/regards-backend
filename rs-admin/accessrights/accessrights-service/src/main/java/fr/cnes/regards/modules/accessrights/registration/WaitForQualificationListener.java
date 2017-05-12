/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.registration;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.service.projectuser.IAccessSettingsService;
import fr.cnes.regards.modules.accessrights.workflow.account.event.OnAccountEmailValidationEvent;
import fr.cnes.regards.modules.accessrights.workflow.projectuser.AccessQualification;
import fr.cnes.regards.modules.accessrights.workflow.projectuser.ProjectUserWorkflowManager;

/**
 * Listen to {@link OnAccountEmailValidationEvent} in order to pass a {@link ProjectUser} from WAITING_ACCOUNT_ACTIVATION to WAITING_ACCESS.
 *
 * @author Xavier-Alexandre Brochard
 */
@Component
public class WaitForQualificationListener implements ApplicationListener<OnAccountEmailValidationEvent> {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(WaitForQualificationListener.class);

    /**
     * CRUD repository handling {@link ProjectUser}s. Autowired by Spring.
     */
    private final IProjectUserRepository projectUserRepository;

    /**
     * Account workflow manager
     */
    private final ProjectUserWorkflowManager projectUserWorkflowManager;

    /**
     * CRUD repository handling {@link AccountSettingst}s. Autowired by Spring.
     */
    private final IAccessSettingsService accessSettingsService;

    /**
     * @param pProjectUserRepository
     * @param pProjectUserWorkflowManager
     * @param pAccessSettingsService
     */
    public WaitForQualificationListener(IProjectUserRepository pProjectUserRepository,
            ProjectUserWorkflowManager pProjectUserWorkflowManager, IAccessSettingsService pAccessSettingsService) {
        super();
        projectUserRepository = pProjectUserRepository;
        projectUserWorkflowManager = pProjectUserWorkflowManager;
        accessSettingsService = pAccessSettingsService;
    }

    @Override
    public void onApplicationEvent(final OnAccountEmailValidationEvent pEvent) {
        try {
            makeProjectUserWaitForQualification(pEvent);
        } catch (EntityException e) {
            LOG.info("Could not change status of project user " + pEvent.getEmail() + " from "
                    + UserStatus.WAITING_ACCOUNT_ACTIVE + " to " + UserStatus.WAITING_ACCESS, e);
            throw new RegistrationRuntimeException();
        }
    }

    /**
     * Pass a {@link ProjectUser} from WAITING_ACCOUNT_ACTIVATION to WAITING_ACCESS
     * @param pEvent the event
     * @throws EntityException if not project user with given email (in event) could be found
     */
    private void makeProjectUserWaitForQualification(OnAccountEmailValidationEvent pEvent) throws EntityException {
        // Retrieve the account's/project user email
        String email = pEvent.getEmail();

        // Retrieve the project user
        Optional<ProjectUser> optional = projectUserRepository.findOneByEmail(pEvent.getEmail());
        ProjectUser projectUser = optional.orElseThrow(() -> new EntityNotFoundException(email, ProjectUser.class));

        // Change state
        projectUserWorkflowManager.makeProjectUserWaitForQualification(projectUser);

        // Auto-accept if configured so
        final AccessSettings settings = accessSettingsService.retrieve();
        if (AccessSettings.AUTO_ACCEPT_MODE.equals(settings.getMode())) {
            projectUserWorkflowManager.qualifyAccess(projectUser, AccessQualification.GRANTED);
        }

        // Save
        projectUserRepository.save(projectUser);
    }

}