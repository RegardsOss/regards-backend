package fr.cnes.regards.modules.emails.domain;

import javax.persistence.*;
import java.time.LocalDateTime;

import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.util.ObjectUtils;

import fr.cnes.regards.framework.jpa.IIdentifiable;

/**
 * Models a simple mail message, including data such as the from, to, cc, subject, and text fields.
 * <p>
 * This is a just a simplified representation of SimpleMailMessage for data base storage.
 *
 * @author Xavier-Alexandre Brochard
 * @author Christophe Mertz
 *
 */
@Entity(name = "T_EMAIL")
@SequenceGenerator(name = "emailSequence", initialValue = 1, sequenceName = "SEQ_EMAIL")
public class Email implements IIdentifiable<Long> {

    /**
     * Array of bcc recipients' email address
     */
    @Column(name = "bcc")
    private String[] bcc;

    /**
     * Array of cc recipients' email address
     */
    @Column(name = "cc")
    private String[] cc;

    /**
     * Sender's email address<br>
     * "_" prefix is required because "from" is a reserved keyword in SQL
     */
    @org.hibernate.validator.constraints.Email
    @Column(name = "_from")
    private String from;

    /**
     * Id of the email
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "emailSequence")
    @Column(name = "id")
    private Long id;

    /**
     * Email address of the replyTo recipient
     */
    @NotBlank
    @Column(name = "replyTo")
    private String replyTo;

    /**
     * Date when the email was sent
     */
    @Column(name = "sentDate")
    private LocalDateTime sentDate;

    /**
     * Subject of the email
     */
    @Column(name = "subject")
    private String subject;

    /**
     * Body of the email
     */
    @Column(name = "text")
    private String text;

    /**
     * Array of recipients' email address "_" prefix is required because "to" is a reserved keyword in SQL
     */
    @NotEmpty
    @Column(name = "_to")
    private String[] to;

    /**
     * Create a new {@code Email}.
     */
    public Email() {
        super();
    }

    /**
     * Get <code>bcc</code>
     *
     * @return The array of bcc recipients' email address
     */
    public String[] getBcc() {
        return bcc;
    }

    /**
     * Get <code>cc</code>
     *
     * @return The array of cc recipients' email address
     */
    public String[] getCc() {
        return cc;
    }

    /**
     * Get <code>from</code>
     *
     * @return The sender's email address
     */
    public String getFrom() {
        return from;
    }

    @Override
    public Long getId() {
        return id;
    }

    /**
     * Get <code>replyTo</code>
     *
     * @return The email address of the replyTo recipient
     */
    public String getReplyTo() {
        return replyTo;
    }

    /**
     * Get <code>sentDate</code>
     *
     * @return The date when the email was sent
     */
    public LocalDateTime getSentDate() {
        return sentDate;
    }

    /**
     * Get <code>subject</code>
     *
     * @return The email's subject
     */
    public String getSubject() {
        return subject;
    }

    /**
     * Get <code>text</code>
     *
     * @return The email's body
     */
    public String getText() {
        return text;
    }

    /**
     * Get <code>to</code>
     *
     * @return The array of recipients' email address
     */
    public String[] getTo() {
        return to;
    }

    /**
     * Set <code>bcc</code>
     *
     * @param pBcc
     *            The array of bcc recipients' email address
     */
    public void setBcc(final String[] pBcc) {
        bcc = pBcc;
    }

    /**
     * Set <code>cc</code>
     *
     * @param pCc
     *            The array of cc recipients' email address
     */
    public void setCc(final String[] pCc) {
        cc = pCc;
    }

    /**
     * Set <code>from</code>
     *
     * @param pFrom
     *            The sender's email address
     */
    public void setFrom(final String pFrom) {
        from = pFrom;
    }

    /**
     * Set <code>id</code>
     *
     * @param pId
     *            The email id
     */
    public void setId(final Long pId) {
        id = pId;
    }

    /**
     * Set <code>replyTo</code>
     *
     * @param pReplyTo
     *            The email address of the replyTo recipient
     */
    public void setReplyTo(final String pReplyTo) {
        replyTo = pReplyTo;
    }

    /**
     * Set <code>sentDate</code>
     *
     * @param pSentDate
     *            The date when the email was sent
     */
    public void setSentDate(final LocalDateTime pSentDate) {
        sentDate = pSentDate;
    }

    /**
     * Set <code>subject</code>
     *
     * @param pSubject
     *            The email's subject
     */
    public void setSubject(final String pSubject) {
        subject = pSubject;
    }

    /**
     * Set <code>text</code>
     *
     * @param pText
     *            The email's body
     */
    public void setText(final String pText) {
        text = pText;
    }

    /**
     * Set <code>to</code>
     *
     * @param pTo
     *            The array of recipients' email address
     */
    public void setTo(final String[] pTo) {
        to = pTo;
    }

    @Override
    public boolean equals(final Object pOther) {
        if (this == pOther) {
            return true;
        }
        if (!(pOther instanceof Email)) {
            return false;
        }
        final Email otherMessage = (Email) pOther;
        return (ObjectUtils.nullSafeEquals(this.from, otherMessage.from)
                && ObjectUtils.nullSafeEquals(this.replyTo, otherMessage.replyTo)
                && java.util.Arrays.equals(this.to, otherMessage.to)
                && java.util.Arrays.equals(this.cc, otherMessage.cc)
                && java.util.Arrays.equals(this.bcc, otherMessage.bcc)
                && ObjectUtils.nullSafeEquals(this.sentDate, otherMessage.sentDate)
                && ObjectUtils.nullSafeEquals(this.subject, otherMessage.subject)
                && ObjectUtils.nullSafeEquals(this.text, otherMessage.text));
    }

    @Override
    public int hashCode() {
        int hashCode;
        final int multiplier = 29;

        if (this.from == null) {
            hashCode = 0;
        } else {
            hashCode = this.from.hashCode();
        }
        hashCode = multiplier * hashCode;
        if (this.replyTo != null) {
            hashCode += this.replyTo.hashCode();
        }
        for (int i = 0; (this.to != null) && (i < this.to.length); i++) {
            hashCode = multiplier * hashCode;
            if (this.to != null) {
                hashCode += this.to[i].hashCode();
            }
        }
        for (int i = 0; (this.cc != null) && (i < this.cc.length); i++) {
            hashCode = multiplier * hashCode;
            if (this.cc != null) {
                hashCode += this.cc[i].hashCode();
            }
        }
        for (int i = 0; (this.bcc != null) && (i < this.bcc.length); i++) {
            hashCode = multiplier * hashCode;
            if (this.bcc != null) {
                hashCode += this.bcc[i].hashCode();
            }
        }
        hashCode = multiplier * hashCode;
        if (this.sentDate != null) {
            hashCode += this.sentDate.hashCode();
        }
        hashCode = multiplier * hashCode;
        if (this.subject != null) {
            hashCode += this.subject.hashCode();
        }
        hashCode = multiplier * hashCode;
        if (this.text != null) {
            hashCode += this.text.hashCode();
        }
        return hashCode;
    }

}
