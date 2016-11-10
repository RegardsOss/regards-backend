/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.module.rest.exception.AlreadyExistingException;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.InvalidValueException;
import fr.cnes.regards.framework.module.rest.exception.ModuleEntityNotFoundException;
import fr.cnes.regards.framework.multitenant.autoconfigure.tenant.ITenantResolver;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.dao.instance.IAccountRepository;
import fr.cnes.regards.modules.accessrights.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.service.AccountService;
import fr.cnes.regards.modules.accessrights.service.IAccountService;
import fr.cnes.regards.modules.accessrights.service.IProjectUserService;

/**
 * Test class for {@link IAccountService}.
 *
 * @author Xavier-Alexandre Brochard
 */
public class AccountServiceTest {

    /**
     * A dummy account
     */
    private static Account account;

    /**
     * Dummy id
     */
    private static final Long ID = 0L;

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
     * Dummy login
     */
    private static final String LOGIN = "login";

    /**
     * Dummy password
     */
    private static final String PASSWORD = "password";

    /**
     * Dummy account status
     */
    private static final AccountStatus STATUS = AccountStatus.ACCEPTED;

    /**
     * Dummy unlock code
     */
    private static final String CODE = "code";

    /**
     * Dummy tenants
     */
    private static final Set<String> TENANTS = new HashSet<>(Arrays.asList("tenant0", "tenant1"));

    /**
     * Tested service
     */
    private IAccountService accountService;

    /**
     * Mock repository
     */
    private IAccountRepository accountRepository;

    /**
     * Mocked service managing {@link ProjectUser}s
     */
    private IProjectUserService projectUserService;

    /**
     * Mocked tenant resolver
     */
    private ITenantResolver tenantResolver;

    /**
     * Mocked JWT Service
     */
    @Autowired
    private JWTService jwtService;

    /**
     * Do some setup before each test
     */
    @Before
    public void setUp() {
        account = new Account(ID, EMAIL, FIRST_NAME, LAST_NAME, LOGIN, PASSWORD, STATUS, CODE);
        // Mock dependencies
        accountRepository = Mockito.mock(IAccountRepository.class);
        projectUserService = Mockito.mock(IProjectUserService.class);
        tenantResolver = Mockito.mock(ITenantResolver.class);
        jwtService = Mockito.mock(JWTService.class);
        // Construct serivice with mock deps
        accountService = new AccountService(accountRepository, projectUserService, tenantResolver, jwtService);
    }

    /**
     * Check that the system allows to retrieve all accounts of an instance.
     */
    @Test
    @Requirement("?")
    @Purpose("Check that the system allows to retrieve all accounts of an instance.")
    public void retrieveAccountList() {
        // Define expected
        final List<Account> expected = Arrays.asList(account);

        // Mock
        final List<Account> mockedResult = Arrays.asList(account);
        Mockito.when(accountRepository.findAll()).thenReturn(mockedResult);

        // Define actual
        final List<Account> actual = accountService.retrieveAccountList();

        // Check equal
        Assert.assertThat(actual, Matchers.samePropertyValuesAs(expected));

        // Verify method call
        Mockito.verify(accountRepository).findAll();
    }

    /**
     * Check that the system silently fails when trying to delete an not existing account.
     *
     * @throws EntityException
     *             Thrown if the {@link Account} is still linked to project users and therefore cannot be removed.
     */
    @Test
    @Requirement("?")
    @Purpose("Check that the system silently fails when trying to delete an not existing account.")
    public void removeAccountNotFound() throws EntityException {
        // Mock
        Mockito.when(accountRepository.findOne(ID)).thenReturn(null);

        // Call the tested method
        accountService.removeAccount(ID);

        // Verify deps calls
        Mockito.verify(accountRepository).findOne(ID);
    }

    /**
     * Check that the system prevents from deleting an account if it is still linked to project users.
     *
     * @throws EntityException
     *             Thrown if the {@link Account} is still linked to project users and therefore cannot be removed.
     */
    @Test(expected = EntityException.class)
    @Requirement("?")
    @Purpose("Check that the system prevents from deleting an account if it is still linked to project users.")
    public void removeAccountUndeletable() throws EntityException {
        // Mock
        Mockito.when(accountRepository.findOne(ID)).thenReturn(account);
        Mockito.when(tenantResolver.getAllTenants()).thenReturn(TENANTS);
        Mockito.when(projectUserService.existUser(EMAIL)).thenReturn(false);

        // Trigger the exception
        accountService.removeAccount(ID);
    }

    /**
     * Check that the system allows to delete an account.
     *
     * @throws EntityException
     *             Thrown if the {@link Account} is still linked to project users and therefore cannot be removed.
     */
    @Test
    @Requirement("?")
    @Purpose("Check that the system allows to delete an account.")
    public void removeAccount() throws EntityException {
        // Mock
        Mockito.when(accountRepository.findOne(ID)).thenReturn(account);
        Mockito.when(tenantResolver.getAllTenants()).thenReturn(TENANTS);
        Mockito.when(projectUserService.existUser(EMAIL)).thenReturn(true);

        // Call the method
        accountService.removeAccount(ID);

        // Verify the repository was correctly called
        Mockito.verify(accountRepository).delete(ID);
    }

    /**
     * Check that the system fails when trying to create an account of already existing email.
     *
     * @throws AlreadyExistingException
     *             Thrown when an account with same email already exists
     */
    @Test(expected = AlreadyExistingException.class)
    @Requirement("?")
    @Purpose("Check that the system fails when trying to create an account of already existing email.")
    public void createAccountAlreadyExisting() throws AlreadyExistingException {
        // Mock
        Mockito.when(accountRepository.findOneByEmail(EMAIL)).thenReturn(account);

        // Trigger the exception
        accountService.createAccount(account);
    }

