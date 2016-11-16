/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import fr.cnes.regards.framework.module.rest.exception.AlreadyExistingException;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.InvalidValueException;
import fr.cnes.regards.framework.multitenant.autoconfigure.tenant.ITenantResolver;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.dao.instance.IAccountRepository;
import fr.cnes.regards.modules.accessrights.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.service.account.AccountService;
import fr.cnes.regards.modules.accessrights.service.account.AccountStateFactory;
import fr.cnes.regards.modules.accessrights.service.account.ActiveState;
import fr.cnes.regards.modules.accessrights.service.account.IAccountService;
import fr.cnes.regards.modules.accessrights.service.account.IAccountState;
import fr.cnes.regards.modules.accessrights.service.account.IllegalActionForAccountStatusException;
import fr.cnes.regards.modules.accessrights.service.account.LockedState;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;

/**
 * Test class for {@link IAccountState}.
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
     * Dummy password
     */
    private static final String PASSWORD = "password";

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
    private JWTService jwtService;

    /**
     * Mocked account state factory
     */
    private AccountStateFactory accountStateFactory;

    /**
     * Do some setup before each test
     */
    @Before
    public void setUp() {
        account = new Account(EMAIL, FIRST_NAME, LAST_NAME, PASSWORD);
        // Mock dependencies
        accountRepository = Mockito.mock(IAccountRepository.class);
        projectUserService = Mockito.mock(IProjectUserService.class);
        tenantResolver = Mockito.mock(ITenantResolver.class);
        jwtService = Mockito.mock(JWTService.class);
        accountStateFactory = Mockito.mock(AccountStateFactory.class);

        // Construct serivice with mock deps
        accountService = new AccountService(accountRepository, accountStateFactory);
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

    // /**
    // * Check that the system silently fails when trying to delete an not existing account.
    // *
    // * @throws EntityException
    // * Thrown if the {@link Account} is still linked to project users and therefore cannot be removed.
    // */
    // @Test
    // @Requirement("?")
    // @Purpose("Check that the system silently fails when trying to delete an not existing account.")
    // public void removeAccountNotFound() throws EntityException {
    // // Prepare the case
    // account.setStatus(AccountStatus.ACTIVE);
    //
    // // Mock
    // Mockito.when(accountRepository.findOne(ID)).thenReturn(null);
    // Mockito.when(accountStateFactory.createState(account))
    // .thenReturn(new ActiveState(projectUserService, accountRepository, jwtService, tenantResolver));
    //
    // // Call the tested method
    // accountService.delete(account);
    //
    // // Verify deps calls
    // Mockito.verify(accountRepository).findOne(ID);
    // }

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
        // Prepare the case
        account.setId(ID);
        account.setStatus(AccountStatus.ACTIVE);

        // Mock
        Mockito.when(accountRepository.findOne(ID)).thenReturn(account);
        Mockito.when(tenantResolver.getAllTenants()).thenReturn(TENANTS);
        Mockito.when(projectUserService.existUser(EMAIL)).thenReturn(true);
        Mockito.when(accountStateFactory.createState(account))
                .thenReturn(new ActiveState(projectUserService, accountRepository, jwtService, tenantResolver));

        // Trigger the exception
        accountService.delete(account);
    }

    /**
     * Check that the system prevents from deleting an account for certain status (ACCEPTED...).
     *
     * @throws EntityException
     *             Thrown if the {@link Account} is still linked to project users and therefore cannot be removed.
     */
    @Test(expected = IllegalActionForAccountStatusException.class)
    @Requirement("?")
    @Purpose("Check that the system prevents from deleting an account for certain status (ACCEPTED...).")
    public void removeAccountWrongStatus() throws EntityException {
        // Mock
        Mockito.when(accountRepository.findOne(ID)).thenReturn(account);
        Mockito.when(tenantResolver.getAllTenants()).thenReturn(TENANTS);
        Mockito.when(projectUserService.existUser(EMAIL)).thenReturn(false);
        Mockito.when(accountStateFactory.createState(account))
                .thenReturn(new ActiveState(projectUserService, accountRepository, jwtService, tenantResolver));

        // Prepare the case
        account.setId(ID);
        account.setStatus(AccountStatus.ACCEPTED);

        // Trigger the exception
        accountService.delete(account);
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
        // Prepare the case
        account.setId(ID);
        account.setStatus(AccountStatus.ACTIVE);

        // Mock
        Mockito.when(accountRepository.findOne(ID)).thenReturn(account);
        Mockito.when(tenantResolver.getAllTenants()).thenReturn(TENANTS);
        Mockito.when(projectUserService.existUser(EMAIL)).thenReturn(false);
        Mockito.when(accountStateFactory.createState(account))
                .thenReturn(new ActiveState(projectUserService, accountRepository, jwtService, tenantResolver));

        // Call the method
        accountService.delete(account);

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
        Mockito.verify(accountRepository).save(Mockito.refEq(account, "id", "status", "code"));
    }

    /**
     * Check that the system fails when trying to update a not existing account
     *
     * @throws EntityNotFoundException
     *             Thrown when no {@link Account} with passed if could be found
     * @throws InvalidValueException
     *             Thrown when passed id is different from the id of passed account
     */
    @Test(expected = EntityNotFoundException.class)
    @Requirement("?")
    @Purpose("Check that the system allows to create a new account.")
    public void updateAccountNotFound() throws EntityNotFoundException, InvalidValueException {
        // Prepare account
        account.setId(ID);

        // Mock
        Mockito.when(accountRepository.exists(ID)).thenReturn(false);

        // Trigger the exception
        accountService.updateAccount(ID, account);
    }

    /**
     * Check that the system fails when trying to update a account with different id thant the passed one.
     *
     * @throws EntityNotFoundException
     *             Thrown when no {@link Account} with passed if could be found
     * @throws InvalidValueException
     *             Thrown when passed id is different from the id of passed account
     */
    @Test(expected = InvalidValueException.class)
    @Requirement("?")
    @Purpose("Check that the system fails when trying to update a account with different id thant the passed one.")
    public void updateAccountDifferentId() throws EntityNotFoundException, InvalidValueException {
        // Prepare the account
        account.setId(ID);

        // Define a wrong id
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
     * @throws EntityNotFoundException
     *             Thrown when no {@link Account} with passed if could be found
     * @throws InvalidValueException
     *             Thrown when passed id is different from the id of passed account
     */
    @Test
    @Requirement("?")
    @Purpose("Check that the system allows to update an account.")
    public void updateAccount() throws EntityNotFoundException, InvalidValueException {
        // Prepare the case
        account.setId(ID);
        account.setFirstName("Newfirstname");

        // Mock
        Mockito.when(accountRepository.exists(ID)).thenReturn(true);
        Mockito.when(accountRepository.findOne(ID)).thenReturn(account);

        // Call tested method
        accountService.updateAccount(ID, account);

        // Check the repository was called to save
        Mockito.verify(accountRepository).save(Mockito.refEq(account));
    }

    /**
     * Check that the system does unlock not locked accounts and feedbacks the caller.
     *
     * @throws EntityNotFoundException
     *             Thrown when no {@link Account} with passed if could be found
     * @throws InvalidValueException
     *             Thrown when passed id is different from the id of passed account
     * @throws IllegalActionForAccountStatusException
     *             Thrown when the account is not of status LOCKED
     */
    @Test(expected = IllegalActionForAccountStatusException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_450")
    @Purpose("Check that the system does unlock not locked accounts and feedbacks the caller.")
    public void unlockAccountNotLocked()
            throws EntityNotFoundException, InvalidValueException, IllegalActionForAccountStatusException {
        // Prepare the error case
        account.setStatus(AccountStatus.ACTIVE);

        // Mock
        Mockito.when(accountRepository.exists(ID)).thenReturn(true);
        Mockito.when(accountRepository.findOne(ID)).thenReturn(account);
        Mockito.when(accountStateFactory.createState(account))
                .thenReturn(new ActiveState(projectUserService, accountRepository, jwtService, tenantResolver));

        // Trigger exception
        accountService.unlockAccount(account, CODE);
    }

    /**
     * Check that the system does not unlock a locked account if the wrong code is passed.
     *
     * @throws EntityNotFoundException
     *             Thrown when no {@link Account} with passed if could be found
     * @throws InvalidValueException
     *             Thrown when passed id is different from the id of passed account
     * @throws IllegalActionForAccountStatusException
     *             Thrown when the account is not of status LOCKED
     */
    @Test(expected = InvalidValueException.class)
    @Requirement("REGARDS_DSL_ADM_ADM_450")
    @Purpose("Check that the system does not unlock a locked account if the wrong code is passed.")
    public void unlockAccountWrongCode()
            throws EntityNotFoundException, InvalidValueException, IllegalActionForAccountStatusException {
        // Mock
        Mockito.when(accountRepository.exists(ID)).thenReturn(true);
        Mockito.when(accountRepository.findOne(ID)).thenReturn(account);
        Mockito.when(accountStateFactory.createState(account))
                .thenReturn(new LockedState(projectUserService, accountRepository, jwtService, tenantResolver));

        // Prepare the case
        account.setStatus(AccountStatus.LOCKED);

        // Trigger exception
        accountService.unlockAccount(account, "wrongCode");
    }

    /**
     * Check that the system allows a user to unlock its account with a code.
     *
     * @throws EntityNotFoundException
     *             Thrown when no {@link Account} with passed if could be found
     * @throws InvalidValueException
     *             Thrown when passed id is different from the id of passed account
     * @throws IllegalActionForAccountStatusException
     *             Thrown when the account is not of status LOCKED
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_450")
    @Purpose("Check that the system allows a user to unlock its account with a code.")
    public void unlockAccountRightCode()
            throws EntityNotFoundException, InvalidValueException, IllegalActionForAccountStatusException {
        // Mock
        Mockito.when(accountRepository.exists(ID)).thenReturn(true);
        Mockito.when(accountRepository.findOne(ID)).thenReturn(account);
        Mockito.when(accountStateFactory.createState(account))
                .thenReturn(new LockedState(projectUserService, accountRepository, jwtService, tenantResolver));

        // Prepare the case
        account.setStatus(AccountStatus.LOCKED);
        account.setCode(CODE);

        // Call tested method
        accountService.unlockAccount(account, CODE);

        // Check
        final Account actual = accountService.retrieveAccount(ID);
        Assert.assertEquals(AccountStatus.ACTIVE, actual.getStatus());
        Mockito.verify(accountRepository).save(Mockito.refEq(account));
    }
}
