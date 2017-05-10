/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.jpa.instance.transactional.InstanceTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.dao.accountunlock.IAccountUnlockTokenRepository;
import fr.cnes.regards.modules.accessrights.dao.instance.IAccountRepository;
import fr.cnes.regards.modules.accessrights.dao.instance.IPasswordResetTokenRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.domain.accountunlock.AccountUnlockToken;
import fr.cnes.regards.modules.accessrights.domain.accountunlock.RequestAccountUnlockDto;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.instance.AccountSettings;
import fr.cnes.regards.modules.accessrights.domain.passwordreset.PasswordResetToken;
import fr.cnes.regards.modules.accessrights.domain.passwordreset.PerformResetPasswordDto;
import fr.cnes.regards.modules.accessrights.domain.passwordreset.RequestResetPasswordDto;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.service.account.IAccountSettingsService;
import fr.cnes.regards.modules.emails.client.IEmailClient;

/**
 * Integration tests for accounts.
 *
 * @author svissier
 * @author Sébastien Binda
 * @author Xavier-Alexandre Brochard
 * @author Christophe Mertz
 * @since 1.0-SNAPSHOT
 */
@InstanceTransactional
@ContextConfiguration(classes = { FeignClientConfiguration.class })
public class AccountControllerIT extends AbstractRegardsTransactionalIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(AccountControllerIT.class);

    /**
     * Dummy email
     */
    private static final String EMAIL = "email@test.com";

    /**
     * Other dummy email
     */
    private static final String EMAIL_LOCKED = "email.locked@test.com";

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

    private String apiAccounts;

    private String apiAccountId;

    private String apiAccountEmail;

    private String apiAccountSetting;

    private final String apiUnlockAccount = "/accounts/{account_email}/unlockAccount";

    private final String apiChangePassword = "/accounts/{account_email}/resetPassword";

    private String errorMessage;

    /**
     * A dummy account
     */
    private Account account;

    /**
     * A dummy instance account
     */
    private Account accountInstance;

    /**
     * Account repository
     */
    @Autowired
    private IAccountRepository accountRepository;

    /**
     * Project user repository
     */
    @Autowired
    private IProjectUserRepository projectUserRepository;

    /**
     * Account setting repository
     */
    @Autowired
    private IAccountSettingsService settingsService;

    /**
     * PasswordResetToken repository
     */
    @Autowired
    private IPasswordResetTokenRepository passwordResetTokenRepository;

    /**
     * AccountUnlockToken repository
     */
    @Autowired
    private IAccountUnlockTokenRepository accountUnlockTokenRepository;

    @Autowired
    private IEmailClient emailClient;

    @Value("${regards.accounts.root.user.login}")
    private String rootAdminInstanceLogin;

    private Account accountLocked;

    @Before
    public void setUp() {
        errorMessage = "Cannot reach model attributes";
        apiAccounts = "/accounts";
        apiAccountId = apiAccounts + "/{account_id}";
        apiAccountEmail = apiAccounts + "/account/{account_email}";
        apiAccountSetting = apiAccounts + "/settings";

        // And start with a single account for convenience
        account = accountRepository.save(new Account(EMAIL, FIRST_NAME, LAST_NAME, PASSWORD));
        accountLocked = new Account(EMAIL_LOCKED, FIRST_NAME, LAST_NAME, PASSWORD);
        accountLocked.setStatus(AccountStatus.LOCKED);
        accountLocked = accountRepository.save(accountLocked);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_300")
    @Purpose("Check that the system allows to retrieve all users for an instance.")
    public void getAllAccounts() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDefaultGet(apiAccounts, expectations, errorMessage);
    }

    @Test
    public void getSettings() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDefaultGet(apiAccountSetting, expectations, errorMessage);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_300")
    @Purpose("Check that the system allows to retrieve a specific user for an instance and handle fail cases.")
    public void getAccount() throws EntityAlreadyExistsException {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDefaultGet(apiAccountId, expectations, errorMessage, account.getId());

        expectations.clear();
        expectations.add(status().isNotFound());
        performDefaultGet(apiAccountId, expectations, errorMessage, Integer.MAX_VALUE);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_300")
    @Purpose("Check that the system allows to retrieve a specific user for an instance and handle fail cases.")
    public void getAccountByEmail() throws EntityAlreadyExistsException {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDefaultGet(apiAccountEmail, expectations, errorMessage, account.getEmail());

        expectations.clear();
        expectations.add(status().isNotFound());
        performDefaultGet(apiAccountEmail, expectations, errorMessage, "error@regards.fr");
    }

    @Test
    @Purpose("Check that the system allows to update account settings.")
    public void updateAccountSetting() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        final AccountSettings toUpdate = settingsService.retrieve();
        toUpdate.setMode("manual");
        performDefaultPut(apiAccountSetting, toUpdate, expectations, errorMessage);

        expectations.clear();
        expectations.add(status().isOk());
        toUpdate.setMode(AccountSettings.AUTO_ACCEPT_MODE);
        performDefaultPut(apiAccountSetting, toUpdate, expectations, errorMessage);

        expectations.clear();
        expectations.add(status().isUnprocessableEntity());
        toUpdate.setMode("sdfqjkmfsdq");
        performDefaultPut(apiAccountSetting, toUpdate, expectations, errorMessage);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_300")
    @Purpose("Check that the system allows to update user for an instance and handle fail cases.")
    public void updateAccount() throws EntityAlreadyExistsException {
        // Prepare the account
        account.setFirstName("AnOtherFirstName");

        // if that's the same functional ID and the parameter is valid:
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDefaultPut(apiAccountId, account, expectations, errorMessage, account.getId());

        // if that's not the same functional ID and the parameter is valid:
        expectations.clear();
        expectations.add(status().isNotFound());
        performDefaultPut(apiAccountId, account, expectations, errorMessage, 99L);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_470")
    @Purpose("Check that the system allows the user to request to receive a mail in order to reset its password.")
    public void requestUnlockAccount() throws EntityAlreadyExistsException {
        // Prepare the test
        final RequestAccountUnlockDto dto = new RequestAccountUnlockDto("/origin/url", "request/link");

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isNoContent());
        performDefaultPost(apiUnlockAccount, dto, expectations, errorMessage, EMAIL_LOCKED);

        Mockito.verify(emailClient, Mockito.only()).sendEmail(Mockito.any());
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_470")
    @Purpose("Check that the system allows to reset an instance user's password.")
    public void performUnlockAccount() throws EntityAlreadyExistsException {
        // Prepare the test
        final String token = "token";

        // Create the token in db
        accountUnlockTokenRepository.save(new AccountUnlockToken(token, accountLocked));

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isNoContent());
        performDefaultPut(apiUnlockAccount, token, expectations, errorMessage, EMAIL_LOCKED);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_470")
    @Purpose("Check that the system allows the user to request to receive a mail in order to reset its password.")
    public void requestResetPassword() throws EntityAlreadyExistsException {
        // Prepare the request parameters
        final RequestResetPasswordDto dto = new RequestResetPasswordDto("/origin/url", "reset/url");

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isNoContent());
        performDefaultPost(apiChangePassword, dto, expectations, errorMessage, account.getEmail());
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_470")
    @Purpose("Check that the system allows to reset an instance user's password.")
    public void performResetPassword() throws EntityAlreadyExistsException {
        // Prepare the request parameters
        final PerformResetPasswordDto dto = new PerformResetPasswordDto("token", "newpassword");

        // Create the token in db
        passwordResetTokenRepository.save(new PasswordResetToken(dto.getToken(), account));

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isNoContent());
        performDefaultPut(apiChangePassword, dto, expectations, errorMessage, account.getEmail());
    }

    /**
     * Check that the system allows to delete an account linked to no project user.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_300")
    @Purpose("Check that the system allows to delete an account linked to no project user.")
    public void deleteAccount() {
        // Prepare the account. Must have a status allowing deletion and have no project user
        account.setEmail("randomEmailNotMatchingAnyProjectUser@test.com");
        account.setStatus(AccountStatus.INACTIVE);
        accountRepository.save(account);

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isNoContent());
        performDefaultDelete(apiAccountId, expectations, errorMessage, account.getId());
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_300")
    @Purpose("Check that the system does not allow to delete an account linked to at least one project user")
    public void deleteAccount_shouldNotAllowDeletion() {
        // Must have account with status allowing deletion and have a linked project user
        String email = "randomEmailMatchingAProjectUser@test.com";
        account.setEmail(email);
        account.setStatus(AccountStatus.LOCKED);
        accountRepository.save(account);

        ProjectUser projectUser = projectUserRepository.findOneByEmail(email).orElse(new ProjectUser());
        projectUser.setEmail(email);
        projectUserRepository.save(projectUser);

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isForbidden());
        performDefaultDelete(apiAccountId, expectations, errorMessage, account.getId());
    }

    @Test
    public void getAccountUser() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath("$.links", Matchers.notNullValue()));
        // expectations.add(MockMvcResultMatchers.jsonPath("$._links.delete", Matchers.notNullValue()));
        // expectations.add(MockMvcResultMatchers.jsonPath("$._links.self", Matchers.notNullValue()));
        // expectations.add(MockMvcResultMatchers.jsonPath("$._links.update", Matchers.notNullValue()));
        // expectations.add(MockMvcResultMatchers.jsonPath("$._links.delete.href", Matchers.notNullValue()));
        // expectations.add(MockMvcResultMatchers.jsonPath("$._links.self.href", Matchers.notNullValue()));
        // expectations.add(MockMvcResultMatchers.jsonPath("$._links.update.href", Matchers.notNullValue()));

        performDefaultGet(apiAccountId, expectations, errorMessage, account.getId());
    }

    @Test
    public void getAccountInstance() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath("$.links", Matchers.notNullValue()));
        // expectations.add(MockMvcResultMatchers.jsonPath("$._links.self", Matchers.notNullValue()));
        // expectations.add(MockMvcResultMatchers.jsonPath("$._links.update", Matchers.notNullValue()));
        // expectations.add(MockMvcResultMatchers.jsonPath("$._links.self.href", Matchers.notNullValue()));
        // expectations.add(MockMvcResultMatchers.jsonPath("$._links.update.href", Matchers.notNullValue()));
        // expectations.add(MockMvcResultMatchers.jsonPath("$._links",
        // Matchers.not(Matchers.containsString(LinkRels.DELETE))));
        accountInstance = accountRepository.findOneByEmail(rootAdminInstanceLogin).get();
        performDefaultGet(apiAccountId, expectations, errorMessage, accountInstance.getId());
    }

    @Test
    @Requirement("REGARDS_DSL_SYS_SEC_300")
    @Purpose("password respects a regular expression which is configurable by instance")
    public void checkPassword() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath("$.validity", Matchers.is(true)));
        performDefaultPost(AccountsController.REQUEST_MAPPING_ROOT + AccountsController.PATH_PASSWORD, PASSWORD,
                           expectations, errorMessage);
        expectations.clear();
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath("$.validity", Matchers.is(false)));
        performDefaultPost(AccountsController.REQUEST_MAPPING_ROOT + AccountsController.PATH_PASSWORD, PASSWORD + "ZE",
                           expectations, errorMessage);
    }

    @Test
    public void getPasswordRules() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isOk());
        performDefaultGet(AccountsController.REQUEST_MAPPING_ROOT + AccountsController.PATH_PASSWORD, expectations,
                          errorMessage);
    }

    @Test
    @Purpose("Check we do not add 'remove' HATEOAS link if the account is linked to project users")
    public void checkHateoasLinks_doNotAddRemoveIfLinkedToUsers() {
        // Must have account with status allowing deletion and have a linked project user
        String email = "randomEmailMatchingAProjectUser@test.com";
        account.setEmail(email);
        account.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(account);

        ProjectUser projectUser = projectUserRepository.findOneByEmail(email).orElse(new ProjectUser());
        projectUser.setEmail(email);
        projectUserRepository.save(projectUser);

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath("$.links", Matchers.hasSize(1)));
        expectations.add(MockMvcResultMatchers.jsonPath("$.links.[0].rel", Matchers.is("self")));
        performDefaultGet(apiAccountId, expectations, errorMessage, account.getId());
    }

    @Test
    @Purpose("Check we do add 'remove' HATEOAS link if the account is linked to no project users")
    public void checkHateoasLinks_addRemoveIfNotLinkedToUsers() {
        // Must have account with status allowing deletion and have a no linked project user
        String email = "randomEmailMatchingNoProjectUser@test.com";
        account.setEmail(email);
        account.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(account);

        // Insert authoriry
        setAuthorities(apiAccountId, RequestMethod.DELETE, "INSTANCE_ADMIN");

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath("$.links", Matchers.hasSize(2)));
        expectations.add(MockMvcResultMatchers.jsonPath("$.links.[1].rel", Matchers.is("delete")));
        performDefaultGet(apiAccountId, expectations, errorMessage, account.getId());
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
