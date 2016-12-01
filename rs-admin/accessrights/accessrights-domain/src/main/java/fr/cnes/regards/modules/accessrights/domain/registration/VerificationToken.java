/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain.registration;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.validation.Valid;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;

/**
 * Verification token for verifiying the user's email process.
 *
 * @author Xavier-Alexandre Brochard
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
     * The link back to the accout
     */
    // @OneToOne(targetEntity = Account.class, fetch = FetchType.EAGER)
    @Valid
    @JoinColumn(nullable = false, name = "user_id", foreignKey = @ForeignKey(name = "FK_VERIFICATION_TOKEN_ACCOUNT"))
    private Account account;

    /**
     * The compouted expiry date based on EXPIRATION
     */
    private Date expiryDate;

    /**
     * Verified?
     */
    private boolean verified;

    public VerificationToken() {
        super();
    }

    public VerificationToken(final String pToken, final Account pAccount) {
        super();
        this.token = pToken;
        this.account = pAccount;
        this.expiryDate = calculateExpiryDate(EXPIRATION);
        this.verified = false;
    }

    /**
     * Calculate expiry date
     *
     * @param pExpiryTimeInMinutes
     *            self expl
     * @return the expiry date
     */
    private Date calculateExpiryDate(final int pExpiryTimeInMinutes) {
        final Calendar cal = Calendar.getInstance();
        cal.setTime(new Timestamp(cal.getTime().getTime()));
        cal.add(Calendar.MINUTE, pExpiryTimeInMinutes);
        return new Date(cal.getTime().getTime());
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
     * @return the account
     */
    public Account getAccount() {
        return account;
    }

    /**
     * @param pAccount
     *            the account to set
     */
    public void setAccount(final Account pAccount) {
        account = pAccount;
    }

    /**
     * @return the expiryDate
     */
    public Date getExpiryDate() {
        return expiryDate;
    }

    /**
     * @param pExpiryDate
     *            the expiryDate to set
     */
    public void setExpiryDate(final Date pExpiryDate) {
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
}
