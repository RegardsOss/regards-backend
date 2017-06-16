/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.projectuser.workflow.state;

import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;

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
     * Constructor
     *
     * @param pProjectUserRepository
     *            the project user repository
     */
    public AbstractDeletableState(final IProjectUserRepository pProjectUserRepository) {
        super();
        projectUserRepository = pProjectUserRepository;
    }

    @Override
    public void removeAccess(final ProjectUser pProjectUser) throws EntityTransitionForbiddenException {
        switch (pProjectUser.getStatus()) {
            case WAITING_ACCOUNT_ACTIVE:
            case WAITING_ACCESS:
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
        projectUserRepository.delete(pProjectUser.getId());
    }

    /**
     * @return the projectUserRepository
     */
    protected IProjectUserRepository getProjectUserRepository() {
        return projectUserRepository;
    }

}
