/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service;

import java.util.Arrays;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.accessrights.dao.instance.IAccountRepository;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.service.account.AccountService;
import fr.cnes.regards.modules.accessrights.service.account.IAccountService;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserTransitions;

/**
 * Test class for {@link IProjectUserTransitions}.
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
     * Tested service
     */
    private IAccountService accountService;

    /**
     * Mock repository
     */
    private IAccountRepository accountRepository;

    /**
     * Do some setup before each test
     */
    @Before
    public void setUp() {
        account = new Account(EMAIL, FIRST_NAME, LAST_NAME, PASSWORD);
        // Mock dependencies
        accountRepository = Mockito.mock(IAccountRepository.class);

        // Construct serivice with mock deps
        accountService = new AccountService(accountRepository);
    }

    /**
     * Check that the system allows to retrieve all accounts of an instance.
     */
    @Test
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
     * Check that the system fails when trying to update a not existing account
     *
     * @throws EntityException
     *             <br>
     *             {@link EntityNotFoundException} Thrown when no {@link Account} with passed if could be found<br>
     *             {@link EntityInconsistentIdentifierException} Thrown when passed id is different from the id of
     *             passed account<br>
     */
    @Test(expected = EntityNotFoundException.class)
    @Purpose("Check that the system allows to create a new account.")
    public void updateAccountNotFound() throws EntityException {
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
     * @throws EntityException
     *             <br>
     *             {@link EntityNotFoundException} Thrown when no {@link Account} with passed if could be found<br>
     *             {@link EntityInconsistentIdentifierException} Thrown when passed id is different from the id of
     *             passed account<br>
     */
    @Test(expected = EntityInconsistentIdentifierException.class)
    @Purpose("Check that the system fails when trying to update a account with different id thant the passed one.")
    public void updateAccountDifferentId() throws EntityException {
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
     * @throws EntityException
     *             <br>
     *             {@link EntityNotFoundException} Thrown when no {@link Account} with passed if could be found<br>
     *             {@link EntityInconsistentIdentifierException} Thrown when passed id is different from the id of
     *             passed account<br>
     */
    @Test
    @Purpose("Check that the system allows to update an account.")
    public void updateAccount() throws EntityException {
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
}
