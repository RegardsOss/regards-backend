/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.projectuser.workflow.state;

import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.service.projectuser.emailverification.IEmailVerificationTokenService;

/**
 * State class of the State Pattern implementing the available actions on a {@link ProjectUser} in status
 * ACCESS_GRANTED.
 *
 * @author Xavier-Alexandre Brochard
 * @since 1.1-SNAPSHOT
 */
@Component
public class AccessGrantedState extends AbstractDeletableState {

    /**
     * @param pProjectUserRepository
     * @param pEmailVerificationTokenService
     */
    public AccessGrantedState(IProjectUserRepository pProjectUserRepository,
            IEmailVerificationTokenService pEmailVerificationTokenService) {
        super(pProjectUserRepository, pEmailVerificationTokenService);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserTransitions#inactiveAccess(fr.cnes.regards.
     * modules.accessrights.domain.projects.ProjectUser)
     */
    @Override
    public void inactiveAccess(final ProjectUser pProjectUser) throws EntityTransitionForbiddenException {
        pProjectUser.setStatus(UserStatus.ACCESS_INACTIVE);
        getProjectUserRepository().save(pProjectUser);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserTransitions#denyAccess(fr.cnes.regards.
     * modules.accessrights.domain.projects.ProjectUser)
     */
    @Override
    public void denyAccess(final ProjectUser pProjectUser) throws EntityTransitionForbiddenException {
        pProjectUser.setStatus(UserStatus.ACCESS_DENIED);
        getProjectUserRepository().save(pProjectUser);
    }

}
