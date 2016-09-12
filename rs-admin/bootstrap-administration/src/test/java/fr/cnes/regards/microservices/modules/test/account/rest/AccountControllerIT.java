package fr.cnes.regards.microservices.modules.test.account.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.List;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.microservices.modules.test.RegardsIntegrationTest;
import fr.cnes.regards.modules.users.domain.Account;
import fr.cnes.regards.modules.users.service.AccountServiceStub;

/**
 * Just Test the REST API so status code. Correction is left to others.
 *
 * @author svissier
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AccountControllerIT extends RegardsIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountControllerIT.class);

    private TestRestTemplate restTemplate;

    private String apiAccounts;

    private String apiAccountId;

    private String apiAccountSetting;

    @Autowired
    private AccountServiceStub serviceStub;

    @Value("${root.admin.login:admin}")
    private String rootAdminLogin;

    @Value("${root.admin.password:admin}")
    private String rootAdminPassword;

    @Before
    public void init() {
        if (restTemplate == null) {
            restTemplate = buildOauth2RestTemplate("acme", "acmesecret", "admin", "admin", "");
        }
        this.apiAccounts = getApiEndpoint().concat("/accounts");
        this.apiAccountId = this.apiAccounts + "/{account_id}";
        this.apiAccountSetting = this.apiAccounts + "/settings";
    }

    @Test
    public void aGetAllAccounts() {

        // we have to use exchange instead of getForEntity as long as we use List otherwise the response body is not
        // well casted.
        ParameterizedTypeReference<List<Account>> typeRef = new ParameterizedTypeReference<List<Account>>() {
        };
        ResponseEntity<List<Account>> response = restTemplate.exchange(this.apiAccounts, HttpMethod.GET, null, typeRef);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void aGetSettings() {
        ParameterizedTypeReference<List<String>> typeRef = new ParameterizedTypeReference<List<String>>() {
        };
        ResponseEntity<List<String>> response = restTemplate.exchange(this.apiAccountSetting, HttpMethod.GET, null,
                                                                      typeRef);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void bCreateAccount() {
        Account newAccount;
        newAccount = new Account("email", "firstName", "lastName", "login", "password");

        ResponseEntity<Account> response = restTemplate.postForEntity(this.apiAccounts, newAccount, Account.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        ResponseEntity<Account> responseConflict = restTemplate.postForEntity(this.apiAccounts, newAccount,
                                                                              Account.class);
        assertEquals(HttpStatus.CONFLICT, responseConflict.getStatusCode());
    }

    @Test
    public void cGetAccount() {
        // make sure that Project with functional Identifier "name" is present.
        assertFalse(!this.serviceStub.existAccount("email"));

        ParameterizedTypeReference<Account> typeRef = new ParameterizedTypeReference<Account>() {
        };
        ResponseEntity<Account> response = restTemplate.exchange(this.apiAccountId, HttpMethod.GET, null, typeRef,
                                                                 "email");
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // make sure that Project with functional Identifier "msdfqmsdfqbndsjkqfmsdbqjkmfsdjkqfbkmfbjkmsdfqsdfmqbsdq"
        // doesn't exist
        assertFalse(this.serviceStub.existAccount("msdfqmsdfqbndsjkqfmsdbqjkmfsdjkqfbkmfbjkmsdfqsdfmqbsdq"));
        ResponseEntity<Account> responseNotFound = restTemplate
                .exchange(this.apiAccountId, HttpMethod.GET, null, typeRef,
                          "msdfqmsdfqbndsjkqfmsdbqjkmfsdjkqfbkmfbjkmsdfqsdfmqbsdq");
        assertEquals(HttpStatus.NOT_FOUND, responseNotFound.getStatusCode());

    }

    @Test
    public void dUpdateAccountSetting() {
        ParameterizedTypeReference<Void> typeRef = new ParameterizedTypeReference<Void>() {
        };
        HttpEntity<String> request = new HttpEntity<>("manual");
        ResponseEntity<Void> response = restTemplate.exchange(this.apiAccountSetting, HttpMethod.PUT, request, typeRef);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        request = new HttpEntity<>("auto-accept");
        response = restTemplate.exchange(this.apiAccountSetting, HttpMethod.PUT, request, typeRef);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        HttpEntity<String> invalidValueRequest = new HttpEntity<>("sdfhnqùlkhsdfq");
        ResponseEntity<Void> invalidValueResponse = restTemplate.exchange(this.apiAccountSetting, HttpMethod.PUT,
                                                                          invalidValueRequest, typeRef);
        assertEquals(HttpStatus.BAD_REQUEST, invalidValueResponse.getStatusCode());
    }

    @Test
    public void dUpdateAccount() {
        // make sure that Project with functional Identifier "name" is present.
        assertThat(this.serviceStub.existAccount("email"));
        Account updated = this.serviceStub.retrieveAccount("email");
        updated.setFirstName("AnOtherFirstName");
        ParameterizedTypeReference<Void> typeRef = new ParameterizedTypeReference<Void>() {
        };
        HttpEntity<Account> request = new HttpEntity<>(updated);
        ResponseEntity<Void> response = restTemplate.exchange(this.apiAccountId, HttpMethod.PUT, request, typeRef,
                                                              "email");
        // if that's the same functional ID and the parameter is valid:
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // if that's not the same functional ID and the parameter is valid:
        Account notSameID = new Account("notSameEmail", "firstName", "lastName", "login", "password");
        HttpEntity<Account> requestOperationNotAllowed = new HttpEntity<>(notSameID);
        ResponseEntity<Void> responseOperationNotAllowed = restTemplate
                .exchange(this.apiAccountId, HttpMethod.PUT, requestOperationNotAllowed, typeRef, "email");
        assertEquals(HttpStatus.BAD_REQUEST, responseOperationNotAllowed.getStatusCode());
    }

    @Test
    public void eDeleteProject() {
        ParameterizedTypeReference<Void> typeRef = new ParameterizedTypeReference<Void>() {
        };
        ResponseEntity<Void> response = restTemplate.exchange(this.apiAccountId, HttpMethod.DELETE, null, typeRef,
                                                              "email");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

}
