/**
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.emails.domain;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit testing of {@link Recipient}
 *
 * @author Maxime Bouveron
 */
public class RecipientTest {

    /**
     * Test recipient
     */
    private final Recipient recipient = new Recipient();

    /**
     * Test address
     */
    private final String address = "adress";

    @Before
    public void setUp() {
        recipient.setAddress(address);
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.emails.domain.Recipient#getAddress()}.
     */
    @Test
    public void testGetAddress() {
        Assert.assertEquals(address, recipient.getAddress());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.emails.domain.Recipient#setAddress(java.lang.String)}.
     */
    @Test
    public void testSetAddress() {
        final String newAddress = "newAddress";
        recipient.setAddress(newAddress);
        Assert.assertEquals(newAddress, recipient.getAddress());
    }

}
