/**LICENSE_PLACEHOLDER*/
package fr.cnes.regards.modules.accessrights.domain.instance;

import java.util.Calendar;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;

/**
 * Data base persisted token for resetting password
 *
 * @author Xavier-Alexandre Brochard
 * @see <a>http://www.baeldung.com/spring-security-registration-i-forgot-my-password</a>
 */
@InstanceEntity
@Entity(name = "T_PASSWORD_RESET_TOKEN")
public class PasswordResetToken {

    private static final int EXPIRATION = 60 * 24;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String token;

    @OneToOne(targetEntity = Account.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "account_id")
    private Account account;

    private Date expiryDate;

    public PasswordResetToken() {
        super();
    }

    public PasswordResetToken(final String pToken) {
        super();

        this.token = pToken;
        this.expiryDate = calculateExpiryDate(EXPIRATION);
    }

    public PasswordResetToken(final String pToken, final Account pAccount) {
        super();

        this.token = pToken;
        this.account = pAccount;
        this.expiryDate = calculateExpiryDate(EXPIRATION);
    }

    //
    public Long getId() {
        return id;
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

    public String getToken() {
        return token;
    }

    public void setToken(final String pToken) {
        this.token = pToken;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(final Date pExpiryDate) {
        this.expiryDate = pExpiryDate;
    }

    private Date calculateExpiryDate(final int pExpiryTimeInMinutes) {
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(new Date().getTime());
        cal.add(Calendar.MINUTE, pExpiryTimeInMinutes);
        return new Date(cal.getTime().getTime());
    }

    public void updateToken(final String pToken) {
        this.token = pToken;
        this.expiryDate = calculateExpiryDate(EXPIRATION);
    }

    //

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((expiryDate == null) ? 0 : expiryDate.hashCode());
        result = (prime * result) + ((token == null) ? 0 : token.hashCode());
        result = (prime * result) + ((account == null) ? 0 : account.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object pObj) {
        if (this == pObj) {
            return true;
        }
        if (pObj == null) {
            return false;
        }
        if (getClass() != pObj.getClass()) {
            return false;
        }
        final PasswordResetToken other = (PasswordResetToken) pObj;
        if (expiryDate == null) {
            if (other.expiryDate != null) {
                return false;
            }
        } else
            if (!expiryDate.equals(other.expiryDate)) {
                return false;
            }
        if (token == null) {
            if (other.token != null) {
                return false;
            }
        } else
            if (!token.equals(other.token)) {
                return false;
            }
        if (account == null) {
            if (other.account != null) {
                return false;
            }
        } else
            if (!account.equals(other.account)) {
                return false;
            }
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Token [String=").append(token).append("]").append("[Expires").append(expiryDate).append("]");
        return builder.toString();
    }

}