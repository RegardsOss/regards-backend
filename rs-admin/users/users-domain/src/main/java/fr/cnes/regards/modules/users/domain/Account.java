package fr.cnes.regards.modules.users.domain;

import javax.validation.constraints.NotNull;

import org.springframework.hateoas.ResourceSupport;

public class Account extends ResourceSupport {

    @NotNull
    private String email_;

    private String firstName_;

    private String lastName_;

    @NotNull
    private String login_;

    // TODO: validation du mot de passe
    private String password_;

    private AccountStatus status_;

    public Account() {
        super();
        this.status_ = AccountStatus.PENDING;
    }

    public Account(String email, String firstName, String lastName, String password) {
        this();
        this.email_ = email;
        this.firstName_ = firstName;
        this.lastName_ = lastName;
        this.login_ = email;
        this.password_ = password;
    }

    public Account(String email, String firstName, String lastName, String login, String password) {
        this();
        this.email_ = email;
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

}
