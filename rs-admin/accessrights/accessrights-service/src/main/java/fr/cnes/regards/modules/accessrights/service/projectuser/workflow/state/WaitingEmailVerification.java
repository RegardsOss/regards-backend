/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.projectuser.workflow.state;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.emailverification.EmailVerificationToken;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;

/**
 * State class of the State Pattern implementing the available actions on a {@link ProjectUser} in status
 * WAITING_ACCESS.
 *
 * @author Xavier-Alexandre Brochard
 * @since 1.1-SNAPSHOT
 */
@Component
public class WaitingEmailVerification extends AbstractDeletableState {

    /**
     * Creates a new PENDING state
     *
     * @param pProjectUserRepository
     *            the project user repository
     * @param pAccessSettingsService
     *            the project user settings repository
     */
    public WaitingEmailVerification(final IProjectUserRepository pProjectUserRepository) {
        super(pProjectUserRepository);
    }

    /* (non-Javadoc)
     * @see fr.cnes.regards.modules.accessrights.service.projectuser.workflow.state.AbstractProjectUserState#verifyEmail(fr.cnes.regards.modules.accessrights.domain.emailverification.EmailVerificationToken)
     */
    @Override
    public void verifyEmail(EmailVerificationToken pEmailVerificationToken) throws EntityException {
        final ProjectUser projectUser = pEmailVerificationToken.getProjectUser();

        if (pEmailVerificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new EntityOperationForbiddenException(projectUser.getEmail(), ProjectUser.class,
                    "Verification token has expired");
        }

        projectUser.setStatus(UserStatus.ACCESS_GRANTED);
        getProjectUserRepository().save(projectUser);

    }

}
