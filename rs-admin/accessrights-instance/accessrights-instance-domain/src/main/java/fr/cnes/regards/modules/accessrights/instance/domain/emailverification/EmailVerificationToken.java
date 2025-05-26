/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.instance.domain.emailverification;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Verification token for verifying the user's email process.
 *
 * @author Xavier-Alexandre Brochard
 * @author Christophe Mertz
 */
@InstanceEntity
@Entity
@Table(name = "t_email_verification_token",
       uniqueConstraints = @UniqueConstraint(name = "uk_email_verification_token_account_id",
                                             columnNames = { "account_id" }))
public class EmailVerificationToken {

    /**
     * Expiration delay in days
     */
    private static final int EXPIRATION = 3;

    /**
     * Id
     */
    @Id
    @SequenceGenerator(name = "EmailVerificationTokenSequenceGenerator",
                       initialValue = 1,
                       sequenceName = "seq_email_verification_token")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "EmailVerificationTokenSequenceGenerator")
    private Long id;

    /**
     * Randomly generated string
     */
    @Column(name = "token", length = 255)
    private String token;

    /**
     * The link back to the {@link Account}
     */
    @OneToOne(optional = false)
    @JoinColumn(updatable = false, name = "account_id", foreignKey = @ForeignKey(name = "fk_email_verification_token"))
    private Account account;

    /**
     * The origin url
     */
    @NotBlank
    @Column(name = "origin_url", columnDefinition = "text")
    private String originUrl;

    /**
     * The request link
     */
    @NotBlank
    @Column(name = "request_link", columnDefinition = "text")
    private String requestLink;

    /**
     * The computed expiration date based on EXPIRATION delay in minutes
     */
    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    /**
     * Default constructor
     */
    public EmailVerificationToken() {
        super();
    }

    /**
     * @param account     The link back to the {@link Account}
     * @param originUrl   The origin url
     * @param requestLink The request link
     */
    public EmailVerificationToken(final Account account, final String originUrl, final String requestLink) {
        super();
        this.renew();
        this.expiryDate = LocalDateTime.now().plusDays(EXPIRATION);
        this.account = account;
        this.originUrl = originUrl;
        this.requestLink = requestLink;
    }

    /**
     * Generate a new random token
     */
    public final void renew() {
        token = UUID.randomUUID().toString();
        expiryDate = LocalDateTime.now().plusDays(EXPIRATION);
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * @return the token
     */
    public String getToken() {
        return token;
    }

    /**
     * @param token the token to set
     */
    public void setToken(final String token) {
        this.token = token;
    }

    /**
     * @return the expiryDate
     */
    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    /**
     * @param expiryDate the expiryDate to set
     */
    public void setExpiryDate(final LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    /**
     * @return the originUrl
     */
    public String getOriginUrl() {
        return originUrl;
    }

    /**
     * @param originUrl the originUrl to set
     */
    public void setOriginUrl(final String originUrl) {
        this.originUrl = originUrl;
    }

    /**
     * @return the projectUser
     */
    public Account getAccount() {
        return account;
    }

    /**
     * @param account the account to set
     */
    public void setAccount(Account account) {
        this.account = account;
    }

    /**
     * @return the requestLink
     */
    public String getRequestLink() {
        return requestLink;
    }

    /**
     * @param requestLink the requestLink to set
     */
    public void setRequestLink(final String requestLink) {
        this.requestLink = requestLink;
    }
}
