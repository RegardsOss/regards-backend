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
package fr.cnes.regards.modules.accessrights.instance.service.workflow.state;

import fr.cnes.regards.framework.jpa.instance.transactional.InstanceTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.instance.dao.IAccountRepository;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.instance.domain.emailverification.EmailVerificationToken;
import fr.cnes.regards.modules.accessrights.instance.service.IAccountService;
import fr.cnes.regards.modules.accessrights.instance.service.accountunlock.IAccountUnlockTokenService;
import fr.cnes.regards.modules.accessrights.instance.service.emailverification.IEmailVerificationTokenService;
import fr.cnes.regards.modules.accessrights.instance.service.passwordreset.IPasswordResetService;
import fr.cnes.regards.modules.accessrights.instance.service.setting.AccountSettingsService;
import fr.cnes.regards.modules.accessrights.instance.service.workflow.events.OnRefuseAccountEvent;
import fr.cnes.regards.modules.project.service.ITenantService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * State class of the State Pattern implementing the available actions on a {@link Account} in status
 * EMAIL_VERIFICATION.
 *
 * @author Julien Canches
 */
@Component
public class EmailVerificationState extends AbstractDeletableState {

    /**
     * Use this to publish Spring application events
     */
    private final ApplicationEventPublisher eventPublisher;

    private final IAccountService accountService;

    private final AccountSettingsService accountSettingsService;

    public EmailVerificationState(IProjectUsersClient projectUsersClient,
                                  IAccountRepository accountRepository,
                                  ITenantService tenantService,
                                  IRuntimeTenantResolver runtimeTenantResolver,
                                  IPasswordResetService passwordResetService,
                                  IAccountUnlockTokenService accountUnlockTokenService,
                                  IEmailVerificationTokenService emailVerificationTokenService,
                                  ApplicationEventPublisher eventPublisher,
                                  IAccountService accountService,
                                  AccountSettingsService accountSettingsService) {
        super(projectUsersClient,
              accountRepository,
              tenantService,
              runtimeTenantResolver,
              passwordResetService,
              accountUnlockTokenService,
              emailVerificationTokenService);
        this.eventPublisher = eventPublisher;
        this.accountService = accountService;
        this.accountSettingsService = accountSettingsService;
    }

    @Override
    @InstanceTransactional
    public void verifyEmail(EmailVerificationToken token) throws EntityException {
        Account account = token.getAccount();
        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new EntityOperationForbiddenException(account.getEmail(),
                                                        Account.class,
                                                        "Verification token has expired");
        }
        emailVerificationTokenService.deleteTokenForAccount(account);
        if (accountSettingsService.isAutoAccept()) {
            accountService.activate(account);
        } else {
            account.setStatus(AccountStatus.PENDING);
            accountRepository.save(account);
        }
    }

    @Override
    public void refuseAccount(final Account account) throws EntityException {
        deleteLinkedProjectUsers(account);
        deleteAccount(account);
        eventPublisher.publishEvent(new OnRefuseAccountEvent(account));
    }
}
