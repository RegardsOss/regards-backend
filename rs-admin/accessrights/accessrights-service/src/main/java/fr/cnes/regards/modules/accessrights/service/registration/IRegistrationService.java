/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.accessrights.service.registration;

import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
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
     * @param dto              The DTO containing all information to create the new account and {@link ProjectUser}
     * @param isExternalAccess does the user access to creates associated to an external account ?
     * @return {@link ProjectUser} created
     * @throws EntityException <br>
     *                         {@link EntityAlreadyExistsException} Thrown when an account with same <code>email</code> already
     *                         exists<br>
     *                         {@link EntityTransitionForbiddenException} Thrown when the account is not in status PENDING<br>
     */
    ProjectUser requestAccess(final AccessRequestDto dto, boolean isExternalAccess) throws EntityException;

}
