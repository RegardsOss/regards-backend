/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;
import fr.cnes.regards.modules.accessrights.service.projectuser.emailverification.IEmailVerificationTokenService;
import fr.cnes.regards.modules.accessrights.service.projectuser.workflow.listeners.WaitForQualificationListener;
import fr.cnes.regards.modules.accessrights.service.utils.AccountUtilsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * {@link IRegistrationService} implementation.
 *
 * @author Xavier-Alexandre Brochard
 * @author Sébastien Binda
 */
@Service
@RegardsTransactional
public class RegistrationService implements IRegistrationService {

    private static final Logger LOG = LoggerFactory.getLogger(RegistrationService.class);

    private final IProjectUserService projectUserService;

    private final IEmailVerificationTokenService tokenService;

    private final WaitForQualificationListener listener;

    private final AccountUtilsService accountUtilsService;

    public RegistrationService(IProjectUserService projectUserService,
                               IEmailVerificationTokenService tokenService,
                               WaitForQualificationListener listener,
                               AccountUtilsService accountUtilsService) {
        this.projectUserService = projectUserService;
        this.tokenService = tokenService;
        this.listener = listener;
        this.accountUtilsService = accountUtilsService;
    }

    @Override
    public ProjectUser requestAccess(AccessRequestDto accessRequestDto, boolean isExternalAccess)
        throws EntityException {

        String email = accessRequestDto.getEmail();
        ProjectUser projectUser = projectUserService.create(accessRequestDto, isExternalAccess, null, null);
        Account account = accountUtilsService.retrieveAccount(email);

        if (!isExternalAccess) {
            // Init the email verification token
            tokenService.create(projectUser, accessRequestDto.getOriginUrl(), accessRequestDto.getRequestLink());
            // Sending proper event if account is active
            if (AccountStatus.ACTIVE.equals(account.getStatus())) {
                LOG.info(
                    "Account is already active for new user {}. Sending AccountAcceptedEvent to handle ProjectUser status.",
                    email);
                listener.onAccountActivation(email);
            }
        }

        return projectUser;
    }

}
