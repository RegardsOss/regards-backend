/**
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.emails.domain;

import java.util.Date;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit testing of {@link Email}
 *
 * @author Maxime Bouveron
 */
public class EmailTest {

    /**
     * Test email
     */
    private Email email;

    /**
     * Test bcc
     */
    private final String[] bcc = { "bcc", "bcc2" };

    /**
     * Test ;
     */
    private final String[] cc = { "cc", "cc2" };

    /**
     * Test from
     */
    private final String from = "from";

    /**
     * Test id
     */
    private final Long id = 0L;

    /**
     * Test replyTo
     */
    private final String replyTo = "replyTo";

    /**
     * Test sentDate
     */
    private final Date sentDate = new Date();

    /**
     * Test subject
     */
    private final String subject = "subject";

    /**
     * Test text
     */
    private final String text = "text";

    /**
     * Test to
     */
    private final String[] to = { "to", "to2" };

    @Before
    public void setUp() {
        email = new Email();
        email.setBcc(bcc);
        email.setCc(cc);
        email.setFrom(from);
        email.setId(id);
        email.setReplyTo(replyTo);
        email.setSentDate(sentDate);
        email.setSubject(subject);
        email.setText(text);
        email.setTo(to);
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.emails.domain.Email#hashCode()}.
     */
    @Test
    public void testHashCode() {
        final Email testMail = new Email();
        Assert.assertNotEquals(email.hashCode(), testMail.hashCode());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.emails.domain.Email#Email()}.
     */
    @Test
    public void testEmail() {
        final Email testMail = new Email();
        Assert.assertArrayEquals(null, testMail.getBcc());
        Assert.assertArrayEquals(null, testMail.getCc());
        Assert.assertEquals(null, testMail.getFrom());
        Assert.assertEquals(null, testMail.getId());
        Assert.assertEquals(null, testMail.getReplyTo());
        Assert.assertEquals(null, testMail.getSentDate());
        Assert.assertEquals(null, testMail.getSubject());
        Assert.assertEquals(null, testMail.getText());
        Assert.assertArrayEquals(null, testMail.getTo());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.emails.domain.Email#getBcc()}.
     */
    @Test
    public void testGetBcc() {
        Assert.assertArrayEquals(bcc, email.getBcc());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.emails.domain.Email#getCc()}.
     */
    @Test
    public void testGetCc() {
        Assert.assertArrayEquals(cc, email.getCc());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.emails.domain.Email#getFrom()}.
     */
    @Test
    public void testGetFrom() {
        Assert.assertEquals(from, email.getFrom());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.emails.domain.Email#getId()}.
     */
    @Test
    public void testGetId() {
        Assert.assertEquals(id, email.getId());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.emails.domain.Email#getReplyTo()}.
     */
    @Test
    public void testGetReplyTo() {
        Assert.assertEquals(replyTo, email.getReplyTo());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.emails.domain.Email#getSentDate()}.
     */
    @Test
    public void testGetSentDate() {
        Assert.assertEquals(sentDate, email.getSentDate());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.emails.domain.Email#getSubject()}.
     */
    @Test
    public void testGetSubject() {
        Assert.assertEquals(subject, email.getSubject());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.emails.domain.Email#getText()}.
     */
    @Test
    public void testGetText() {
        Assert.assertEquals(text, email.getText());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.emails.domain.Email#getTo()}.
     */
    @Test
    public void testGetTo() {
        Assert.assertArrayEquals(to, email.getTo());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.emails.domain.Email#setBcc(java.lang.String[])}.
     */
    @Test
    public void testSetBcc() {
        final String[] newBcc = { "newbcc", "newbcc2" };
        email.setBcc(newBcc);
        Assert.assertArrayEquals(newBcc, email.getBcc());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.emails.domain.Email#setCc(java.lang.String[])}.
     */
    @Test
    public void testSetCc() {
        final String[] newCc = { "newcc", "newcc2" };
        email.setCc(newCc);
        Assert.assertArrayEquals(newCc, email.getCc());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.emails.domain.Email#setFrom(java.lang.String)}.
     */
    @Test
    public void testSetFrom() {
        final String newFrom = "newfrom";
        email.setFrom(newFrom);
        Assert.assertEquals(newFrom, email.getFrom());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.emails.domain.Email#setId(java.lang.Long)}.
     */
    @Test
    public void testSetId() {
        final Long newId = 4L;
        email.setId(newId);
        Assert.assertEquals(newId, email.getId());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.emails.domain.Email#setReplyTo(java.lang.String)}.
     */
    @Test
    public void testSetReplyTo() {
        final String newReplyTo = "newreplyTo";
        email.setReplyTo(newReplyTo);
        Assert.assertEquals(newReplyTo, email.getReplyTo());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.emails.domain.Email#setSentDate(java.util.Date)}.
     */
    @Test
    public void testSetSentDate() {
        final Date newSentDate = new Date();
        email.setSentDate(newSentDate);
        Assert.assertEquals(newSentDate, email.getSentDate());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.emails.domain.Email#setSubject(java.lang.String)}.
     */
    @Test
    public void testSetSubject() {
        final String newSubject = "newsubject";
        email.setSubject(newSubject);
        Assert.assertEquals(newSubject, email.getSubject());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.emails.domain.Email#setText(java.lang.String)}.
     */
    @Test
    public void testSetText() {
        final String newText = "newtext";
        email.setText(newText);
        Assert.assertEquals(newText, email.getText());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.emails.domain.Email#setTo(java.lang.String[])}.
     */
    @Test
    public void testSetTo() {
        final String[] newTo = { "newto", "newto2" };
        email.setTo(newTo);
        Assert.assertArrayEquals(newTo, email.getTo());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.emails.domain.Email#equals(java.lang.Object)}.
     */
    @Test
    public void testEqualsObject() {
        final Email testMail = new Email();
        Assert.assertFalse(email.equals(testMail));

        final Email testMail2 = new Email();
        Assert.assertTrue(testMail2.equals(testMail));

        Assert.assertTrue(testMail.equals(testMail));

        Assert.assertFalse(testMail.equals(null));
    }

}
