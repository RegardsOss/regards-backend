/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runners.MethodSorters;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.web.servlet.ResultMatcher;

import fr.cnes.regards.microservices.core.security.jwt.JWTService;
import fr.cnes.regards.microservices.modules.test.RegardsIntegrationTest;
import fr.cnes.regards.modules.accessRights.domain.Account;
import fr.cnes.regards.modules.accessRights.service.AccountServiceStub;

/**
 * Just Test the REST API so status code. Correction is left to others.
 *
 * @author svissier
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AccountControllerIT extends RegardsIntegrationTest {

    @Autowired
    private JWTService jwtService_;

    private String jwt_;

    private String apiAccounts;

    private String apiAccountId;

    private String apiAccountSetting;

    private String apiUnlockAccount;

    private String apiChangePassword;

    private String errorMessage;

    @Autowired
    private AccountServiceStub serviceStub;

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
        errorMessage = "Cannot reach model attributes";
        apiAccounts = "/accounts";
        apiAccountId = apiAccounts + "/{account_id}";
        apiAccountSetting = apiAccounts + "/settings";
        apiUnlockAccount = apiAccountId + "/unlock/{unlock_code}";
        apiChangePassword = apiAccountId + "/password/{reset_code}";
        apiAccountCode = apiAccounts + "/code";
    }

    @Test
    public void aGetAllAccounts() {
        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiAccounts, jwt_, expectations, errorMessage);
    }

    @Test
    public void aGetSettings() {
        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiAccountSetting, jwt_, expectations, errorMessage);
    }

    @Test
    public void bCreateAccount() {
        Account newAccount;
        newAccount = new Account("email@email.email", "firstName", "lastName", "login", "password");

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isCreated());
        performPost(apiAccounts, jwt_, newAccount, expectations, errorMessage);

        expectations.clear();
        expectations.add(status().isConflict());
        performPost(apiAccounts, jwt_, newAccount, expectations, errorMessage);

        Account containNulls = new Account();

        expectations.clear();
        expectations.add(status().isUnprocessableEntity());
        performPost(apiAccounts, jwt_, containNulls, expectations, errorMessage);
    }

    @Test
    public void cGetAccount() {

        Long accountId = serviceStub.retrieveAccountList().get(0).getId();

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiAccountId, jwt_, expectations, errorMessage, accountId);

        expectations.clear();
        expectations.add(status().isNotFound());
        performGet(apiAccountId, jwt_, expectations, errorMessage, Integer.MAX_VALUE);

    }

    @Test
    public void dUpdateAccountSetting() {

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performPut(apiAccountSetting, jwt_, "manual", expectations, errorMessage);

        expectations.clear();
        expectations.add(status().isOk());
        performPut(apiAccountSetting, jwt_, "auto-accept", expectations, errorMessage);

        expectations.clear();
        expectations.add(status().isBadRequest());
        performPut(apiAccountSetting, jwt_, "sdfqjkmfsdq", expectations, errorMessage);
    }

    @Test
    public void cGetCode() {
        String accountEmail = "email@email.email";
        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiAccountCode + "?email=" + accountEmail + "&type=UNLOCK", jwt_, expectations, errorMessage);
    }

    @Test
    public void dUpdateAccount() {
        Account updated = serviceStub.retrieveAccount("email@email.email");
        updated.setFirstName("AnOtherFirstName");
        Long accountId = updated.getId();

        // if that's the same functional ID and the parameter is valid:
        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performPut(apiAccountId, jwt_, updated, expectations, errorMessage, accountId);

        // if that's not the same functional ID and the parameter is valid:
        Account notSameID = new Account("notSameEmail", "firstName", "lastName", "login", "password");

        expectations.clear();
        expectations.add(status().isBadRequest());
        performPut(apiAccountId, jwt_, notSameID, expectations, errorMessage, accountId);
    }

    @Test
    public void dUnlockAccount() {

        Account account = serviceStub.retrieveAccount("email@email.email");
        Long accountId = account.getId();

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiUnlockAccount, jwt_, expectations, errorMessage, accountId, account.getCode());
    }

    @Test
    public void dChangeAccountPassword() {
        Account account = serviceStub.retrieveAccount("email@email.email");
        Long accountId = account.getId();

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performPut(apiChangePassword, jwt_, "newPassword", expectations, errorMessage, accountId, account.getCode());

    }

    @Test
    public void eDeleteAccount() {
        Long accountId = serviceStub.retrieveAccountList().get(0).getId();

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDelete(apiAccountId, jwt_, expectations, errorMessage, accountId);

    }

}
