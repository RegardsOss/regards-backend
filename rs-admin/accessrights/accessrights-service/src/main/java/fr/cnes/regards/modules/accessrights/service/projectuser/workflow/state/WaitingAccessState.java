/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.projectuser.workflow.state;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.service.projectuser.emailverification.IEmailVerificationTokenService;
import fr.cnes.regards.modules.accessrights.service.projectuser.workflow.events.OnDenyEvent;
import fr.cnes.regards.modules.accessrights.service.projectuser.workflow.events.OnGrantAccessEvent;

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
     * Use this to publish Spring application events
     */
    private final ApplicationEventPublisher eventPublisher;

    /**
     * @param pProjectUserRepository
     * @param pEmailVerificationTokenService
     * @param pEventPublisher
     */
    public WaitingAccessState(IProjectUserRepository pProjectUserRepository,
            IEmailVerificationTokenService pEmailVerificationTokenService, ApplicationEventPublisher pEventPublisher, IPublisher publisher) {
        super(pProjectUserRepository, pEmailVerificationTokenService, publisher);
        eventPublisher = pEventPublisher;
    }

    /* (non-Javadoc)
     * @see fr.cnes.regards.modules.accessrights.service.projectuser.workflow.state.AbstractDeletableState#removeAccess(fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser)
     */
    @Override
    public void removeAccess(ProjectUser pProjectUser) throws EntityTransitionForbiddenException {
        doDelete(pProjectUser);
    }

    /* (non-Javadoc)
     * @see fr.cnes.regards.modules.accessrights.service.projectuser.workflow.state.AbstractProjectUserState#denyAccess(fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser)
     */
    @Override
    public void denyAccess(ProjectUser pProjectUser) throws EntityTransitionForbiddenException {
        pProjectUser.setStatus(UserStatus.ACCESS_DENIED);
        getProjectUserRepository().save(pProjectUser);
        eventPublisher.publishEvent(new OnDenyEvent(pProjectUser));
    }

    /* (non-Javadoc)
     * @see fr.cnes.regards.modules.accessrights.service.projectuser.workflow.state.AbstractProjectUserState#grantAccess(fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser)
     */
    @Override
    public void grantAccess(ProjectUser pProjectUser) throws EntityException {
        pProjectUser.setStatus(UserStatus.WAITING_EMAIL_VERIFICATION);
        getProjectUserRepository().save(pProjectUser);
        eventPublisher.publishEvent(new OnGrantAccessEvent(pProjectUser));
    }

}
