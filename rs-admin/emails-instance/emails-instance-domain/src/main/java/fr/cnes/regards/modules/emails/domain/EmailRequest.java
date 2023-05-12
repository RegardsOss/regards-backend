package fr.cnes.regards.modules.emails.domain;

import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.framework.jpa.converters.StringArrayConverter;
import org.hibernate.annotations.Type;
import org.springframework.util.ObjectUtils;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;

/**
 * Models a request for a simple mail message, including data such as the from, to, cc, subject, and text fields.
 * <p>
 * This is a just a request for a simplified representation of SimpleMailMessage for data base storage.
 *
 * @author Xavier-Alexandre Brochard
 * @author Christophe Mertz
 */
@InstanceEntity
@Entity
@Table(name = "t_email_requests", indexes = { @Index(name = "idx_next_try_date", columnList = "next_try_date") })
public class EmailRequest implements IIdentifiable<Long> {

    public static final int MAX_EMAIL_ADDRESS_SIZE = 320;

    public static final int MAX_SUBJECT_SIZE = 78;

    // completely arbitrary but @Type("text") cannot be used conjointly with @Convert
    public static final int MAX_ARRAY_STRING_SIZE = 1000;

    public static final int MAX_UNSUCCESSFULL_TRY = 9;

    /**
     * Id of the email request
     */
    @Id
    @SequenceGenerator(name = "emailRequestSequence", initialValue = 1, sequenceName = "seq_email_request")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "emailRequestSequence")
    @Column(name = "id")
    private Long id;

    /**
     * Array of bcc recipients' email address
     */
    @Column(name = "bcc_addrs", length = MAX_ARRAY_STRING_SIZE)
    @Convert(converter = StringArrayConverter.class)
    private String[] bcc;

    /**
     * Array of cc recipients' email address
     */
    @Column(name = "cc_addrs", length = MAX_ARRAY_STRING_SIZE)
    @Convert(converter = StringArrayConverter.class)
    private String[] cc;

    /**
     * Sender's email address<br>
     * "_" prefix is required because "from" is a reserved keyword in SQL
     */
    @Column(name = "from_addr", length = MAX_EMAIL_ADDRESS_SIZE)
    @javax.validation.constraints.Email
    private String from;

    /**
     * Email address of the replyTo recipient
     */
    @Column(name = "reply_to_addr", length = MAX_EMAIL_ADDRESS_SIZE)
    private String replyTo;

    /**
     * Subject of the email
     */
    @Column(name = "subject", length = MAX_SUBJECT_SIZE)
    private String subject;

    /**
     * Body of the email
     */
    @Column(name = "text")
    @Type(type = "text")
    private String text;

    @NotEmpty
    @Column(name = "to_addrs", length = MAX_ARRAY_STRING_SIZE)
    @Convert(converter = StringArrayConverter.class)
    private String[] to;

    @Column(name = "attachment_name", length = 100) // reasonable size for a filename
    private String attachmentName;

    //    @Lob
    @Column(name = "attachment")
    private byte[] attachment;

    @Column(name = "nb_unsuccessfull_try")
    private int nbUnsuccessfullTry = 0;

    @Column(name = "next_try_date", nullable = false)
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    @NotNull(message = "next try date is required")
    private OffsetDateTime nextTryDate;

    /**
     * Create a {@code EmailRequest}.
     */
    public EmailRequest() {
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
     * Get <code>nbUnsuccessfullTry</code>
     *
     * @return the number unsuccessfull try for the sending email
     */
    public int getNbUnsuccessfullTry() {
        return nbUnsuccessfullTry;
    }

    /**
     * Get <code>nextTryDate</code>
     *
     * @return the date of next try for the sending email
     */
    public OffsetDateTime getNextTryDate() {
        return nextTryDate;
    }

    /**
     * Get <code>attachmentName</code>
     *
     * @return the name of attechment
     */
    public String getAttachmentName() {
        return attachmentName;
    }

    /**
     * Get <code>attachment</code>
     *
     * @return the array of byte for attachment
     */
    public byte[] getAttachment() {
        return attachment;
    }

    /**
     * Set <code>bcc</code>
     *
     * @param bcc The array of bcc recipients' email address
     */
    public void setBcc(final String[] bcc) {
        this.bcc = bcc;
    }

    /**
     * Set <code>cc</code>
     *
     * @param cc The array of cc recipients' email address
     */
    public void setCc(final String[] cc) {
        this.cc = cc;
    }

    /**
     * Set <code>from</code>
     *
     * @param from The sender's email address
     */
    public void setFrom(final String from) {
        this.from = from;
    }

    /**
     * Set <code>id</code>
     *
     * @param id The email id
     */
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * Set <code>replyTo</code>
     *
     * @param replyTo The email address of the replyTo recipient
     */
    public void setReplyTo(final String replyTo) {
        this.replyTo = replyTo;
    }

    /**
     * Set <code>subject</code>
     *
     * @param subject The email's subject
     */
    public void setSubject(final String subject) {
        this.subject = subject;
    }

    /**
     * Set <code>text</code>
     *
     * @param text The email's body
     */
    public void setText(final String text) {
        this.text = text;
    }

    /**
     * Set <code>to</code>
     *
     * @param to The array of recipients' email address
     */
    public void setTo(final String[] to) {
        this.to = to;
    }

    /**
     * Set <code>attachmentName</code>
     *
     * @param attName the name of attachment
     */
    public void setAttachmentName(String attName) {
        this.attachmentName = attName;
    }

    /**
     * Set <code>attachment</code>
     *
     * @param attachment the array of byte for attachment
     */
    public void setAttachment(byte[] attachment) {
        this.attachment = attachment;
    }

    /**
     * Set <code>nbUnsuccessfullTry</code>
     *
     * @param nbUnsuccessfullTry the number unsuccessfull try for the sending email
     */
    public void setNbUnsuccessfullTry(int nbUnsuccessfullTry) {
        this.nbUnsuccessfullTry = nbUnsuccessfullTry;
    }

    /**
     * Set <code>nextTryDate</code>
     *
     * @param nextTryDate the date of next try for the sending email
     */
    public void setNextTryDate(OffsetDateTime nextTryDate) {
        this.nextTryDate = nextTryDate;
    }

    @Override
    public boolean equals(final Object pOther) {
        if (this == pOther) {
            return true;
        }
        if (!(pOther instanceof EmailRequest)) {
            return false;
        }
        final EmailRequest otherMessage = (EmailRequest) pOther;
        return (ObjectUtils.nullSafeEquals(this.from, otherMessage.from)
                && ObjectUtils.nullSafeEquals(this.replyTo,
                                              otherMessage.replyTo)
                && java.util.Arrays.equals(this.to, otherMessage.to)
                && java.util.Arrays.equals(this.cc, otherMessage.cc)
                && java.util.Arrays.equals(this.bcc, otherMessage.bcc)
                && ObjectUtils.nullSafeEquals(this.subject, otherMessage.subject)
                && ObjectUtils.nullSafeEquals(this.text, otherMessage.text)
                && this.nbUnsuccessfullTry == otherMessage.nbUnsuccessfullTry
                && ObjectUtils.nullSafeEquals(this.nextTryDate, otherMessage.nextTryDate));
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
            hashCode += this.to[i].hashCode();
        }
        for (int i = 0; (this.cc != null) && (i < this.cc.length); i++) {
            hashCode = multiplier * hashCode;
            hashCode += this.cc[i].hashCode();
        }
        for (int i = 0; (this.bcc != null) && (i < this.bcc.length); i++) {
            hashCode = multiplier * hashCode;
            hashCode += this.bcc[i].hashCode();
        }
        hashCode = multiplier * hashCode;
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
