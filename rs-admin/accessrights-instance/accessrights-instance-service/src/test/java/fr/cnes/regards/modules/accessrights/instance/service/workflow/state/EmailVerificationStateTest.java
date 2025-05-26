/*

 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.modules.accessrights.instance.service.workflow.state;

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
import fr.cnes.regards.modules.project.service.ITenantService;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;

/**
 * @author Julien Canches
 */
@RunWith(MockitoJUnitRunner.class)
public class EmailVerificationStateTest {

    private static final String EMAIL = "email@test.com";

    private static final String FIRST_NAME = "Firstname";

    private static final String LAST_NAME = "Lastname";

    private static final String PASSWORD = "password";

    private static final String ORIGIN_URL = "originUrl";

    private static final String REQUEST_LINK = "requestLink";

    private @Mock IProjectUsersClient projectUsersClient;

    private @Mock IAccountRepository accountRepository;

    private @Mock ITenantService tenantService;

    private @Mock IRuntimeTenantResolver runtimeTenantResolver;

    private @Mock IPasswordResetService passwordResetService;

    private @Mock IAccountUnlockTokenService accountUnlockTokenService;

    private @Mock IEmailVerificationTokenService emailVerificationTokenService;

    private @Mock ApplicationEventPublisher eventPublisher;

    private @Mock IAccountService accountService;

    private @Mock AccountSettingsService accountSettingsService;

    private EmailVerificationState state;

    @Before
    public void init() {
        state = new EmailVerificationState(projectUsersClient,
                                           accountRepository,
                                           tenantService,
                                           runtimeTenantResolver,
                                           passwordResetService,
                                           accountUnlockTokenService,
                                           emailVerificationTokenService,
                                           eventPublisher,
                                           accountService,
                                           accountSettingsService);
    }

    /**
     * Check that the system does not allow to confirm an email validation token that is expired.
     */
    @Test
    public void fail_if_verification_delay_expired() {
        Account account = new Account(EMAIL, FIRST_NAME, LAST_NAME, PASSWORD);
        EmailVerificationToken emailToken = new EmailVerificationToken(account, ORIGIN_URL, REQUEST_LINK);
        emailToken.setExpiryDate(LocalDateTime.now().minusHours(1));

        Assertions.assertThatExceptionOfType(EntityOperationForbiddenException.class)
                  .isThrownBy(() -> state.verifyEmail(emailToken));
    }

    /**
     * Check that the system moves an account to status PENDING when the email verification
     * token is verified, and when the "auto accept" setting is false.
     */
    @Test
    public void update_account_to_pending() throws Exception {
        Mockito.when(accountSettingsService.isAutoAccept()).thenReturn(false);
        Account account = new Account(EMAIL, FIRST_NAME, LAST_NAME, PASSWORD);
        EmailVerificationToken emailToken = new EmailVerificationToken(account, ORIGIN_URL, REQUEST_LINK);

        state.verifyEmail(emailToken);

        Assertions.assertThat(account.getStatus()).isEqualTo(AccountStatus.PENDING);
    }

    /**
     * Check that the system moves an account to status ACTIVE when the email verification
     * token is verified, and when the "auto accept" setting is true.
     */
    @Test
    public void update_account_to_active() throws Exception {
        Mockito.when(accountSettingsService.isAutoAccept()).thenReturn(true);
        Account account = new Account(EMAIL, FIRST_NAME, LAST_NAME, PASSWORD);
        EmailVerificationToken emailToken = new EmailVerificationToken(account, ORIGIN_URL, REQUEST_LINK);

        state.verifyEmail(emailToken);
        Mockito.verify(accountService).activate(account);
    }

}
