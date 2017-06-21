/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.projectuser.workflow.state;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.service.projectuser.emailverification.IEmailVerificationTokenService;
import fr.cnes.regards.modules.accessrights.service.projectuser.workflow.events.OnActiveEvent;

/**
 * State class of the State Pattern implementing the available actions on a {@link ProjectUser} in status ACCESS_INACTIVE.
 *
 * @author Xavier-Alexandre Brochard
 */
@Component
public class AccessInactiveState extends AbstractDeletableState {

    /**
     * Use this to publish Spring application events
     */
    private final ApplicationEventPublisher eventPublisher;

    /**
     * @param pProjectUserRepository
     * @param pEmailVerificationTokenService
     * @param pPublisher
     * @param pEventPublisher
     */
    public AccessInactiveState(IProjectUserRepository pProjectUserRepository,
            IEmailVerificationTokenService pEmailVerificationTokenService, IPublisher pPublisher,
            ApplicationEventPublisher pEventPublisher) {
        super(pProjectUserRepository, pEmailVerificationTokenService, pPublisher);
        eventPublisher = pEventPublisher;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserTransitions#activeAccess(fr.cnes.regards.
     * modules.accessrights.domain.projects.ProjectUser)
     */
    @Override
    public void activeAccess(final ProjectUser pProjectUser) throws EntityTransitionForbiddenException {
        pProjectUser.setStatus(UserStatus.ACCESS_GRANTED);
        getProjectUserRepository().save(pProjectUser);
        eventPublisher.publishEvent(new OnActiveEvent(pProjectUser));
    }

}