    /**
     * Check that the system allows to create a new account.
     *
     * @throws AlreadyExistingException
     *             Thrown when an account with same email already exists
     */
    @Test
    @Requirement("?")
    @Purpose("Check that the system allows to create a new account.")
    public void createAccount() throws AlreadyExistingException {
        // Mock
        Mockito.when(accountRepository.findOneByEmail(EMAIL)).thenReturn(null);

        // Call tested method
        accountService.createAccount(account);

        // Verify the repository was correctly called
        Mockito.verify(accountRepository).save(Mockito.refEq(account));
    }

    /**
     * Check that the system fails when trying to update a not existing account
     *
     * @throws ModuleEntityNotFoundException
     *             Thrown when no {@link Account} with passed if could be found
     * @throws InvalidValueException
     *             Thrown when passed id is different from the id of passed account
     */
    @Test(expected = ModuleEntityNotFoundException.class)
    @Requirement("?")
    @Purpose("Check that the system allows to create a new account.")
    public void updateAccountNotFound() throws ModuleEntityNotFoundException, InvalidValueException {
        // Mock
        Mockito.when(accountRepository.exists(ID)).thenReturn(false);

        // Trigger the exception
        accountService.updateAccount(ID, account);
    }

    /**
     * Check that the system fails when trying to update a account with different id thant the passed one.
     *
     * @throws ModuleEntityNotFoundException
     *             Thrown when no {@link Account} with passed if could be found
     * @throws InvalidValueException
     *             Thrown when passed id is different from the id of passed account
     */
    @Test(expected = InvalidValueException.class)
    @Requirement("?")
    @Purpose("Check that the system fails when trying to update a account with different id thant the passed one.")
    public void updateAccountDifferentId() throws ModuleEntityNotFoundException, InvalidValueException {
        final Long wrongId = 1L;

        // Mock
        Mockito.when(accountRepository.exists(wrongId)).thenReturn(true);
        Mockito.when(accountRepository.exists(ID)).thenReturn(true);

        // Trigger exception
        accountService.updateAccount(1L, account);
    }

    /**
     * Check that the system allows to update an account.
     *
     * @throws ModuleEntityNotFoundException
     *             Thrown when no {@link Account} with passed if could be found
     * @throws InvalidValueException
     *             Thrown when passed id is different from the id of passed account
     */
    @Test
    @Requirement("?")
    @Purpose("Check that the system allows to update an account.")
    public void updateAccount() throws ModuleEntityNotFoundException, InvalidValueException {
        // Mock
        Mockito.when(accountRepository.exists(ID)).thenReturn(true);
        Mockito.when(accountRepository.findOne(ID)).thenReturn(account);
        account.setCode("newCode");

        // Call tested method
        accountService.updateAccount(ID, account);

        // Check the repository was called to save
        Mockito.verify(accountRepository).save(Mockito.refEq(account));
    }

    /**
     * Check that the system does unlock not locked accounts and feedbacks the caller.
     *
     * @throws ModuleEntityNotFoundException
     *             Thrown when no {@link Account} with passed if could be found
     * @throws InvalidValueException
     *             Thrown when passed id is different from the id of passed account
     */
    @Test(expected = InvalidValueException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_450")
    @Purpose("Check that the system does unlock not locked accounts and feedbacks the caller.")
    public void unlockAccountNotLocked() throws ModuleEntityNotFoundException, InvalidValueException {
        // Mock
        Mockito.when(accountRepository.exists(ID)).thenReturn(true);
        Mockito.when(accountRepository.findOne(ID)).thenReturn(account);

        // Prepare the error case
        account.setStatus(AccountStatus.ACTIVE);

        // Trigger exception
        accountService.unlockAccount(ID, CODE);
    }

    /**
     * Check that the system does not unlock a locked account if the wrong code is passed.
     *
     * @throws ModuleEntityNotFoundException
     *             Thrown when no {@link Account} with passed if could be found
     * @throws InvalidValueException
     *             Thrown when passed id is different from the id of passed account
     */
    @Test(expected = InvalidValueException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_450")
    @Purpose("Check that the system does not unlock a locked account if the wrong code is passed.")
    public void unlockAccountWrongCode() throws ModuleEntityNotFoundException, InvalidValueException {
        // Mock
        Mockito.when(accountRepository.exists(ID)).thenReturn(true);
        Mockito.when(accountRepository.findOne(ID)).thenReturn(account);

        // Trigger exception
        accountService.unlockAccount(ID, "wrongCode");
    }

    /**
     * Check that the system allows a user to unlock its account with a code.
     *
     * @throws ModuleEntityNotFoundException
     *             Thrown when no {@link Account} with passed if could be found
     * @throws InvalidValueException
     *             Thrown when passed id is different from the id of passed account
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_450")
    @Purpose("Check that the system allows a user to unlock its account with a code.")
    public void unlockAccountRightCode() throws ModuleEntityNotFoundException, InvalidValueException {
        // Mock
        Mockito.when(accountRepository.exists(ID)).thenReturn(true);
        Mockito.when(accountRepository.findOne(ID)).thenReturn(account);

        // Prepare the case
        account.setStatus(AccountStatus.LOCKED);

        // Call tested method
        accountService.unlockAccount(ID, CODE);

        // Check
        final Account actual = accountService.retrieveAccount(ID);
        Assert.assertEquals(AccountStatus.ACTIVE, actual.getStatus());
        Mockito.verify(accountRepository).save(Mockito.refEq(account));
    }
}
