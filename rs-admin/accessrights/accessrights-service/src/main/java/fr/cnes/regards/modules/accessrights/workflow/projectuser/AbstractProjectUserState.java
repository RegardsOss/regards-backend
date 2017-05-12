/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.workflow.projectuser;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;

/**
 * Default implementation is to throw an exception stating that the called action is not allowed for the account's
 * current status.
 * @author Xavier-Alexandre Brochard
 * @since  1.1-SNAPSHOT
 */
public abstract class AbstractProjectUserState implements IProjectUserTransitions {

    @Override
    public void makeProjectUserWaitForQualification(final ProjectUser pProjectUser)
            throws EntityTransitionForbiddenException {
        throwTransitionForbiddenException(pProjectUser);
    }

    @Override
    public void qualifyAccess(final ProjectUser pProjectUser, final AccessQualification pQualification)
            throws EntityException {
        throwTransitionForbiddenException(pProjectUser);
    }

    @Override
    public void inactiveAccess(final ProjectUser pProjectUser) throws EntityTransitionForbiddenException {
        throwTransitionForbiddenException(pProjectUser);
    }

    @Override
    public void activeAccess(final ProjectUser pProjectUser) throws EntityTransitionForbiddenException {
        throwTransitionForbiddenException(pProjectUser);
    }

    @Override
    public void denyAccess(final ProjectUser pProjectUser) throws EntityTransitionForbiddenException {
        throwTransitionForbiddenException(pProjectUser);
    }

    @Override
    public void grantAccess(final ProjectUser pProjectUser) throws EntityTransitionForbiddenException {
        throwTransitionForbiddenException(pProjectUser);
    }

    @Override
    public void removeAccess(final ProjectUser pProjectUser) throws EntityTransitionForbiddenException {
        throwTransitionForbiddenException(pProjectUser);
    }

    /**
     * Default common implementation of a transition
     * @param pProjectUser the project user
     * @throws EntityTransitionForbiddenException always
     */
    private void throwTransitionForbiddenException(final ProjectUser pProjectUser)
            throws EntityTransitionForbiddenException {
        throw new EntityTransitionForbiddenException(pProjectUser.getId().toString(), ProjectUser.class,
                pProjectUser.getStatus().toString(), Thread.currentThread().getStackTrace()[2].getMethodName());
    }
}
