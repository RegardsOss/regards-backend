/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.accessrights.instance.domain.passwordreset;

import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;

/**
 * Data base persisted token for resetting password
 *
 * @author Xavier-Alexandre Brochard
 * @author Christophe Mertz
 *
 * @see <a>http://www.baeldung.com/spring-security-registration-i-forgot-my-password</a>
 */
@InstanceEntity
@Entity
@Table(name = "t_password_reset_token",
        uniqueConstraints = @UniqueConstraint(name = "uk_password_reset_token_account_id",
                columnNames = { "account_id" }))
public class PasswordResetToken {

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
    @JoinColumn(updatable = false, name = "account_id", foreignKey = @ForeignKey(name = "fk_password_reset_token"))
    private Account account;

    /**
     * The computed expiration date based on EXPIRATION delay in minutes
     */
    private LocalDateTime expiryDate;

    /**
     * Default constructor
     */
    public PasswordResetToken() {
        super();
    }

    /**
     * Constructor
     * @param pToken the token string
     */
    public PasswordResetToken(final String pToken) {
        super();
        this.token = pToken;
        this.expiryDate = calculateExpiryDate(EXPIRATION);
    }

    /**
     * Constructor
     * @param pToken the token string
     * @param pAccount the linked account
     */
    public PasswordResetToken(final String pToken, final Account pAccount) {
        super();

        this.token = pToken;
        this.account = pAccount;
        this.expiryDate = calculateExpiryDate(EXPIRATION);
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
    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    /**
     * @param pExpiryDate
     *            the expiryDate to set
     */
    public void setExpiryDate(final LocalDateTime pExpiryDate) {
        this.expiryDate = pExpiryDate;
    }

    /**
     * Refresh the expiry date
     * @param pToken
     */
    public void updateToken(final String pToken) {
        this.token = pToken;
        this.expiryDate = calculateExpiryDate(EXPIRATION);
    }

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
        } else if (!expiryDate.equals(other.expiryDate)) {
            return false;
        }
        if (token == null) {
            if (other.token != null) {
                return false;
            }
        } else if (!token.equals(other.token)) {
            return false;
        }
        if (account == null) {
            if (other.account != null) {
                return false;
            }
        } else if (!account.equals(other.account)) {
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