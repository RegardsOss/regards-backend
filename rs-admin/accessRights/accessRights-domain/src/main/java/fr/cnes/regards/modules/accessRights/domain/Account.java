/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.domain;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.hateoas.Identifiable;

public class Account implements Identifiable<Long> {

    private static Long maxAccountId_ = 0L;

    @NotNull
    private Long id_;

    public void setAccountId(Long pAccountId) {
        id_ = pAccountId;
    }

    @Email
    private String email_;

    @NotBlank
    private String firstName_;

    @NotBlank
    private String lastName_;

    @NotBlank
    private String login_;

    @NotBlank
    // TODO: validation du mot de passe
    private String password_;

    @NotNull
    private AccountStatus status_;

    public Account() {
        super();
        this.id_ = maxAccountId_;
        maxAccountId_++;
        this.status_ = AccountStatus.PENDING;
    }

    public Account(String email) {
        this();
        this.email_ = email;
    }

    public Account(String email, String firstName, String lastName, String password) {
        this(email);
        this.firstName_ = firstName;
        this.lastName_ = lastName;
        this.login_ = email;
        this.password_ = password;
    }

    public Account(String email, String firstName, String lastName, String login, String password) {
        this(email);
        this.firstName_ = firstName;
        this.lastName_ = lastName;
        this.login_ = login;
        this.password_ = password;
    }

    public String getEmail() {
        return email_;
    }

    public void setEmail(String pEmail) {
        email_ = pEmail;
    }

    public String getFirstName() {
        return firstName_;
    }

    public void setFirstName(String pFirstName) {
        firstName_ = pFirstName;
    }

    public String getLastName() {
        return lastName_;
    }

    public void setLastName(String pLastName) {
        lastName_ = pLastName;
    }

    public String getLogin() {
        return login_;
    }

    public void setLogin(String pLogin) {
        login_ = pLogin;
    }

    public String getPassword() {
        return password_;
    }

    public void setPassword(String pPassword) {
        password_ = pPassword;
    }

    public AccountStatus getStatus() {
        return status_;
    }

    public void setStatus(AccountStatus pStatus) {
        status_ = pStatus;
    }

    public void unlock() {
        if (this.status_.equals(AccountStatus.LOCKED)) {
            this.status_ = AccountStatus.ACTIVE;
        }
    }

    @Override
    public Long getId() {
        return id_;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof Account) && ((Account) o).email_.equals(this.email_);
    }

}
