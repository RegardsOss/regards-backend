/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.emails.domain;

import java.io.IOException;

import javax.mail.MessagingException;

import org.springframework.hateoas.Identifiable;
import org.springframework.mail.SimpleMailMessage;

/**
 * Data Transfer Object wrapping a {@link SimpleMailMessage} with an id for data base persistence.
 *
 * @author xbrochard
 *
 */
public class EmailDTO implements Identifiable<Long> {

    /**
     * Id
     */
    private Long id_;

    /**
     * Wrapped mail represented as a {@link SimpleMailMessage}
     */
    private SimpleMailMessage mail_;

    // /**
    // * Base64 encoded mail for data base persistence. {@link SimpleMailMessage} is not {@link Serializable}.
    // *
    // */
    // private String encodedMail_;

    /**
     * Creates an {@link EmailDTO} with the given id_ and the given {@link SimpleMailMessage}.
     *
     * @param pId
     *            The email id_
     * @param pMail
     *            The email
     * @throws MessagingException
     * @throws IOException
     */
    public EmailDTO(final Long pId, final SimpleMailMessage pMail) {
        super();
        id_ = pId;
        mail_ = pMail;
    }

    /**
     * Creates an {@link EmailDTO} with the given id_ <br>
     * and the given {@link SimpleMailMessage}.
     *
     * @param pId
     *            The email id_
     * @param pMail
     *            The email
     * @throws MessagingException
     * @throws IOException
     */
    public EmailDTO(final SimpleMailMessage pMail) {
        this(null, pMail);
    }
    //
    // /**
    // * Creates an {@link EmailDTO} with the given id_ <br>
    // * and the given encoded mail as a base 64 string that can be decoded into a {@link SimpleMailMessage}.
    // *
    // * @param pId
    // * The email id_
    // * @param pEncodedMail
    // * The encoded email
    // * @throws MessagingException
    // * @throws IOException
    // */
    // public EmailDTO(final Long pId, final String pEncodedMail) throws IOException, MessagingException {
    // super();
    // id_ = pId;
    // encodedMail_ = pEncodedMail;
    // }

    // /**
    // * Creates an {@link EmailDTO} with the given encoded mail as a base 64 string that can be decoded into a
    // * {@link SimpleMailMessage}.
    // *
    // * @param pEncodedMail
    // * The encoded email
    // * @throws MessagingException
    // * @throws IOException
    // */
    // public EmailDTO(final String pEncodedMail) {
    // super();
    // encodedMail_ = pEncodedMail;
    // }

    /**
     * Get id_
     *
     * @return The email id_
     */
    @Override
    public Long getId() {
        return id_;
    }

    /**
     * Get mail_
     *
     * @return The wrapped mail message as {@link SimpleMailMessage}
     */
    public SimpleMailMessage getMail() {
        return mail_;
        // byte[] bytearray = Base64Utils.decodeFromString(encodedMail_);
        // ByteArrayInputStream bais = new ByteArrayInputStream(bytearray);
        // SimpleMailMessage message = new SimpleMailMessage(session, bais);

        // Session session = Session.
        // return mail_;
    }

    // public void setEncodedMail(final String pEncodedMail) {
    // encodedMail_ = pEncodedMail;
    // }

    /**
     * Set id_
     *
     * @param pId
     *            The email id_
     */
    public void setId(final Long pId) {
        id_ = pId;
    }

    /**
     * Set mail_
     *
     * @param pMail
     *            The email
     */
    public void setMail(final SimpleMailMessage pMail) {
        mail_ = pMail;
    }

    // public String getEncodedMail() {
    // return encodedMail_;
    // }

}
