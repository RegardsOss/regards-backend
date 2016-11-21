/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.projectuser;

import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.modules.accessrights.domain.AccessRequestDTO;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;

/**
 * State pattern implementation defining the actions managing the state of a project user.<br>
 * Default implementation is to throw an exception stating that the called action is not allowed for the account's
 * current status.
 *
 * @author Xavier-Alexandre Brochard
 * @since 1.1-SNAPSHOT
 */
public interface IProjectUserTransitions {

    /**
     * Make a new request access by creating a project user in status WAITING_ACCESS
     *
     * @param pAccessRequestDTO
     *            The DTO containing all information to create the new {@link ProjectUser}
     * @throws EntityException
     *             <br>
     *             {@link EntityTransitionForbiddenException} If the account has a not null status<br>
     *             {@link EntityNotFoundException} If no role with passed name could be found<br>
     *             {@link EntityAlreadyExistsException} If a project user with passed email already exists<br>
     */
    default void requestProjectAccess(final AccessRequestDTO pAccessRequestDTO) throws EntityException {
        throw new EntityTransitionForbiddenException(pAccessRequestDTO.getEmail(), ProjectUser.class, null,
                Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    /**
     * Passes the project user from status WAITING_ACCESS to ACCESS_GRANTED or ACCESS_DENIED according to the project
     * user's acceptance policy.
     *
     * @param pProjectUser
     *            the project user
     * @param pQualification
     *            what decision is taken on this project user
     * @throws EntityTransitionForbiddenException
     *             when the project user is not in status WAITING_ACCESS
     */
    default void qualifyAccess(final ProjectUser pProjectUser, final AccessQualification pQualification)
            throws EntityTransitionForbiddenException {
        throw new EntityTransitionForbiddenException(pProjectUser.getId().toString(), ProjectUser.class,
                pProjectUser.getStatus().toString(), Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    /**
     * Passes an ACCESS_GRANTED project user to the status ACCESS_INACTIVE.
     *
     * @param pProjectUser
     *            the project user
     * @throws EntityTransitionForbiddenException
     *             when the project user is not in status ACCESS_GRANTED
     */
    default void inactiveAccess(final ProjectUser pProjectUser) throws EntityTransitionForbiddenException {
        throw new EntityTransitionForbiddenException(pProjectUser.getId().toString(), ProjectUser.class,
                pProjectUser.getStatus().toString(), Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    /**
     * Passes an ACCESS_INACTIVE project user to the status ACCESS_GRANTED.
     *
     * @param pProjectUser
     *            the project user
     * @throws EntityTransitionForbiddenException
     *             when the project user is not in status ACCESS_INACTIVE
     */
    default void activeAccess(final ProjectUser pProjectUser) throws EntityTransitionForbiddenException {
        throw new EntityTransitionForbiddenException(pProjectUser.getId().toString(), ProjectUser.class,
                pProjectUser.getStatus().toString(), Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    /**
     * Passes an ACCESS_GRANTED project user to the status ACCESS_DENIED.
     *
     * @param pProjectUser
     *            the project user
     * @throws EntityTransitionForbiddenException
     *             when the project user is not in status ACCESS_GRANTED
     */
    default void denyAccess(final ProjectUser pProjectUser) throws EntityTransitionForbiddenException {
        throw new EntityTransitionForbiddenException(pProjectUser.getId().toString(), ProjectUser.class,
                pProjectUser.getStatus().toString(), Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    /**
     * Passes an ACCESS_DENIED project user to the status ACCESS_GRANTED.
     *
     * @param pProjectUser
     *            the project user
     * @throws EntityTransitionForbiddenException
     *             when the project user is not in status ACCESS_DENIED
     */
    default void grantAccess(final ProjectUser pProjectUser) throws EntityTransitionForbiddenException {
        throw new EntityTransitionForbiddenException(pProjectUser.getId().toString(), ProjectUser.class,
                pProjectUser.getStatus().toString(), Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    /**
     * Passes a WAITING_ACCESS/ACCESS_GRANTED/ACCESS_INACTIVE/ACCESS_DENIED project user to the status NO_ACCESS and
     * deletes it.
     *
     * @param pProjectUser
     *            the project user
     * @throws EntityTransitionForbiddenException
     *             when the project user is not in status WAITING_ACCESS/ACCESS_GRANTED/ACCESS_INACTIVE/ACCESS_DENIED
     */
    default void removeAccess(final ProjectUser pProjectUser) throws EntityTransitionForbiddenException {
        throw new EntityTransitionForbiddenException(pProjectUser.getId().toString(), ProjectUser.class,
                pProjectUser.getStatus().toString(), Thread.currentThread().getStackTrace()[1].getMethodName());
    }

}
