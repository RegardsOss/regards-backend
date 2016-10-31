/**
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain.instance;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.regards.modules.accessrights.domain.AccountStatus;

/**
 *
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

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        status = AccountStatus.PENDING;
        accountTest = new Account(id, email, firstName, lastName, login, password, status, code);
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.instance.Account#Account()}.
     */
    @Test
    public void testAccount() {
        Account account = new Account();
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.instance.Account#Account(java.lang.String)}.
     */
    @Test
    public void testAccountEmail() {
        Account account = new Account(email);
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.instance.Account#Account(java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
     */
    @Test
    public void testAccountWithoutLogin() {
        Account account = new Account(email, firstName, lastName, password);
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.instance.Account#Account(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
     */
    @Test
    public void testAccountWithLogin() {
        Account account = new Account(email, firstName, lastName, login, password);
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.instance.Account#Account(java.lang.Long, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, fr.cnes.regards.modules.accessrights.domain.AccountStatus, java.lang.String)}.
     */
    @Test
    public void testAccountFull() {
        Account account = new Account(id, email, firstName, lastName, login, password, status, code);
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
        String newEmail = "newMail";
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
        String newFirstName = "newFirstName";
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
        String newLastName = "newLastName";
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
        String newLogin = "newLogin";
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
        String newPassword = "newPassword";
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
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.instance.Account#setStatus(fr.cnes.regards.modules.accessrights.domain.AccountStatus)}.
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
        Long newId = 4L;
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
        String newCode = "newCode";
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

        otherAccount = new Account(4L, "otherMail", "otherFirstName", "otherLastName", "otherLogin", "otherPassword",
                AccountStatus.INACTIVE, "otherCode");
        Assert.assertFalse(accountTest.equals(otherAccount));
    }

}
