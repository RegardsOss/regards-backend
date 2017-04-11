/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain.registration;

import java.time.LocalDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import org.hibernate.validator.constraints.NotBlank;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;

/**
 * Verification token for verifying the user's email process.
 *
 * @author Xavier-Alexandre Brochard
 * @author Christophe Mertz
 *
 * @see <a>http://www.baeldung.com/registration-verify-user-by-email</a>
 */
@InstanceEntity
@Entity(name = "T_VERIFICATION_TOKEN")
public class VerificationToken {

    /**
     * Expiration delay in minutes (=24 hours)
     */
    private static final int EXPIRATION = 60 * 24;

    /**
     * Id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /**
     * Randomly generated string
     */
    private String token;

    /**
     * The link back to the {@link Account}
     */
    @OneToOne(optional = false)
    @JoinColumn(updatable = false, name = "account_id", foreignKey = @ForeignKey(name = "FK_VERIFICATION_TOKEN"))
    private Account account;

    /**
     * The origin url
     */
    @NotBlank
    @Column(name = "originUrl")
    private String originUrl;

    /**
     * The request link
     */
    @NotBlank
    @Column(name = "requestLink")
    private String requestLink;

    /**
     * The computed expiration date based on EXPIRATION delay in minutes
     */
    private LocalDateTime expiryDate;

    /**
     * Verified?
     */
    private boolean verified;

    public VerificationToken() {
        super();
    }

    /**
     *
     * @param pAccount
     *            The link back to the {@link Account}
     * @param pOriginUrl
     *            The origin url
     * @param pRequestLink
     *            The request link
     */
    public VerificationToken(final Account pAccount, final String pOriginUrl, final String pRequestLink) {
        super();
        token = UUID.randomUUID().toString();
        account = pAccount;
        originUrl = pOriginUrl;
        requestLink = pRequestLink;
        expiryDate = calculateExpiryDate(EXPIRATION);
        verified = false;
    }

    /**
     * Calculate expiration date
     *
     * @param pExpiryTimeInMinutes
     *            the expiration time in minutes
     * @return the expiration date
     */
    private LocalDateTime calculateExpiryDate(final long pExpiryTimeInMinutes) {
        return LocalDateTime.now().plusMinutes(pExpiryTimeInMinutes);
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param pId
     *            the id to set
     */
    public void setId(final Long pId) {
        id = pId;
    }

    /**
     * @return the token
     */
    public String getToken() {
        return token;
    }

    /**
     * @param pToken
     *            the token to set
     */
    public void setToken(final String pToken) {
        token = pToken;
    }

    /**
     * @return the {@link Account}
     */
    public Account getAccount() {
        return account;
    }

    /**
     * @param pAccount
     *            the {@link Account} to set
     */
    public void setAccount(final Account pAccount) {
        account = pAccount;
    }

    /**
     * @return the expiryDate
     */
    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    /**
     * @param pExpiryDate
     *            the expiryDate to set
     */
    public void setExpiryDate(final LocalDateTime pExpiryDate) {
        expiryDate = pExpiryDate;
    }

    /**
     * @return the verified
     */
    public boolean isVerified() {
        return verified;
    }

    /**
     * @param pVerified
     *            the verified to set
     */
    public void setVerified(final boolean pVerified) {
        verified = pVerified;
    }

    /**
     * @return the originUrl
     */
    public String getOriginUrl() {
        return originUrl;
    }

    /**
     * @param pOriginUrl
     *            the originUrl to set
     */
    public void setOriginUrl(final String pOriginUrl) {
        originUrl = pOriginUrl;
    }

    /**
     * @return the requestLink
     */
    public String getRequestLink() {
        return requestLink;
    }

    /**
     * @param pRequestLink
     *            the requestLink to set
     */
    public void setRequestLink(final String pRequestLink) {
        requestLink = pRequestLink;
    }
}
