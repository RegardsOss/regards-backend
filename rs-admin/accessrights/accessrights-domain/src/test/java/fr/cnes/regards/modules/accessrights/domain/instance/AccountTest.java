/**
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain.instance;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.regards.modules.accessrights.domain.AccountStatus;

/**
 * Unit test for {@link Account}
 *
 * @author Maxime Bouveron
 */
public class AccountTest {

    /**
     * Test account
     */
    private Account accountTest;

    /**
     * Test id
     */
    private final Long id = 0L;

    /**
     * Test email
     */
    private final String email = "mail";

    /**
     * Test firstName
     */
    private final String firstName = "firstname";

    /**
     * Test lastName
     */
    private final String lastName = "lastname";

    /**
     * Test login
     */
    private final String login = "login";

    /**
     * Test password
     */
    private final String password = "password";

    /**
     * Test status
     */
    private AccountStatus status;

    /**
     * Test code
     */
    private final String code = "code";

    @Before
    public void setUp() {
        status = AccountStatus.PENDING;
        accountTest = new Account(id, email, firstName, lastName, login, password, status, code);
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.instance.Account#Account()}.
     */
    @Test
    public void testAccount() {
        final Account account = new Account();

        Assert.assertEquals(null, account.getId());
        Assert.assertEquals(null, account.getEmail());
        Assert.assertEquals(null, account.getFirstName());
        Assert.assertEquals(null, account.getLastName());
        Assert.assertEquals(null, account.getLogin());
        Assert.assertEquals(null, account.getPassword());
        Assert.assertEquals(AccountStatus.PENDING, account.getStatus());
        Assert.assertEquals(null, account.getCode());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.instance.Account#Account(java.lang.String)}.
     */
    @Test
    public void testAccountEmail() {
        final Account account = new Account(email);

        Assert.assertEquals(email, account.getEmail());
        Assert.assertEquals(null, account.getFirstName());
        Assert.assertEquals(null, account.getLastName());
        Assert.assertEquals(email, account.getLogin());
        Assert.assertEquals(null, account.getPassword());
        Assert.assertEquals(AccountStatus.PENDING, account.getStatus());
        Assert.assertEquals(null, account.getCode());
    }

    /**
     * Test method for {@link Account#Account(java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
     */
    @Test
    public void testAccountWithoutLogin() {
        final Account account = new Account(email, firstName, lastName, password);

        Assert.assertEquals(email, account.getEmail());
        Assert.assertEquals(firstName, account.getFirstName());
        Assert.assertEquals(lastName, account.getLastName());
        Assert.assertEquals(email, account.getLogin());
        Assert.assertEquals(password, account.getPassword());
        Assert.assertEquals(AccountStatus.PENDING, account.getStatus());
        Assert.assertEquals(null, account.getCode());
    }

    /**
     * Test method for
     * {@link Account#Account(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}
     */
    @Test
    public void testAccountWithLogin() {
        final Account account = new Account(email, firstName, lastName, login, password);

        Assert.assertEquals(email, account.getEmail());
        Assert.assertEquals(firstName, account.getFirstName());
        Assert.assertEquals(lastName, account.getLastName());
        Assert.assertEquals(login, account.getLogin());
        Assert.assertEquals(password, account.getPassword());
        Assert.assertEquals(AccountStatus.PENDING, account.getStatus());
        Assert.assertEquals(null, account.getCode());
    }

    /**
     * Test method for {@link Account#Account(Long, String, String, String, String, jString, AccountStatus, String)}.
     */
    @Test
    public void testAccountFull() {
        final Account account = new Account(id, email, firstName, lastName, login, password, status, code);

        Assert.assertEquals(id, account.getId());
        Assert.assertEquals(email, account.getEmail());
        Assert.assertEquals(firstName, account.getFirstName());
        Assert.assertEquals(lastName, account.getLastName());
        Assert.assertEquals(login, account.getLogin());
        Assert.assertEquals(password, account.getPassword());
        Assert.assertEquals(AccountStatus.PENDING, account.getStatus());
        Assert.assertEquals(code, account.getCode());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.instance.Account#getEmail()}.
     */
    @Test
    public void testGetEmail() {
        Assert.assertEquals(email, accountTest.getEmail());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.instance.Account#setEmail(java.lang.String)}.
     */
    @Test
    public void testSetEmail() {
        final String newEmail = "newMail";
        accountTest.setEmail(newEmail);
        Assert.assertEquals(newEmail, accountTest.getEmail());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.instance.Account#getFirstName()}.
     */
    @Test
    public void testGetFirstName() {
        Assert.assertEquals(firstName, accountTest.getFirstName());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.instance.Account#setFirstName(java.lang.String)}.
     */
    @Test
    public void testSetFirstName() {
        final String newFirstName = "newFirstName";
        accountTest.setFirstName(newFirstName);
        Assert.assertEquals(newFirstName, accountTest.getFirstName());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.instance.Account#getLastName()}.
     */
    @Test
    public void testGetLastName() {
        Assert.assertEquals(lastName, accountTest.getLastName());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.instance.Account#setLastName(java.lang.String)}.
     */
    @Test
    public void testSetLastName() {
        final String newLastName = "newLastName";
        accountTest.setLastName(newLastName);
        Assert.assertEquals(newLastName, accountTest.getLastName());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.instance.Account#getLogin()}.
     */
    @Test
    public void testGetLogin() {
        Assert.assertEquals(login, accountTest.getLogin());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.instance.Account#setLogin(java.lang.String)}.
     */
    @Test
    public void testSetLogin() {
        final String newLogin = "newLogin";
        accountTest.setLogin(newLogin);
        Assert.assertEquals(newLogin, accountTest.getLogin());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.instance.Account#getPassword()}.
     */
    @Test
    public void testGetPassword() {
        Assert.assertEquals(password, accountTest.getPassword());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.instance.Account#setPassword(java.lang.String)}.
     */
    @Test
    public void testSetPassword() {
        final String newPassword = "newPassword";
        accountTest.setPassword(newPassword);
        Assert.assertEquals(newPassword, accountTest.getPassword());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.instance.Account#getStatus()}.
     */
    @Test
    public void testGetStatus() {
        Assert.assertEquals(status, accountTest.getStatus());
    }

    /**
     * Test method for {@link Account#setStatus(fr.cnes.regards.modules.accessrights.domain.AccountStatus)}.
     */
    @Test
    public void testSetStatus() {
        accountTest.setStatus(AccountStatus.ACTIVE);
        Assert.assertEquals(AccountStatus.ACTIVE, accountTest.getStatus());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.instance.Account#unlock()}.
     */
    @Test
    public void testUnlock() {
        // Test if Account Status is unlocked
        accountTest.unlock();
        Assert.assertEquals(AccountStatus.PENDING, accountTest.getStatus());

        // Test if Account Status is locked
        accountTest.setStatus(AccountStatus.LOCKED);
        accountTest.unlock();
        Assert.assertEquals(AccountStatus.ACTIVE, accountTest.getStatus());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.instance.Account#getId()}.
     */
    @Test
    public void testGetId() {
        Assert.assertEquals(id, accountTest.getId());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.instance.Account#setId(java.lang.Long)}.
     */
    @Test
    public void testSetId() {
        final Long newId = 4L;
        accountTest.setId(newId);
        Assert.assertEquals(newId, accountTest.getId());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.instance.Account#getCode()}.
     */
    @Test
    public void testGetCode() {
        Assert.assertEquals(code, accountTest.getCode());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.instance.Account#setCode(java.lang.String)}.
     */
    @Test
    public void testSetCode() {
        final String newCode = "newCode";
        accountTest.setCode(newCode);
        Assert.assertEquals(newCode, accountTest.getCode());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.instance.Account#equals(java.lang.Object)}.
     */
    @Test
    public void testEqualsObject() {
        Account otherAccount = new Account(id, email, firstName, lastName, login, password, status, code);
        Assert.assertTrue(accountTest.equals(otherAccount));

        final Long localId = 4L;
        otherAccount = new Account(localId, "otherMail", "otherFirstName", "otherLastName", "otherLogin",
                "otherPassword", AccountStatus.INACTIVE, "otherCode");
        Assert.assertFalse(accountTest.equals(otherAccount));
    }

}
