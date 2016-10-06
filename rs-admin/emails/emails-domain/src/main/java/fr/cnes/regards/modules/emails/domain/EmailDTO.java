/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.emails.domain;

import javax.mail.internet.MimeMessage;

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
     * Wrapped mail represented as a {@link MimeMessage}
     */
    private MimeMessage mail_;

    /**
     * Creates an {@link EmailDTO} with the given id_ and the given {@link SimpleMailMessage}.
     *
     * @param pId
     *            The email id_
     * @param pMail
     *            The email
     */
    public EmailDTO(final Long pId, final MimeMessage pMail) {
        super();
        id_ = pId;
        mail_ = pMail;
    }

    /**
     * Creates an {@link EmailDTO} with the given id_ <br>
     * and the given {@link MimeMessage}.
     *
     * @param pId
     *            The email id_
     * @param pMail
     *            The email
     */
    public EmailDTO(final MimeMessage pMail) {
        this(null, pMail);
    }

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
     * @return The wrapped mail message as {@link MimeMessage}
     */
    public MimeMessage getMail() {
        return mail_;
    }

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
    public void setMail(final MimeMessage pMail) {
        mail_ = pMail;
    }

}
