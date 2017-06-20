/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.projectuser.workflow.state;

import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.service.projectuser.emailverification.IEmailVerificationTokenService;

/**
 * Abstract state implementation to implement the delete action on a project user.<br>
 * Various states share this common implementation.
 *
 * @author Xavier-Alexandre Brochard
 * @since 1.1-SNAPSHOT
 */
public abstract class AbstractDeletableState extends AbstractProjectUserState {

    /**
     * Repository managing {@link ProjectUser}s. Autowired by Spring.
     */
    private final IProjectUserRepository projectUserRepository;

    /**
     * Service to manage email verification tokens for project users.
     */
    private final IEmailVerificationTokenService emailVerificationTokenService;

    /**
     * @param pProjectUserRepository
     * @param pEmailVerificationTokenService
     */
    public AbstractDeletableState(IProjectUserRepository pProjectUserRepository,
            IEmailVerificationTokenService pEmailVerificationTokenService) {
        super();
        projectUserRepository = pProjectUserRepository;
        emailVerificationTokenService = pEmailVerificationTokenService;
    }

    @Override
    public void removeAccess(final ProjectUser pProjectUser) throws EntityTransitionForbiddenException {
        switch (pProjectUser.getStatus()) {
            case WAITING_ACCOUNT_ACTIVE:
            case WAITING_ACCESS:
            case WAITING_EMAIL_VERIFICATION:
            case ACCESS_DENIED:
            case ACCESS_GRANTED:
            case ACCESS_INACTIVE:
                doDelete(pProjectUser);
                break;
            default:
                throw new EntityTransitionForbiddenException(pProjectUser.getId().toString(), ProjectUser.class,
                        pProjectUser.getStatus().toString(), Thread.currentThread().getStackTrace()[1].getMethodName());
        }
    }

    /**
     * Delete a project user
     *
     * @param pProjectUser
     *            the project user
     */
    protected void doDelete(final ProjectUser pProjectUser) {
        emailVerificationTokenService.deleteTokenForProjectUser(pProjectUser);
        projectUserRepository.delete(pProjectUser.getId());
    }

    /**
     * @return the projectUserRepository
     */
    protected IProjectUserRepository getProjectUserRepository() {
        return projectUserRepository;
    }

    /**
     * @return the emailVerificationTokenService
     */
    public IEmailVerificationTokenService getEmailVerificationTokenService() {
        return emailVerificationTokenService;
    }

}
