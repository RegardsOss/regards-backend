/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.module.rest.exception.AlreadyExistingException;
import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.dao.instance.IAccountRepository;
import fr.cnes.regards.modules.accessrights.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.instance.AccountSettings;
import fr.cnes.regards.modules.accessrights.service.account.IAccountService;
import fr.cnes.regards.modules.accessrights.service.account.IAccountSettingsService;

/**
 * Integration tests for accounts.
 *
 * @author svissier
 * @author sbinda
 * @author Xavier-Alexandre Brochard
 * @since 1.0-SNAPSHOT
 */
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
public class AccountControllerIT extends AbstractAdministrationIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(AccountControllerIT.class);

    /**
     * A dummy account
     */
    private static Account account;

    /**
     * Dummy email
     */
    private static final String EMAIL = "email@test.com";

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

    @Autowired
    private JWTService jwtService;

    @Autowired
    private MethodAuthorizationService authService;

    private String jwt;

    private String apiAccounts;

    private String apiAccountId;

    private String apiAccountSetting;

    private String apiUnlockAccount;

    private String apiChangePassword;

    private final String apiValidatePassword = "/accounts/{account_login}/validate?password={account_password}";

    private String errorMessage;

    @Autowired
    private IAccountService accountService;

    @Autowired
    private IAccountRepository accountRepository;

    @Autowired
    private IAccountSettingsService settingsService;

    @Value("${root.admin.login:admin}")
    private String rootAdminLogin;

    @Value("${root.admin.password:admin}")
    private String rootAdminPassword;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private String apiAccountCode;

    @Override
    public void init() {
        final String tenant = AbstractAdministrationIT.PROJECT_TEST_NAME;
        jwt = jwtService.generateToken(tenant, "email", "SVG", "USER");
        authService.setAuthorities(tenant, "/accounts", RequestMethod.GET, "USER");
        authService.setAuthorities(tenant, "/accounts", RequestMethod.POST, "USER");
        authService.setAuthorities(tenant, "/accounts/{account_id}", RequestMethod.GET, "USER");
        authService.setAuthorities(tenant, "/accounts/{account_id}", RequestMethod.PUT, "USER");
        authService.setAuthorities(tenant, "/accounts/{account_id}", RequestMethod.DELETE, "USER");
        authService.setAuthorities(tenant, "/accounts/code", RequestMethod.GET, "USER");
        authService.setAuthorities(tenant, "/accounts/{account_id}/password/{reset_code}", RequestMethod.PUT, "USER");
        authService.setAuthorities(tenant, "/accounts/{account_id}/unlock/{unlock_code}", RequestMethod.GET, "USER");
        authService.setAuthorities(tenant, "/accounts/settings", RequestMethod.GET, "USER");
        authService.setAuthorities(tenant, "/accounts/settings", RequestMethod.PUT, "USER");
        authService.setAuthorities(tenant, "/accounts/{account_login}/validate", RequestMethod.GET, "USER");
        errorMessage = "Cannot reach model attributes";
        apiAccounts = "/accounts";
        apiAccountId = apiAccounts + "/{account_id}";
        apiAccountSetting = apiAccounts + "/settings";
        apiUnlockAccount = apiAccountId + "/unlock/{unlock_code}";
        apiChangePassword = apiAccountId + "/password/{reset_code}";
        apiAccountCode = apiAccounts + "/code";

        // Clear the repo
        accountRepository.deleteAll();
        // And start with a single account for convenience
        account = accountRepository.save(new Account(EMAIL, FIRST_NAME, LAST_NAME, PASSWORD));
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_300")
    @Purpose("Check that the system allows to retrieve all users for an instance.")
    public void getAllAccounts() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiAccounts, jwt, expectations, errorMessage);
    }

    @Test
    @Requirement("?")
    @Purpose("Check that the system allows to retrieve account settings for an instance.")
    public void getSettings() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiAccountSetting, jwt, expectations, errorMessage);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_300")
    @Purpose("Check that the system allows to create a user for an instance and handle fail cases.")
    public void createAccount() {
        accountRepository.deleteAll();

        // Regular success case
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isCreated());
        performPost(apiAccounts, jwt, account, expectations, errorMessage);

        // Conflict case
        expectations.clear();
        expectations.add(status().isConflict());
        performPost(apiAccounts, jwt, account, expectations, errorMessage);

        // Malformed case
        final Account containNulls = new Account("notanemail", "", null, null);
        expectations.clear();
        expectations.add(status().isBadRequest());
        performPost(apiAccounts, jwt, containNulls, expectations, errorMessage);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_300")
    @Purpose("Check that the system allows to retrieve a specific user for an instance and handle fail cases.")
    public void getAccount() throws AlreadyExistingException {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiAccountId, jwt, expectations, errorMessage, account.getId());

        expectations.clear();
        expectations.add(status().isNotFound());
        performGet(apiAccountId, jwt, expectations, errorMessage, Integer.MAX_VALUE);
    }

    @Test
    @Purpose("Check that the system allows to update account settings.")
    public void updateAccountSetting() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        final AccountSettings toUpdate = settingsService.retrieve();
        toUpdate.setMode("manual");
        performPut(apiAccountSetting, jwt, toUpdate, expectations, errorMessage);

        expectations.clear();
        expectations.add(status().isOk());
        toUpdate.setMode("auto-accept");
        performPut(apiAccountSetting, jwt, toUpdate, expectations, errorMessage);

        expectations.clear();
        expectations.add(status().isBadRequest());
        toUpdate.setMode("sdfqjkmfsdq");
        performPut(apiAccountSetting, jwt, toUpdate, expectations, errorMessage);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_450")
    @Requirement("REGARDS_DSL_ADM_ADM_460")
    @Requirement("REGARDS_DSL_ADM_ADM_470")
    @Purpose("Check that the system allows to provide a reset/unlock code associated to an instance user.")
    public void getCode() throws AlreadyExistingException {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiAccountCode + "?email=" + account.getEmail() + "&type=UNLOCK", jwt, expectations, errorMessage);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_300")
    @Purpose("Check that the system allows to update user for an instance and handle fail cases.")
    public void updateAccount() throws AlreadyExistingException {
        // Prepare the account
        account.setFirstName("AnOtherFirstName");

        // if that's the same functional ID and the parameter is valid:
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performPut(apiAccountId, jwt, account, expectations, errorMessage, account.getId());

        // if that's not the same functional ID and the parameter is valid:
        expectations.clear();
        expectations.add(status().isBadRequest());
        performPut(apiAccountId, jwt, account, expectations, errorMessage, 99L);

        // If entity not found
        final Long inexistentId = 99L;
        account.setId(inexistentId);
        expectations.clear();
        expectations.add(status().isNotFound());
        performPut(apiAccountId, jwt, account, expectations, errorMessage, inexistentId);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_450")
    @Purpose("Check that the system allows to unlock an instance user's account.")
    public void unlockAccount() throws AlreadyExistingException {
        // Prepare the account
        account.setStatus(AccountStatus.LOCKED);
        accountRepository.save(account);

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiUnlockAccount, jwt, expectations, errorMessage, account.getId(), account.getCode());
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_470")
    @Purpose("Check that the system allows to reset an instance user's password.")
    public void changeAccountPassword() throws AlreadyExistingException {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performPut(apiChangePassword, jwt, "newPassword", expectations, errorMessage, account.getId(),
                   account.getCode());

    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_300")
    @Purpose("Check that the system allows to delete an instance user.")
    public void deleteAccount() throws AlreadyExistingException {
        // Prepare the account. Must have a status allowing deletion
        account.setStatus(AccountStatus.INACTIVE);
        accountRepository.save(account);

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDelete(apiAccountId, jwt, expectations, errorMessage, account.getId());
    }

    @Test
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Purpose("Check that the system allows validate an instance user's password.")
    public void validatePassword() {
        final String wrongPassword = "wrongPassword";
        Assert.assertNotEquals(PASSWORD, wrongPassword);

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiValidatePassword, jwt, expectations, errorMessage, EMAIL, PASSWORD);

        expectations.clear();
        expectations.add(status().isUnauthorized());
        performGet(apiValidatePassword, jwt, expectations, errorMessage, EMAIL, wrongPassword);

        final String wrongEmail = "wrongEmail";
        Assert.assertFalse(accountService.existAccount(wrongEmail));
        expectations.clear();
        expectations.add(status().isNotFound());
        performGet(apiValidatePassword, jwt, expectations, errorMessage, wrongEmail, PASSWORD);
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
