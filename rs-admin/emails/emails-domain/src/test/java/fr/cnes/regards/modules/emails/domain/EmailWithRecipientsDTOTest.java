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

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit testing of {@link EmailWithRecipientsDTO}
 *
 * @author Maxime Bouveron
 */
public class EmailWithRecipientsDTOTest {

    /**
     * test EmailWithRecipientsDTO
     */
    private final EmailWithRecipientsDTO emailDTO = new EmailWithRecipientsDTO();

    /**
     * test recipients
     */
    private final Set<Recipient> recipients = new HashSet<>();

    /**
     * Test email
     */
    private final Email email = new Email();

    @Before
    public void setUp() {
        recipients.add(new Recipient());
        final Long localId = 4L;
        email.setId(localId);
        emailDTO.setRecipients(recipients);
        emailDTO.setEmail(email);
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.emails.domain.EmailWithRecipientsDTO#getRecipients()}.
     */
    @Test
    public void testGetRecipients() {
        Assert.assertEquals(recipients, emailDTO.getRecipients());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.emails.domain.EmailWithRecipientsDTO#setRecipients(java.util.Set)}.
     */
    @Test
    public void testSetRecipients() {
        final Set<Recipient> newRecipients = new HashSet<>();
        newRecipients.add(new Recipient());
        newRecipients.add(new Recipient());
        emailDTO.setRecipients(newRecipients);
        Assert.assertEquals(newRecipients, emailDTO.getRecipients());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.emails.domain.EmailWithRecipientsDTO#getEmail()}.
     */
    @Test
    public void testGetEmail() {
        Assert.assertEquals(email, emailDTO.getEmail());
    }

    /**
     * Test method for {@link EmailWithRecipientsDTO#setEmail(Email)}.
     */
    @Test
    public void testSetEmail() {
        final Email newEmail = new Email();
        final Long localId = 3L;
        newEmail.setId(localId);
        emailDTO.setEmail(newEmail);
        Assert.assertEquals(newEmail, emailDTO.getEmail());
    }

}
