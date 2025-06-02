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
package fr.cnes.regards.modules.accessrights.instance.rest;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.instance.dao.IAccountRepository;
import fr.cnes.regards.modules.accessrights.instance.dao.IPasswordResetTokenRepository;
import fr.cnes.regards.modules.accessrights.instance.dao.accountunlock.IAccountUnlockTokenRepository;
import fr.cnes.regards.modules.accessrights.instance.dao.emailverification.IEmailVerificationTokenRepository;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountNPassword;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.instance.domain.accountunlock.AccountUnlockToken;
import fr.cnes.regards.modules.accessrights.instance.domain.accountunlock.PerformUnlockAccountDto;
import fr.cnes.regards.modules.accessrights.instance.domain.accountunlock.RequestAccountUnlockDto;
import fr.cnes.regards.modules.accessrights.instance.domain.emailverification.EmailVerificationToken;
import fr.cnes.regards.modules.accessrights.instance.domain.emailverification.EmailVerificationTokenDto;
import fr.cnes.regards.modules.accessrights.instance.domain.passwordreset.PasswordResetToken;
import fr.cnes.regards.modules.accessrights.instance.domain.passwordreset.PerformChangePasswordDto;
import fr.cnes.regards.modules.accessrights.instance.domain.passwordreset.PerformResetPasswordDto;
import fr.cnes.regards.modules.accessrights.instance.domain.passwordreset.RequestResetPasswordDto;
import fr.cnes.regards.modules.accessrights.instance.service.encryption.EncryptionUtils;
import fr.cnes.regards.modules.accessrights.instance.service.setting.AccountSettingsService;
import fr.cnes.regards.modules.authentication.client.IExternalAuthenticationClient;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests around account state changes using endpoints 'accounts/{email}/*'
 *
 * @author Xavier-Alexandre Brochard
 * @author Julien Canches
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=admin_instance_rest" })
class AccountStateChangesIT extends AbstractRegardsIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(AccountStateChangesIT.class);

    /**
     * Dummy email
     */
    private static final String EMAIL = "AccountIT@test.com";

    /**
     * Dummy first name
     */
    private static final String FIRST_NAME = "Firstname";

    /**
     * Dummy last name
     */
    private static final String LAST_NAME = "Lastname";

    /**
     * Dummy password
     */
    private static final String PASSWORD = "password";

    /**
     * Dummy password 2
     */
    private static final String PASSWORD2 = "newpassword";

    /**
     * Dummy invalid password (not accepted by regex rules defined in test properties)
     */
    private static final String INVALID_PASSWORD = "PASSWORD";

    /**
     * Dummy origin URL
     */
    private static final String ORIGIN_URL = "abcdef";

    /**
     * Dummy request link
     */
    private static final String REQUEST_LINK = "uvwxyz";

    @Autowired
    private IAccountRepository accountRepository;

    private Account account;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @MockBean
    private IExternalAuthenticationClient externalAuthenticationClient;

    @MockBean
    private IEmailClient emailClient;

    @Autowired
    private AccountSettingsService settings;

    @Autowired
    private IEmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    private IPasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private IAccountUnlockTokenRepository accountUnlockTokenRepository;

    /**
     * Do some setup before each test
     */
    @BeforeEach
    void setUp() {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
    }

    @AfterEach
    void tearDown() {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        emailVerificationTokenRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        accountUnlockTokenRepository.deleteAll();
        accountRepository.deleteAll();
    }

    private void createAccount(AccountStatus status) {
        account = new Account(EMAIL, FIRST_NAME, LAST_NAME, EncryptionUtils.encryptPassword(PASSWORD));
        account.setStatus(status);
        account.setOrigin(Account.REGARDS_ORIGIN);
        account = accountRepository.save(account);
    }

    private Account getAccount() {
        return accountRepository.findById(account.getId()).orElseThrow();
    }

    private void assertAccountIsInState(AccountStatus status) {
        assertThat(getAccount().getStatus()).isEqualTo(status);
    }

    /**
     * Verifies that exactly one email was sent using the email service and returns the email
     * message.
     */
    private String getUniqueEmailMessage() {
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        Mockito.verify(emailClient).sendEmail(message.capture(), Mockito.any(), Mockito.isNull(), Mockito.eq(EMAIL));
        return message.getValue();
    }

    private String extractToken(String emailMessage) {
        Pattern pattern = Pattern.compile(Pattern.quote(REQUEST_LINK)
                                          + "&origin_url="
                                          + Pattern.quote(ORIGIN_URL)
                                          + "&token="
                                          + "([0123456789abcdef-]*)"
                                          + "&account_email="
                                          + Pattern.quote(EMAIL));
        assertThat(emailMessage).containsPattern(pattern);
        Matcher matcher = pattern.matcher(emailMessage);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    static List<Arguments> verifyEmailCases() {
        return List.of(Arguments.of(false, AccountStatus.PENDING), Arguments.of(true, AccountStatus.ACTIVE));
    }

    /**
     * Check that the system allows to create an account and verify that a verification email is sent.
     */
    @Test
    void createAccountAndSendEmail() {
        // GIVEN
        account = new Account(EMAIL, FIRST_NAME, LAST_NAME, PASSWORD);
        AccountNPassword input = new AccountNPassword(account, "password");
        input.setOriginUrl(ORIGIN_URL);
        input.setRequestLink(REQUEST_LINK);
        // WHEN
        String endpoint = AccountsController.TYPE_MAPPING;
        performDefaultPost(endpoint, input, customizer().expectStatusCreated(), "Unable to create" + " " + "the user");
        // THEN
        String emailMessage = getUniqueEmailMessage();
        String token = extractToken(emailMessage);
        Optional<EmailVerificationToken> tokenRecord = emailVerificationTokenRepository.findByToken(token);
        assertThat(tokenRecord).isPresent();
        assertThat(tokenRecord.get().getAccount().getEmail()).isEqualTo(EMAIL);
    }

    /**
     * Check that the system allows a user to verify (confirm) their email.
     */
    @ParameterizedTest
    @MethodSource("verifyEmailCases")
    void verifyEmail(boolean autoAccept, AccountStatus expectedStatus) throws Exception {
        // GIVEN
        settings.setAutoAccept(autoAccept);
        createAccount(AccountStatus.EMAIL_VERIFICATION);
        EmailVerificationToken token = emailVerificationTokenRepository.save(new EmailVerificationToken(account,
                                                                                                        ORIGIN_URL,
                                                                                                        REQUEST_LINK));
        // WHEN
        String endpoint = AccountsController.TYPE_MAPPING + AccountsController.VERIFY_EMAIL_PATH;
        performDefaultGet(endpoint, customizer().expectStatusOk(), "Unable to verify the account", token.getToken());
        // THEN
        assertAccountIsInState(expectedStatus);
    }

    /**
     * Check that the system rejects properly a non existent email verification token.
     */
    @Test
    void rejectNonExistentEmailVerificationToken() {
        // WHEN-THEN
        String endpoint = AccountsController.TYPE_MAPPING + AccountsController.VERIFY_EMAIL_PATH;
        performDefaultGet(endpoint,
                          customizer().expectStatusNotFound(),
                          "A 404 not found is expected",
                          "token-that-doesnt-exist");
    }

    /**
     * Check that the system rejects an expired email verification token.
     */
    @Test
    void rejectExpiredEmailVerificationToken() {
        // GIVEN
        createAccount(AccountStatus.EMAIL_VERIFICATION);
        EmailVerificationToken token = new EmailVerificationToken(account, "theOriginUrl", "theRequestLink");
        token.setExpiryDate(LocalDateTime.now().minusDays(4));
        token = emailVerificationTokenRepository.save(token);
        // WHEN
        String endpoint = AccountsController.TYPE_MAPPING + AccountsController.VERIFY_EMAIL_PATH;
        performDefaultGet(endpoint,
                          customizer().expectStatusForbidden(),
                          "Account operation was not forbidden",
                          token.getToken());
        // THEN
        assertAccountIsInState(AccountStatus.EMAIL_VERIFICATION);
    }

    /**
     * Check that the system allows an instance administrator to reset the account to EMAIL_VERIFICATION
     * provided that its status is not already EMAIL_VERIFICATION.
     */
    @ParameterizedTest
    @EnumSource(value = AccountStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "EMAIL_VERIFICATION")
    void resetEmailVerificationStatus(AccountStatus initialStatus) {
        // GIVEN
        createAccount(initialStatus);
        LocalDateTime time = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        String token = "TOKEN";
        EmailVerificationTokenDto dto = new EmailVerificationTokenDto(time, ORIGIN_URL, REQUEST_LINK, token);
        // WHEN
        String endpoint = AccountsController.TYPE_MAPPING + AccountsController.RESET_EMAIL_CONFIRMATION_PATH;
        performDefaultPost(endpoint,
                           dto,
                           customizer().expectStatusOk(),
                           "Failed to reset to EMAIL_VERIFICATION",
                           EMAIL);
        // THEN
        assertAccountIsInState(AccountStatus.EMAIL_VERIFICATION);
        assertThat(emailVerificationTokenRepository.findByToken(token)).isPresent();
        EmailVerificationToken addedToken = emailVerificationTokenRepository.findByToken(token).get();
        assertThat(addedToken.getExpiryDate()).isEqualTo(time);
        assertThat(addedToken.getOriginUrl()).isEqualTo(ORIGIN_URL);
        assertThat(addedToken.getRequestLink()).isEqualTo(REQUEST_LINK);
    }

    /**
     * Check that the system does not allow an instance administrator to reset the account in
     * EMAIL_VERIFICATION state if the account is already in this state.
     */
    @Test
    void resetEmailVerificationStatusInInvalidState() {
        // GIVEN
        createAccount(AccountStatus.EMAIL_VERIFICATION);
        LocalDateTime time = LocalDateTime.now();
        String token = "TOKEN";
        EmailVerificationTokenDto dto = new EmailVerificationTokenDto(time, ORIGIN_URL, REQUEST_LINK, token);
        // WHEN-THEN
        String endpoint = AccountsController.TYPE_MAPPING + AccountsController.RESET_EMAIL_CONFIRMATION_PATH;
        performDefaultPost(endpoint,
                           dto,
                           customizer().expectStatusConflict(),
                           "resetEmailVerificationStatus did not reject the request",
                           EMAIL);
        // THEN
        assertAccountIsInState(AccountStatus.EMAIL_VERIFICATION);
    }

    /**
     * Check that the system allows an admin to manually accept an account.
     */
    @ParameterizedTest
    @EnumSource(value = AccountStatus.class, mode = EnumSource.Mode.INCLUDE, names = { "PENDING", "INACTIVE_PASSWORD" })
    @Requirement("REGARDS_DSL_ADM_ADM_510")
    void acceptAccount(AccountStatus initialStatus) throws EntityException {
        // GIVEN
        settings.setAutoAccept(false);
        createAccount(initialStatus);
        // WHEN-THEN
        String endpoint = AccountsController.TYPE_MAPPING + AccountsController.ACCEPT_ACCOUNT_PATH;
        performDefaultPut(endpoint, null, customizer().expectStatusOk(), "Unable to accept the account", EMAIL);
        // THEN
        assertAccountIsInState(AccountStatus.ACTIVE);
    }

    /**
     * Check that the system does not allow an admin to accept an account that is not in PENDING state.
     */
    @ParameterizedTest
    @EnumSource(value = AccountStatus.class, mode = EnumSource.Mode.EXCLUDE, names = { "PENDING", "INACTIVE_PASSWORD" })
    void acceptAccountInInvalidState(AccountStatus initialStatus) throws EntityException {
        // GIVEN
        settings.setAutoAccept(false);
        createAccount(initialStatus);
        // WHEN-THEN
        String endpoint = AccountsController.TYPE_MAPPING + AccountsController.ACCEPT_ACCOUNT_PATH;
        performDefaultPut(endpoint,
                          null,
                          customizer().expectStatusForbidden(),
                          "The request to accept a user in state " + initialStatus + " was not forbidden",
                          EMAIL);
        // THEN
        assertAccountIsInState(initialStatus);
    }

    /**
     * Check that the system allows an admin to manually refuse an account.
     */
    @ParameterizedTest
    @EnumSource(value = AccountStatus.class,
                mode = EnumSource.Mode.INCLUDE,
                names = { "PENDING", "EMAIL_VERIFICATION", "INACTIVE_PASSWORD" })
    void refuseAccount(AccountStatus initialStatus) throws EntityException {
        // GIVEN
        settings.setAutoAccept(false);
        createAccount(initialStatus);
        // WHEN-THEN
        String endpoint = AccountsController.TYPE_MAPPING + AccountsController.REFUSE_ACCOUNT_PATH;
        performDefaultPut(endpoint, null, customizer().expectStatusOk(), "Unable to refuse the account", EMAIL);
        // THEN
        assertThat(accountRepository.findById(account.getId())).isEmpty();
    }

    /**
     * Check that the system does not allow an admin to manually refuse an account that is not in PENDING status.
     */
    @ParameterizedTest
    @EnumSource(value = AccountStatus.class,
                mode = EnumSource.Mode.EXCLUDE,
                names = { "PENDING", "EMAIL_VERIFICATION", "INACTIVE_PASSWORD" })
    void refuseAccountInInvalidState(AccountStatus initialStatus) throws EntityException {
        // GIVEN
        settings.setAutoAccept(false);
        createAccount(initialStatus);
        // WHEN-THEN
        String endpoint = AccountsController.TYPE_MAPPING + AccountsController.REFUSE_ACCOUNT_PATH;
        performDefaultPut(endpoint,
                          null,
                          customizer().expectStatusForbidden(),
                          "Request to refuse the account with status " + initialStatus + " was not forbidden",
                          EMAIL);
        // THEN
        assertAccountIsInState(initialStatus);
    }

    /**
     * Check that the system allows an admin to inactivate an account.
     */
    @Test
    void inactivateAccount() {
        // GIVEN
        createAccount(AccountStatus.ACTIVE);
        // WHEN
        String endpoint = AccountsController.TYPE_MAPPING + AccountsController.INACTIVE_ACCOUNT_PATH;
        performDefaultPut(endpoint, null, customizer().expectStatusOk(), "Unable to deactivate the account", EMAIL);
        // THEN
        assertAccountIsInState(AccountStatus.INACTIVE);
    }

    /**
     * Check that the system does not allow an admin to inactivate an account that is not active.
     */
    @ParameterizedTest
    @EnumSource(value = AccountStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "ACTIVE")
    void inactivateAccountInInvalidState(AccountStatus initialState) {
        // GIVEN
        createAccount(initialState);
        // WHEN
        String endpoint = AccountsController.TYPE_MAPPING + AccountsController.INACTIVE_ACCOUNT_PATH;
        performDefaultPut(endpoint,
                          null,
                          customizer().expectStatusForbidden(),
                          "Unable to deactivate the account",
                          EMAIL);
        // THEN
        assertAccountIsInState(initialState);
    }

    /**
     * Check that the system allows an admin to activate an inactive account.
     */
    @Test
    void activateAccount() {
        // GIVEN
        createAccount(AccountStatus.INACTIVE);
        // WHEN
        String endpoint = AccountsController.TYPE_MAPPING + AccountsController.ACTIVE_ACCOUNT_PATH;
        performDefaultPut(endpoint, null, customizer().expectStatusOk(), "Unable to activate the account", EMAIL);
        // THEN
        assertAccountIsInState(AccountStatus.ACTIVE);
    }

    /**
     * Check that the system does not allow an admin to activate an account that is not inactive.
     */
    @ParameterizedTest
    @EnumSource(value = AccountStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "INACTIVE")
    void activateAccountInInvalidState(AccountStatus initialState) {
        // GIVEN
        createAccount(initialState);
        // WHEN
        String endpoint = AccountsController.TYPE_MAPPING + AccountsController.ACTIVE_ACCOUNT_PATH;
        performDefaultPut(endpoint,
                          null,
                          customizer().expectStatusForbidden(),
                          "Unable to deactivate the account",
                          EMAIL);
        // THEN
        assertAccountIsInState(initialState);
    }

    /**
     * Check that the system allows a user to request unlocking their account.
     */
    @Test
    void requestUnlockAccount() {
        // GIVEN
        createAccount(AccountStatus.LOCKED);
        // WHEN
        String endpoint = AccountsController.TYPE_MAPPING + AccountsController.UNLOCK_ACCOUNT_PATH;
        RequestAccountUnlockDto request = new RequestAccountUnlockDto(ORIGIN_URL, REQUEST_LINK);
        performDefaultPost(endpoint,
                           request,
                           customizer().expectStatusNoContent(),
                           "Unable to request unlock of  the account",
                           EMAIL);
        // THEN
        String emailMessage = getUniqueEmailMessage();
        String token = extractToken(emailMessage);
        Optional<AccountUnlockToken> tokenRecord = accountUnlockTokenRepository.findByToken(token);
        assertThat(tokenRecord).isPresent();
        assertThat(tokenRecord.get().getAccount().getEmail()).isEqualTo(EMAIL);
    }

    /**
     * Check that the system does not allow a user to request unlocking if the account is not locked.
     */
    @ParameterizedTest
    @EnumSource(value = AccountStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "LOCKED")
    void requestUnlockAccountInInvalidState(AccountStatus initialStatus) {
        // GIVEN
        createAccount(initialStatus);
        // WHEN
        String endpoint = AccountsController.TYPE_MAPPING + AccountsController.UNLOCK_ACCOUNT_PATH;
        RequestAccountUnlockDto request = new RequestAccountUnlockDto(ORIGIN_URL, REQUEST_LINK);
        performDefaultPost(endpoint,
                           request,
                           customizer().expectStatusForbidden(),
                           "Unlock request was not forbidden",
                           EMAIL);
        // THEN
        assertAccountIsInState(initialStatus);
    }

    /**
     * Check that the system allows a user to unlock their account using the token received in email.
     */
    @Test
    void performUnlockAccount() {
        // GIVEN
        createAccount(AccountStatus.LOCKED);
        AccountUnlockToken token = accountUnlockTokenRepository.save(new AccountUnlockToken("tokenUUID", account));
        // WHEN
        String endpoint = AccountsController.TYPE_MAPPING + AccountsController.UNLOCK_ACCOUNT_PATH;
        PerformUnlockAccountDto perform = new PerformUnlockAccountDto();
        perform.setToken(token.getToken());
        performDefaultPut(endpoint,
                          perform,
                          customizer().expectStatusNoContent(),
                          "Unable to unlock the account",
                          EMAIL);
        // THEN
        assertAccountIsInState(AccountStatus.ACTIVE);
    }

    /**
     * Check that the system does not allow a user to unlock their account using the token received in email
     * if the account is no longer locked.
     */
    @ParameterizedTest
    @EnumSource(value = AccountStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "LOCKED")
    void performUnlockAccountInInvalidState(AccountStatus initialStatus) {
        // GIVEN
        createAccount(initialStatus);
        AccountUnlockToken token = accountUnlockTokenRepository.save(new AccountUnlockToken("tokenUUID", account));
        // WHEN
        String endpoint = AccountsController.TYPE_MAPPING + AccountsController.UNLOCK_ACCOUNT_PATH;
        PerformUnlockAccountDto perform = new PerformUnlockAccountDto();
        perform.setToken(token.getToken());
        performDefaultPut(endpoint,
                          perform,
                          customizer().expectStatusForbidden(),
                          "Unlock action was not forbidden",
                          EMAIL);
        // THEN
        assertAccountIsInState(initialStatus);
    }

    /**
     * Check that the system allows a user to request a password reset.
     */
    @ParameterizedTest
    @EnumSource(value = AccountStatus.class)
    void requestResetPassword() {
        // GIVEN
        createAccount(AccountStatus.ACTIVE);
        // WHEN
        String endpoint = AccountsController.TYPE_MAPPING + AccountsController.RESET_PASSWORD_PATH;
        RequestResetPasswordDto request = new RequestResetPasswordDto(ORIGIN_URL, REQUEST_LINK);
        performDefaultPost(endpoint,
                           request,
                           customizer().expectStatusNoContent(),
                           "Unable to request password reset",
                           EMAIL);
        // THEN
        String emailMessage = getUniqueEmailMessage();
        String token = extractToken(emailMessage);
        Optional<PasswordResetToken> tokenRecord = passwordResetTokenRepository.findByToken(token);
        assertThat(tokenRecord).isPresent();
        assertThat(tokenRecord.get().getAccount().getEmail()).isEqualTo(EMAIL);
    }

    /**
     * Check that the system allows a user to change their password using a token received by email.
     */
    @ParameterizedTest
    @EnumSource(value = AccountStatus.class)
    void performResetPassword(AccountStatus initialStatus) {
        // GIVEN
        createAccount(initialStatus);
        String tokenValue = "tokenUUID";
        passwordResetTokenRepository.save(new PasswordResetToken(tokenValue, account));
        // WHEN
        String endpoint = AccountsController.TYPE_MAPPING + AccountsController.RESET_PASSWORD_PATH;
        PerformResetPasswordDto perform = new PerformResetPasswordDto(tokenValue, PASSWORD2);
        performDefaultPut(endpoint,
                          perform,
                          customizer().expectStatusNoContent(),
                          "Unable to change the account password through a reset token",
                          EMAIL);
        // THEN
        assertAccountIsInState(AccountStatus.ACTIVE); // Looks surprising but a cron task should make it INACTIVE if
        // the validity is expired
        assertThat(getAccount().getPassword()).isEqualTo(EncryptionUtils.encryptPassword(PASSWORD2));
    }

    /**
     * Check that the system allows a user to change their password using a token received by email.
     */
    @Test
    void performResetPasswordWithInvalidNewPassword() {
        // GIVEN
        createAccount(AccountStatus.ACTIVE);
        String tokenValue = "tokenUUID";
        passwordResetTokenRepository.save(new PasswordResetToken(tokenValue, account));
        // WHEN
        String endpoint = AccountsController.TYPE_MAPPING + AccountsController.RESET_PASSWORD_PATH;
        PerformResetPasswordDto perform = new PerformResetPasswordDto(tokenValue, INVALID_PASSWORD);
        performDefaultPut(endpoint,
                          perform,
                          // TODO different from response code in performChangePassword although this is the same
                          // invalid input
                          customizer().expectStatus(HttpStatus.UNPROCESSABLE_ENTITY),
                          "Invalid new request was not rejected",
                          EMAIL);
        // THEN
        assertAccountIsInState(AccountStatus.ACTIVE);
        assertThat(getAccount().getPassword()).isEqualTo(EncryptionUtils.encryptPassword(PASSWORD));
    }

    /**
     * Check that the system allows a user to change their password using their old password.
     */
    @ParameterizedTest
    @EnumSource(value = AccountStatus.class)
    void performChangePassword(AccountStatus accountStatus) {
        // GIVEN
        createAccount(accountStatus);
        // WHEN
        String endpoint = AccountsController.TYPE_MAPPING + AccountsController.CHANGE_PASSWORD_PATH;
        PerformChangePasswordDto perform = new PerformChangePasswordDto(PASSWORD, PASSWORD2);
        performDefaultPut(endpoint,
                          perform,
                          customizer().expectStatusNoContent(),
                          "Unable to change the account password",
                          EMAIL);
        // THEN
        assertAccountIsInState(AccountStatus.ACTIVE);  // Looks surprising but this seems to be by design
        assertThat(getAccount().getPassword()).isEqualTo(EncryptionUtils.encryptPassword(PASSWORD2));
    }

    /**
     * Check that the system does not allow a user to change their password using their old password, if the
     * new password does not meet the requirements.
     */
    @ParameterizedTest
    @EnumSource(value = AccountStatus.class)
    void performChangePasswordWithInvalidNewPassword(AccountStatus accountStatus) {
        // GIVEN
        createAccount(accountStatus);
        // WHEN
        String endpoint = AccountsController.TYPE_MAPPING + AccountsController.CHANGE_PASSWORD_PATH;
        PerformChangePasswordDto perform = new PerformChangePasswordDto(PASSWORD, INVALID_PASSWORD);
        performDefaultPut(endpoint,
                          perform,
                          customizer().expectStatusBadRequest(),
                          "Change to invalid new password was not rejected",
                          EMAIL);
        // THEN
        assertAccountIsInState(accountStatus);
        assertThat(getAccount().getPassword()).isEqualTo(EncryptionUtils.encryptPassword(PASSWORD));
    }

    /**
     * Check that the system allows to delete an account whatever its status is.
     */
    @ParameterizedTest
    @EnumSource(value = AccountStatus.class)
    void removeAccount(AccountStatus accountStatus) {
        // GIVEN
        createAccount(accountStatus);
        // WHEN
        String endpoint = AccountsController.TYPE_MAPPING + AccountsController.ACCOUNT_ID_PATH;
        performDefaultDelete(endpoint, customizer().expectStatusNoContent(), "Delete account failed", account.getId());
        // THEN
        assertThat(accountRepository.findById(account.getId())).isEmpty();
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
