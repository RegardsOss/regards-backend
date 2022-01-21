/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.instance.domain.accountunlock;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.Valid;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;

/**
 * Token for account unlocking process.
 *
 * @author Xavier-Alexandre Brochard
 * @author Christophe Mertz
 *
 */
@InstanceEntity
@Entity
@Table(name = "t_account_unlock_token",
        uniqueConstraints = @UniqueConstraint(name = "uk_account_unlock_token_account_id",
                columnNames = { "account_id" }))
public class AccountUnlockToken {

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
    @Column(name = "token", length = 255)
    private String token;

    /**
     * The link back to the {@link Account}
     */
    @Valid
    @OneToOne(optional = false)
    @JoinColumn(updatable = false, name = "account_id", foreignKey = @ForeignKey(name = "fk_unlock_token"))
    private Account account;

    /**
     * The computed expiration date based on EXPIRATION delay in minutes
     */
    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    /**
     * Verified?
     */
    @Column(name = "verified")
    private boolean verified;

    /**
     * Default constructor
     */
    public AccountUnlockToken() {
        super();
    }

    /**
     * Constructor
     * @param pToken the the token string
     * @param pAccount the linked account
     */
    public AccountUnlockToken(final String pToken, final Account pAccount) {
        super();
        this.token = pToken;
        this.account = pAccount;
        this.expiryDate = calculateExpiryDate(EXPIRATION);
        this.verified = false;
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
     *
     * Update token expiracy date from the current date.
     *
    
     */
    public void updateExipracyDate() {
        this.expiryDate = calculateExpiryDate(EXPIRATION);
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
}
