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
package fr.cnes.regards.modules.accessrights.domain;

import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit test for {@link AccessRequestDto}
 *
 * @author Maxime Bouveron
 */
public class AccessRequestDTOTest {

    /**
     * Test PerformResetPasswordDto
     */
    private AccessRequestDto access;

    /**
     * Test email
     */
    private final String email = "email";

    /**
     * Test first name
     */
    private final String firstName = "firstName";

    /**
     * Test last name
     */
    private final String lastName = "lastName";

    /**
     * Test MetaData
     */
    private final List<MetaData> metaDatas = new ArrayList<>();

    /**
     * Test password
     */
    private final String password = "password";

    /**
     * The origin url
     */
    private String originUrl;

    /**
     * The request link
     */
    private String requestLink;

    /**
     * Setup
     */
    @Before
    public void setUp() {
        access = new AccessRequestDto(email,
                                      firstName,
                                      lastName,
                                      null,
                                      metaDatas,
                                      password,
                                      originUrl,
                                      requestLink,
                                      null,
                                      null,
                                      0L);
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto#getEmail()}.
     */
    @Test
    public void testGetEmail() {
        Assert.assertEquals(email, access.getEmail());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto#getFirstName()}.
     */
    @Test
    public void testGetFirstName() {
        Assert.assertEquals(firstName, access.getFirstName());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto#getLastName()}.
     */
    @Test
    public void testGetLastName() {
        Assert.assertEquals(lastName, access.getLastName());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto#getMetadata()}.
     */
    @Test
    public void testGetMetaData() {
        Assert.assertEquals(metaDatas, access.getMetadata());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto#getPassword()}.
     */
    @Test
    public void testGetPassword() {
        Assert.assertEquals(password, access.getPassword());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto#setEmail(java.lang.String)}.
     */
    @Test
    public void testSetEmail() {
        final String newEmail = "newEmail";
        access.setEmail(newEmail);
        Assert.assertEquals(newEmail, access.getEmail());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto#setFirstName(java.lang.String)}.
     */
    @Test
    public void testSetFirstName() {
        final String newFirstName = "newFirstName";
        access.setFirstName(newFirstName);
        Assert.assertEquals(newFirstName, access.getFirstName());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto#setLastName(java.lang.String)}.
     */
    @Test
    public void testSetLastName() {
        final String newLastName = "newLastName";
        access.setLastName(newLastName);
        Assert.assertEquals(newLastName, access.getLastName());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto#setMetadata(java.util.List)}.
     */
    @Test
    public void testSetMetaData() {
        final List<MetaData> newMetaData = new ArrayList<>();
        newMetaData.add(new MetaData());
        access.setMetadata(newMetaData);
        Assert.assertEquals(newMetaData, access.getMetadata());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto#setPassword(java.lang.String)}.
     */
    @Test
    public void testSetPassword() {
        final String newPassword = "newPassword";
        access.setPassword(newPassword);
        Assert.assertEquals(newPassword, access.getPassword());
    }

}
