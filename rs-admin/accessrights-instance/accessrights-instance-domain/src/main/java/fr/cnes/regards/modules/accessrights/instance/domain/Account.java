/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.instance.domain;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Length;

import fr.cnes.regards.framework.gson.annotation.GsonIgnore;
import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;

/**
 * Account entity
 *
 * @author Xavier-Alexandre Brochard
 */
@InstanceEntity
@Entity
@Table(name = "t_account", uniqueConstraints = @UniqueConstraint(name = "uk_account_email", columnNames = { "email" }))
@SequenceGenerator(name = "accountSequence", initialValue = 1, sequenceName = "seq_account")
public class Account implements IIdentifiable<Long> {

    @Transient
    private static final int RANDOM_STRING_LENGTH = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "accountSequence")
    @Column(name = "id")
    private Long id;

    @Valid
    @Length(max = 128)
    @Email
    @Column(name = "email", length = 128)
    private String email;

    @Valid
    @Length(max = 128)
    @NotBlank
    @Column(name = "firstName", length = 128)
    private String firstName;

    @Valid
    @Length(max = 128)
    @NotBlank
    @Column(name = "lastName", length = 128)
    private String lastName;

    /**
     * invalidity date of the account
     */
    @Column
    private LocalDateTime invalidityDate;

    /**
     * By default an account is considered internal and not relying on an external identity service provider
     */
    @Column
    private Boolean external = false;

    @Column(name = "authentication_failed_counter")
    private Long authenticationFailedCounter = 0L;

    @Valid
    @Length(max = 255)
    @GsonIgnore
    @Column(name = "password", length = 255)
    private String password;

    /**
     * last password update date
     */
    @Column(name = "password_update_date")
    private LocalDateTime passwordUpdateDate;

    @NotNull
    @Column(name = "status", length = 20)
    @Enumerated(value = EnumType.STRING)
    private AccountStatus status;

    /**
     * Default empty constructor used by serializers
     */
    @SuppressWarnings("unused")
    private Account() {
        super();
        status = AccountStatus.PENDING;
    }

    /**
     * Creates new Account
     *
     * @param email
     *            the email
     * @param firstName
     *            the first name
     * @param lastName
     *            the last name
     * @param password
     *            the password
     */
    public Account(final String email, final String firstName, final String lastName, final String password) {
        super();
        status = AccountStatus.PENDING;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        setPassword(password);
    }

    /**
     * @return the id
     */
    @Override
    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    /**
     * @return the firstName
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * @param firstName
     *            the firstName to set
     */
    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    /**
     * @return the lastName
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * @param lastName
     *            the lastName to set
     */
    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password
     *            the password to set
     */
    public final void setPassword(final String password) {
        if (password != null) {
            passwordUpdateDate = LocalDateTime.now();
            this.password = password;
        }
    }

    /**
     * @return the status
     */
    public AccountStatus getStatus() {
        return status;
    }

    /**
     * @param status
     *            the status to set
     */
    public void setStatus(final AccountStatus status) {
        this.status = status;
    }

    /**
     * @return the last password update date
     */
    public LocalDateTime getPasswordUpdateDate() {
        return passwordUpdateDate;
    }

    public void setPasswordUpdateDate(final LocalDateTime passwordUpdateDate) {
        this.passwordUpdateDate = passwordUpdateDate;
    }

    /**
     * @return whether this account is external to REGARDS
     */
    public Boolean getExternal() {
        return external;
    }

    public void setExternal(final Boolean external) {
        this.external = external;
    }

    /**
     * @return the authentication failed counter
     */
    public Long getAuthenticationFailedCounter() {
        return authenticationFailedCounter;
    }

    public void setAuthenticationFailedCounter(final Long authenticationFailedCounter) {
        this.authenticationFailedCounter = authenticationFailedCounter;
    }

    /**
     * @return the account invalidity date
     */
    public LocalDateTime getInvalidityDate() {
        return invalidityDate;
    }

    public void setInvalidityDate(final LocalDateTime invalidityDate) {
        this.invalidityDate = invalidityDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        Account account = (Account) o;

        return email.equals(account.email);
    }

    @Override
    public int hashCode() {
        return email.hashCode();
    }
}
