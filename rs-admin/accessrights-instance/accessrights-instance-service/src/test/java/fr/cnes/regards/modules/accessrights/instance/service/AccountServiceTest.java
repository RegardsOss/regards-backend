/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.instance.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.instance.dao.IAccountRepository;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountStatus;

/**
 * Test class for {@link AccountService}.
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

    private static final String PASSWORD_REGEX = "[a-z]+";

    private static final String PASSWORD_RULES = "At least one letter. Only small letters.";

    private static final String ROOT_PASSWORD = "rootPassword";

    private static final String ROOT_LOGIN = "rootLogin";

    private static final Long ACCOUNT_VALIDIDTY_DURATION = 2L;

    private static final Long PASSWORD_VALIDITY_DURATION = 1L;

    private static final Long FAILED_AUTHENTICATION_THRESHOLD = 2L;

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

        final IRuntimeTenantResolver resolver = Mockito.mock(IRuntimeTenantResolver.class);

        // Construct service with mock deps
        accountService = new AccountService(accountRepository, PASSWORD_REGEX, PASSWORD_RULES,
                PASSWORD_VALIDITY_DURATION, ACCOUNT_VALIDIDTY_DURATION, ROOT_LOGIN, ROOT_PASSWORD,
                FAILED_AUTHENTICATION_THRESHOLD, resolver);
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
        final Page<Account> mockedPage = new PageImpl<>(mockedResult);
        Mockito.when(accountRepository.findAll(Mockito.any(Pageable.class))).thenReturn(mockedPage);

        // Define actual
        final Page<Account> actual = accountService.retrieveAccountList(new PageRequest(0, 100));

        // Check equal
        Assert.assertEquals(actual.getContent().size(), expected.size());
        Assert.assertThat(actual.getContent().get(0), Matchers.samePropertyValuesAs(expected.get(0)));

        // Verify method call
        Mockito.verify(accountRepository).findAll(Mockito.any(Pageable.class));
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
        final Account fakeAccount = new Account("fake@toto.toto", "pFirstName", "pLastName", "pPassword");
        fakeAccount.setId(wrongId);

        // Mock
        Mockito.when(accountRepository.findOne(wrongId)).thenReturn(fakeAccount);
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

    @Test
    @Requirement("REGARDS_DSL_SYS_SEC_300")
    @Purpose("password has a complexity which is configurable by a regular expression")
    public void testValidPassword() {
        final String validPassword = "sdfnqnonhninsdfbzhvdvnqn";
        final String invalidPassword = "XCTGFU";
        Assert.assertTrue(accountService.validPassword(validPassword));
        Assert.assertFalse(accountService.validPassword(invalidPassword));
    }

    @Test
    @Ignore // FIXME: to be done in an integration test but i do not have the courage to do so right now. I don't
            // doubt!!!!
    @Requirement("REGARDS_DSL_SYS_SEC_410")
    @Requirement("REGARDS_DSL_SYS_SEC_310")
    @Purpose("Chack that the system can invalidate an account on the basis of its account validity duration or passowrd validity duration")
    public void testCheckAccountValidity() {

        final Account accountValid = new Account("valid@toto.toto", "pFirstName", "pLastName", "pPassword");
        accountValid.setInvalidityDate(LocalDateTime.now().plusDays(ACCOUNT_VALIDIDTY_DURATION));
        accountValid.setStatus(AccountStatus.ACTIVE);

        final Account accountInvalid = new Account("invalid@toto.toto", "pFirstName", "pLastName", "pPassword");
        accountInvalid.setInvalidityDate(LocalDateTime.now().minusDays(1L));
        accountInvalid.setStatus(AccountStatus.ACTIVE);

        final Account accountPasswordInvalid = new Account("passwordInvalid@toto.toto", "pFirstName", "pLastName",
                "pPassword");
        accountPasswordInvalid.setInvalidityDate(LocalDateTime.now().plusDays(ACCOUNT_VALIDIDTY_DURATION));
        // set the password validity date in two instruction so we are sure it is invalid without any headakes
        accountPasswordInvalid
                .setPasswordUpdateDate(LocalDateTime.now().minusDays(PASSWORD_VALIDITY_DURATION).minusDays(1L));
        accountPasswordInvalid.setStatus(AccountStatus.ACTIVE);
        final Set<Account> toCheck = Sets.newHashSet(accountValid, accountInvalid, accountPasswordInvalid);
        Mockito.when(accountRepository.findAllByStatusNot(AccountStatus.LOCKED)).thenReturn(toCheck);

        // lets test now that everything is in place
        accountService.checkAccountValidity();
        Assert.assertEquals(1L, toCheck.stream().filter(a -> a.getStatus().equals(AccountStatus.ACTIVE)).count());
        Assert.assertEquals(2L, toCheck.stream().filter(a -> a.getStatus().equals(AccountStatus.INACTIVE)).count());
        Assert.assertEquals(AccountStatus.INACTIVE, accountInvalid.getStatus());
        Assert.assertEquals(AccountStatus.INACTIVE, accountPasswordInvalid.getStatus());
    }

}
