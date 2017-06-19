/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.registration;

import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;

/**
 * Interface defining the service providing registration features.
 *
 * @author Xavier-Alexandre Brochard
 */
public interface IRegistrationService { //NOSONAR

    /**
     * Creates a new account if needed and creates a new project user.
     *
     * @param pDto
     *            The DTO containing all information to create the new {@link Account} and {@link ProjectUser}
     * @param pValidationUrl
     *            The validation url for the account confirmation email
     * @throws EntityException
     *             <br>
     *             {@link EntityAlreadyExistsException} Thrown when an account with same <code>email</code> already
     *             exists<br>
     *             {@link EntityTransitionForbiddenException} Thrown when the account is not in status PENDING<br>
     * @return void
     */
    void requestAccess(final AccessRequestDto pDto) throws EntityException;

}
