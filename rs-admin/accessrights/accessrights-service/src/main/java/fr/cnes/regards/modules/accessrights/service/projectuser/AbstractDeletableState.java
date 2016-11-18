/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.projectuser;

import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.module.rest.exception.ModuleForbiddenTransitionException;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;

/**
 * Abstract state implementation to implement the delete action on a project user.<br>
 * Various states share this common implementation.
 *
 * @author Xavier-Alexandre Brochard
 * @since 1.1-SNAPSHOT
 */
abstract class AbstractDeletableState implements IProjectUserTransitions {

    /**
     * Repository managing {@link ProjectUser}s. Autowired by Spring.
     */
    @Autowired
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
    public void removeAccess(final ProjectUser pProjectUser) throws ModuleForbiddenTransitionException {
        switch (pProjectUser.getStatus()) {
            case WAITING_ACCESS:
            case ACCESS_DENIED:
            case ACCESS_GRANTED:
            case ACCESS_INACTIVE:
                doDelete(pProjectUser);
                break;
            default:
                throw new ModuleForbiddenTransitionException(pProjectUser.getId().toString(), ProjectUser.class,
                        pProjectUser.getStatus().toString(), Thread.currentThread().getStackTrace()[1].getMethodName());
        }
    }

    /**
     * Delete a project user
     *
     * @param pProjectUser
     *            the project user
     */
    private void doDelete(final ProjectUser pProjectUser) {
        projectUserRepository.delete(pProjectUser.getId());
    }

    /**
     * @return the projectUserRepository
     */
    protected IProjectUserRepository getProjectUserRepository() {
        return projectUserRepository;
    }

}
