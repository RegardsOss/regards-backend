/*
 * Copyright 2017-20XX CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.service.projectuser.workflow.state;

import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.service.projectuser.emailverification.IEmailVerificationTokenService;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Action to accept mail verification for a user.
 * The user is currently waiting for mail verification.
 * The user can now use the project.
 *
 * @author Thomas Fache
 **/
public class MailVerification extends AbstractUserAccessUpdate {

    private final LocalDateTime expirationDate;

    private final IEmailVerificationTokenService emailVerificationTokenService;

    public MailVerification(IProjectUserRepository projectUserRepository,
                            ApplicationEventPublisher eventPublisher,
                            IEmailVerificationTokenService emailVerificationTokenService,
                            ProjectUser projectUser,
                            LocalDateTime expirationDate) {
        super(projectUserRepository, eventPublisher, projectUser);
        this.expirationDate = expirationDate;
        this.emailVerificationTokenService = emailVerificationTokenService;
    }

    @Override
    protected List<UserStatus> expectedAccesses() {
        return Collections.singletonList(UserStatus.WAITING_EMAIL_VERIFICATION);
    }

    @Override
    protected void doSpecificValidation() throws EntityOperationForbiddenException {
        if (expirationDate.isBefore(LocalDateTime.now())) {
            throw new EntityOperationForbiddenException(user.getEmail(),
                                                        ProjectUser.class,
                                                        "Verification token has expired");
        }
        emailVerificationTokenService.deleteTokenForProjectUser(user);
    }

    @Override
    protected UserStatus newAccess() {
        return UserStatus.ACCESS_GRANTED;
    }

    @Override
    protected Optional<ApplicationEvent> eventToPublish() {
        return Optional.empty();
    }
}
