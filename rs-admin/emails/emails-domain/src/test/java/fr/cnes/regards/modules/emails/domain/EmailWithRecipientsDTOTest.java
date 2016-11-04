/**
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.emails.domain;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit testing of {@link EmailWithRecipientsDTO}
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

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        recipients.add(new Recipient());
        email.setId(4L);
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
        Set<Recipient> newRecipients = new HashSet<>();
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
     * Test method for
     * {@link fr.cnes.regards.modules.emails.domain.EmailWithRecipientsDTO#setEmail(fr.cnes.regards.modules.emails.domain.Email)}.
     */
    @Test
    public void testSetEmail() {
        Email newEmail = new Email();
        newEmail.setId(3L);
        emailDTO.setEmail(newEmail);
        Assert.assertEquals(newEmail, emailDTO.getEmail());
    }

}
