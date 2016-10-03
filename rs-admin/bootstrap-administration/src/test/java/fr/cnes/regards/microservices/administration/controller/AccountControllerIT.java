/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration.controller;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.microservices.core.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.microservices.core.test.RegardsIntegrationTest;
import fr.cnes.regards.modules.accessRights.domain.Account;
import fr.cnes.regards.modules.accessRights.domain.AccountStatus;
import fr.cnes.regards.modules.accessRights.service.IAccountService;
import fr.cnes.regards.security.utils.jwt.JWTService;

/**
 * Just Test the REST API so status code. Correction is left to others.
 *
 * @author svissier
 *
 */
public class AccountControllerIT extends RegardsIntegrationTest {

    @Autowired
    private JWTService jwtService_;

    @Autowired
    private MethodAuthorizationService authService_;

    private String jwt_;

    private String apiAccounts_;

    private String apiAccountId_;

    private String apiAccountSetting_;

    private String apiUnlockAccount_;

    private String apiChangePassword_;

    private final String apiValidatePassword_ = "/accounts/{account_login}/validate?password={account_password}";

    private String errorMessage;

    @Autowired
    private IAccountService accountService_;

    @Value("${root.admin.login:admin}")
    private String rootAdminLogin;

    @Value("${root.admin.password:admin}")
    private String rootAdminPassword;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private String apiAccountCode;

    @Before
    public void init() {
        setLogger(LoggerFactory.getLogger(AccountControllerIT.class));
        jwt_ = jwtService_.generateToken("PROJECT", "email", "SVG", "USER");
        authService_.setAuthorities("/accounts", RequestMethod.GET, "USER");
        authService_.setAuthorities("/accounts", RequestMethod.POST, "USER");
        authService_.setAuthorities("/accounts/{account_id}", RequestMethod.GET, "USER");
        authService_.setAuthorities("/accounts/{account_id}", RequestMethod.PUT, "USER");
        authService_.setAuthorities("/accounts/{account_id}", RequestMethod.DELETE, "USER");
        authService_.setAuthorities("/accounts/code", RequestMethod.GET, "USER");
        authService_.setAuthorities("/accounts/{account_id}/password/{reset_code}", RequestMethod.PUT, "USER");
        authService_.setAuthorities("/accounts/{account_id}/unlock/{unlock_code}", RequestMethod.GET, "USER");
        authService_.setAuthorities("/accounts/settings", RequestMethod.GET, "USER");
        authService_.setAuthorities("/accounts/settings", RequestMethod.PUT, "USER");
        authService_.setAuthorities("/accounts/{account_login}/validate", RequestMethod.GET, "USER");
        errorMessage = "Cannot reach model attributes";
        apiAccounts_ = "/accounts";
        apiAccountId_ = apiAccounts_ + "/{account_id}";
        apiAccountSetting_ = apiAccounts_ + "/settings";
        apiUnlockAccount_ = apiAccountId_ + "/unlock/{unlock_code}";
        apiChangePassword_ = apiAccountId_ + "/password/{reset_code}";
        apiAccountCode = apiAccounts_ + "/code";
    }

    @Test
    public void getAllAccounts() {
        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiAccounts_, jwt_, expectations, errorMessage);
    }

    @Test
    public void getSettings() {
        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiAccountSetting_, jwt_, expectations, errorMessage);
    }

    @Test
    @DirtiesContext
    public void createAccount() {
        Account newAccount;
        newAccount = new Account(1584L, "pEmail@email.email", "pFirstName", "pLastName", "pLogin", "pPassword",
                AccountStatus.PENDING, "pCode");

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isCreated());
        performPost(apiAccounts_, jwt_, newAccount, expectations, errorMessage);

        expectations.clear();
        expectations.add(status().isConflict());
        performPost(apiAccounts_, jwt_, newAccount, expectations, errorMessage);

        Account containNulls = new Account();

        expectations.clear();
        expectations.add(status().isUnprocessableEntity());
        performPost(apiAccounts_, jwt_, containNulls, expectations, errorMessage);
    }

    @Test
    public void getAccount() {

        Long accountId = accountService_.retrieveAccountList().get(0).getId();

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiAccountId_, jwt_, expectations, errorMessage, accountId);

        expectations.clear();
        expectations.add(status().isNotFound());
        performGet(apiAccountId_, jwt_, expectations, errorMessage, Integer.MAX_VALUE);

    }

    @Test
    public void updateAccountSetting() {

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performPut(apiAccountSetting_, jwt_, "manual", expectations, errorMessage);

        expectations.clear();
        expectations.add(status().isOk());
        performPut(apiAccountSetting_, jwt_, "auto-accept", expectations, errorMessage);

        expectations.clear();
        expectations.add(status().isBadRequest());
        performPut(apiAccountSetting_, jwt_, "sdfqjkmfsdq", expectations, errorMessage);
    }

    @Test
    public void getCode() {
        Account account = accountService_.retrieveAccountList().get(0);
        String accountEmail = account.getEmail();

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiAccountCode + "?email=" + accountEmail + "&type=UNLOCK", jwt_, expectations, errorMessage);
    }

    @Test
    public void updateAccount() {
        Account updated = accountService_.retrieveAccountList().get(0);
        updated.setFirstName("AnOtherFirstName");
        Long accountId = updated.getId();

        // if that's the same functional ID and the parameter is valid:
        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performPut(apiAccountId_, jwt_, updated, expectations, errorMessage, accountId);

        // if that's not the same functional ID and the parameter is valid:
        Account notSameID = new Account("notSameEmail", "firstName", "lastName", "login", "password");

        expectations.clear();
        expectations.add(status().isBadRequest());
        performPut(apiAccountId_, jwt_, notSameID, expectations, errorMessage, accountId);
    }

    @Test
    public void unlockAccount() {
        Account account = accountService_.retrieveAccountList().get(0);
        Long accountId = account.getId();

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiUnlockAccount_, jwt_, expectations, errorMessage, accountId, account.getCode());
    }

    @Test
    public void changeAccountPassword() {
        Account account = accountService_.retrieveAccountList().get(0);
        Long accountId = account.getId();

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performPut(apiChangePassword_, jwt_, "newPassword", expectations, errorMessage, accountId, account.getCode());

    }

    @Test
    @DirtiesContext
    public void deleteAccount() {
        Long accountId = accountService_.retrieveAccountList().get(0).getId();

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDelete(apiAccountId_, jwt_, expectations, errorMessage, accountId);

    }

    @Test
    public void validatePassword() {
        Account account = accountService_.retrieveAccountList().get(0);
        String login = account.getLogin();
        String rightPassword = account.getPassword();
        String wrongPassword = "wrongPassword";
        assertNotEquals(rightPassword, wrongPassword);

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiValidatePassword_, jwt_, expectations, errorMessage, login, rightPassword);

        expectations.clear();
        expectations.add(status().isOk());
        performGet(apiValidatePassword_, jwt_, expectations, errorMessage, login, wrongPassword);

        String wrongLogin = "wrongLogin";
        assertFalse(accountService_.existAccount(wrongLogin));
        expectations.clear();
        expectations.add(status().isNotFound());
        performGet(apiValidatePassword_, jwt_, expectations, errorMessage, wrongLogin, rightPassword);
    }

}
