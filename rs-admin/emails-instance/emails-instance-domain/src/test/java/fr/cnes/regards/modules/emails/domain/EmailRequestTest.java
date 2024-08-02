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
package fr.cnes.regards.modules.emails.domain;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit testing of {@link EmailRequest}
 *
 * @author Maxime Bouveron
 * @author Christophe Mertz
 */
public class EmailRequestTest {

    /**
     * Test email
     */
    private EmailRequest emailRequest;

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
        emailRequest = new EmailRequest();
        emailRequest.setBcc(bcc);
        emailRequest.setCc(cc);
        emailRequest.setFrom(from);
        emailRequest.setId(id);
        emailRequest.setReplyTo(replyTo);
        emailRequest.setSubject(subject);
        emailRequest.setText(text);
        emailRequest.setTo(to);
    }

    /**
     * Test method for {@link EmailRequest#hashCode()}.
     */
    @Test
    public void testHashCode() {
        final EmailRequest testMail = new EmailRequest();
        Assert.assertNotEquals(emailRequest.hashCode(), testMail.hashCode());
    }

    /**
     * Test method for {@link EmailRequest#EmailRequest()}.
     */
    @Test
    public void testEmail() {
        final EmailRequest testMail = new EmailRequest();
        Assert.assertArrayEquals(null, testMail.getBcc());
        Assert.assertArrayEquals(null, testMail.getCc());
        Assert.assertNull(testMail.getFrom());
        Assert.assertNull(testMail.getId());
        Assert.assertNull(testMail.getReplyTo());
        Assert.assertNull(testMail.getSubject());
        Assert.assertNull(testMail.getText());
        Assert.assertArrayEquals(null, testMail.getTo());
    }

    /**
     * Test method for {@link EmailRequest#getBcc()}.
     */
    @Test
    public void testGetBcc() {
        Assert.assertArrayEquals(bcc, emailRequest.getBcc());
    }

    /**
     * Test method for {@link EmailRequest#getCc()}.
     */
    @Test
    public void testGetCc() {
        Assert.assertArrayEquals(cc, emailRequest.getCc());
    }

    /**
     * Test method for {@link EmailRequest#getFrom()}.
     */
    @Test
    public void testGetFrom() {
        Assert.assertEquals(from, emailRequest.getFrom());
    }

    /**
     * Test method for {@link EmailRequest#getId()}.
     */
    @Test
    public void testGetId() {
        Assert.assertEquals(id, emailRequest.getId());
    }

    /**
     * Test method for {@link EmailRequest#getReplyTo()}.
     */
    @Test
    public void testGetReplyTo() {
        Assert.assertEquals(replyTo, emailRequest.getReplyTo());
    }

    /**
     * Test method for {@link EmailRequest#getSubject()}.
     */
    @Test
    public void testGetSubject() {
        Assert.assertEquals(subject, emailRequest.getSubject());
    }

    /**
     * Test method for {@link EmailRequest#getText()}.
     */
    @Test
    public void testGetText() {
        Assert.assertEquals(text, emailRequest.getText());
    }

    /**
     * Test method for {@link EmailRequest#getTo()}.
     */
    @Test
    public void testGetTo() {
        Assert.assertArrayEquals(to, emailRequest.getTo());
    }

    /**
     * Test method for {@link EmailRequest#setBcc(java.lang.String[])}.
     */
    @Test
    public void testSetBcc() {
        final String[] newBcc = { "newbcc", "newbcc2" };
        emailRequest.setBcc(newBcc);
        Assert.assertArrayEquals(newBcc, emailRequest.getBcc());
    }

    /**
     * Test method for {@link EmailRequest#setCc(java.lang.String[])}.
     */
    @Test
    public void testSetCc() {
        final String[] newCc = { "newcc", "newcc2" };
        emailRequest.setCc(newCc);
        Assert.assertArrayEquals(newCc, emailRequest.getCc());
    }

    /**
     * Test method for {@link EmailRequest#setFrom(java.lang.String)}.
     */
    @Test
    public void testSetFrom() {
        final String newFrom = "newfrom";
        emailRequest.setFrom(newFrom);
        Assert.assertEquals(newFrom, emailRequest.getFrom());
    }

    /**
     * Test method for {@link EmailRequest#setId(java.lang.Long)}.
     */
    @Test
    public void testSetId() {
        final Long newId = 4L;
        emailRequest.setId(newId);
        Assert.assertEquals(newId, emailRequest.getId());
    }

    /**
     * Test method for {@link EmailRequest#setReplyTo(java.lang.String)}.
     */
    @Test
    public void testSetReplyTo() {
        final String newReplyTo = "newreplyTo";
        emailRequest.setReplyTo(newReplyTo);
        Assert.assertEquals(newReplyTo, emailRequest.getReplyTo());
    }

    /**
     * Test method for {@link EmailRequest#setSubject(java.lang.String)}.
     */
    @Test
    public void testSetSubject() {
        final String newSubject = "newsubject";
        emailRequest.setSubject(newSubject);
        Assert.assertEquals(newSubject, emailRequest.getSubject());
    }

    /**
     * Test method for {@link EmailRequest#setText(java.lang.String)}.
     */
    @Test
    public void testSetText() {
        final String newText = "newtext";
        emailRequest.setText(newText);
        Assert.assertEquals(newText, emailRequest.getText());
    }

    /**
     * Test method for {@link EmailRequest#setTo(java.lang.String[])}.
     */
    @Test
    public void testSetTo() {
        final String[] newTo = { "newto", "newto2" };
        emailRequest.setTo(newTo);
        Assert.assertArrayEquals(newTo, emailRequest.getTo());
    }

    /**
     * Test method for {@link EmailRequest#equals(java.lang.Object)}.
     */
    @Test
    public void testEqualsObject() {
        final EmailRequest testMail = new EmailRequest();
        Assert.assertNotEquals(emailRequest, testMail);

        final EmailRequest testMail2 = new EmailRequest();
        Assert.assertEquals(testMail2, testMail);
    }

}
