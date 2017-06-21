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
import fr.cnes.regards.modules.accessrights.service.projectuser.workflow.events.OnGrantAccessEvent;

/**
 * State class of the State Pattern implementing the available actions on a {@link ProjectUser} in status ACCESS_DENIED.
 *
 * @author Xavier-Alexandre Brochard
 * @since 1.1-SNAPSHOT
 */
@Component
public class AccessDeniedState extends AbstractDeletableState {

    /**
     * Use this to publish Spring application events
     */
    private final ApplicationEventPublisher eventPublisher;

    /**
     * @param pProjectUserRepository
     * @param pEmailVerificationTokenService
     * @param pEventPublisher
     * @param publisher
     */
    public AccessDeniedState(IProjectUserRepository pProjectUserRepository,
            IEmailVerificationTokenService pEmailVerificationTokenService, ApplicationEventPublisher pEventPublisher,
            IPublisher publisher) {
        super(pProjectUserRepository, pEmailVerificationTokenService, publisher);
        eventPublisher = pEventPublisher;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserTransitions#grantAccess(fr.cnes.regards.
     * modules.accessrights.domain.projects.ProjectUser)
     */
    @Override
    public void grantAccess(final ProjectUser pProjectUser) throws EntityTransitionForbiddenException {
        pProjectUser.setStatus(UserStatus.WAITING_EMAIL_VERIFICATION);
        getProjectUserRepository().save(pProjectUser);
        eventPublisher.publishEvent(new OnGrantAccessEvent(pProjectUser));
    }

}
