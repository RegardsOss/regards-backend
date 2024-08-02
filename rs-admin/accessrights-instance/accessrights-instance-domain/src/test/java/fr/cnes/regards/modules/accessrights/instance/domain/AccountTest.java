/**
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 * <p>
 * This file is part of REGARDS.
 * <p>
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.accessrights.instance.domain;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for {@link Account}
 *
 * @author Maxime Bouveron
 */
public class AccountTest {

    /**
     * Test email
     */
    private final String email = "mail@cnes.fr";

    /**
     * Test firstName
     */
    private final String firstName = "firstname";

    /**
     * Test lastName
     */
    private final String lastName = "lastname";

    /**
     * Test password
     */
    private final String password = "password";

    /**
     * Test account
     */
    private Account accountTest;

    /**
     * Test status
     */
    private AccountStatus status;

    @Before
    public void setUp() {
        status = AccountStatus.PENDING;
        accountTest = new Account(email, firstName, lastName, password);
    }

    /**
     * Test method for {@link Account#Account(String, String, String, String)}.
     */
    @Test
    public void testAccountFull() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        final Account account = new Account(email, firstName, lastName, password);

        System.out.println(account.getEmail().length());
        validator.validate(account).forEach(c -> System.out.println(c.getMessage()));

        Assert.assertTrue(validator.validate(account).isEmpty());

        Assert.assertNull(account.getId());
        Assert.assertEquals(email, account.getEmail());
        Assert.assertEquals(firstName, account.getFirstName());
        Assert.assertEquals(lastName, account.getLastName());
        Assert.assertEquals(password, account.getPassword());
        Assert.assertEquals(AccountStatus.PENDING, account.getStatus());
    }

    /**
     * Test method for {@link Account#getEmail()}.
     */
    @Test
    public void testGetEmail() {
        Assert.assertEquals(email, accountTest.getEmail());
    }

    /**
     * Test method for {@link Account#setEmail(java.lang.String)}.
     */
    @Test
    public void testSetEmail() {
        final String newEmail = "newMail";
        accountTest.setEmail(newEmail);
        Assert.assertEquals(newEmail, accountTest.getEmail());
    }

    /**
     * Test method for {@link Account#getFirstName()}.
     */
    @Test
    public void testGetFirstName() {
        Assert.assertEquals(firstName, accountTest.getFirstName());
    }

    /**
     * Test method for
     * {@link Account#setFirstName(java.lang.String)}.
     */
    @Test
    public void testSetFirstName() {
        final String newFirstName = "newFirstName";
        accountTest.setFirstName(newFirstName);
        Assert.assertEquals(newFirstName, accountTest.getFirstName());
    }

    /**
     * Test method for {@link Account#getLastName()}.
     */
    @Test
    public void testGetLastName() {
        Assert.assertEquals(lastName, accountTest.getLastName());
    }

    /**
     * Test method for
     * {@link Account#setLastName(java.lang.String)}.
     */
    @Test
    public void testSetLastName() {
        final String newLastName = "newLastName";
        accountTest.setLastName(newLastName);
        Assert.assertEquals(newLastName, accountTest.getLastName());
    }

    /**
     * Test method for {@link Account#getPassword()}.
     */
    @Test
    public void testGetPassword() {
        Assert.assertEquals(password, accountTest.getPassword());
    }

    /**
     * Test method for
     * {@link Account#setPassword(java.lang.String)}.
     */
    @Test
    public void testSetPassword() {
        final String newPassword = "newPassword";
        accountTest.setPassword(newPassword);
        Assert.assertEquals(newPassword, accountTest.getPassword());
    }

    /**
     * Test method for {@link Account#getStatus()}.
     */
    @Test
    public void testGetStatus() {
        Assert.assertEquals(status, accountTest.getStatus());
    }

    /**
     * Test method for {@link Account#setStatus(AccountStatus)}.
     */
    @Test
    public void testSetStatus() {
        accountTest.setStatus(AccountStatus.ACTIVE);
        Assert.assertEquals(AccountStatus.ACTIVE, accountTest.getStatus());
    }

    /**
     * Test method for {@link Account#getId()}.
     */
    @Test
    public void testGetId() {
        Assert.assertNull(accountTest.getId());
    }

    /**
     * Test method for {@link Account#setId(java.lang.Long)}.
     */
    @Test
    public void testSetId() {
        final Long newId = 4L;
        accountTest.setId(newId);
        Assert.assertEquals(newId, accountTest.getId());
    }

    /**
     * Test method for {@link Account#equals(java.lang.Object)}.
     */
    @Test
    public void testEqualsObject() {
        Account otherAccount = new Account(email, "otherFirstName", "otherLastName", "otherPassword");
        Assert.assertEquals(accountTest, otherAccount);

        otherAccount = new Account("otherMail", firstName, lastName, password);
        Assert.assertNotEquals(accountTest, otherAccount);
    }

}
