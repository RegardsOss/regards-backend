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
package fr.cnes.regards.modules.accessrights.instance.domain;

import fr.cnes.regards.framework.gson.annotation.GsonIgnore;
import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.modules.project.domain.Project;
import org.hibernate.validator.constraints.Length;

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Set;

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

    public static final String REGARDS_ORIGIN = "Regards";

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
    @Column(name = "firstname", length = 128)
    private String firstName;

    @Valid
    @Length(max = 128)
    @NotBlank
    @Column(name = "lastname", length = 128)
    private String lastName;

    @Column
    private LocalDateTime invalidityDate;

    @Length(max = 128)
    @Column(length = 128)
    private String origin;

    @Column(name = "authentication_failed_counter")
    private Long authenticationFailedCounter = 0L;

    @Valid
    @Length(max = 255)
    @GsonIgnore
    @Column(name = "password")
    private String password;

    @Column(name = "password_update_date")
    private LocalDateTime passwordUpdateDate;

    @NotNull
    @Column(name = "status", length = 20)
    @Enumerated(value = EnumType.STRING)
    private AccountStatus status;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "ta_account_project", joinColumns = @JoinColumn(name = "account_id"),
        foreignKey = @ForeignKey(name = "fk_account_project__account_id"),
        inverseJoinColumns = @JoinColumn(name = "project_id"),
        inverseForeignKey = @ForeignKey(name = "fk_account_project__project_id"))
    private Set<Project> projects;

    /**
     * Default empty constructor used by serializers
     */
    @SuppressWarnings("unused")
    private Account() {
        super();
        status = AccountStatus.PENDING;
    }

    public Account(final String email, final String firstName, final String lastName, final String password) {
        status = AccountStatus.PENDING;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        setPassword(password);
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        if (password != null) {
            passwordUpdateDate = LocalDateTime.now();
            this.password = password;
        }
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(final AccountStatus status) {
        this.status = status;
    }

    public LocalDateTime getPasswordUpdateDate() {
        return passwordUpdateDate;
    }

    public void setPasswordUpdateDate(final LocalDateTime passwordUpdateDate) {
        this.passwordUpdateDate = passwordUpdateDate;
    }

    public boolean isExternal() {
        return !REGARDS_ORIGIN.equals(getOrigin());
    }

    public Long getAuthenticationFailedCounter() {
        return authenticationFailedCounter;
    }

    public void setAuthenticationFailedCounter(final Long authenticationFailedCounter) {
        this.authenticationFailedCounter = authenticationFailedCounter;
    }

    public LocalDateTime getInvalidityDate() {
        return invalidityDate;
    }

    public void setInvalidityDate(final LocalDateTime invalidityDate) {
        this.invalidityDate = invalidityDate;
    }

    public Set<Project> getProjects() {
        return projects;
    }

    public Account setProjects(Set<Project> projectList) {
        this.projects = projectList;
        return this;
    }

    public String getOrigin() {
        return origin;
    }

    public Account setOrigin(String origin) {
        this.origin = origin;
        return this;
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
