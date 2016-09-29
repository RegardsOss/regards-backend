/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.domain;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.hateoas.Identifiable;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Account implements Identifiable<Long> {

    private static Long maxAccountId_ = 0L;

    @NotNull
    private Long id_;

    @NotNull
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

    @JsonIgnore
    private String code_;

    public Account() {
        super();
        id_ = maxAccountId_;
        maxAccountId_++;
        status_ = AccountStatus.PENDING;
    }

    public Account(String pEmail) {
        this();
        email_ = pEmail;
        login_ = pEmail;
    }

    public Account(String pEmail, String pFirstName, String pLastName, String pPassword) {
        this(pEmail);
        firstName_ = pFirstName;
        lastName_ = pLastName;
        password_ = pPassword;
    }

    public Account(String pEmail, String pFirstName, String pLastName, String pLogin, String pPassword) {
        this(pEmail);
        firstName_ = pFirstName;
        lastName_ = pLastName;
        login_ = pLogin;
        password_ = pPassword;
    }

    public Account(Long pId, String pEmail, String pFirstName, String pLastName, String pLogin, String pPassword,
            AccountStatus pStatus, String pCode) {
        super();
        id_ = pId;
        email_ = pEmail;
        firstName_ = pFirstName;
        lastName_ = pLastName;
        login_ = pLogin;
        password_ = pPassword;
        status_ = pStatus;
        code_ = pCode;
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
        if (status_.equals(AccountStatus.LOCKED)) {
            status_ = AccountStatus.ACTIVE;
        }
    }

    public void setAccountId(Long pAccountId) {
        id_ = pAccountId;
    }

    @Override
    public Long getId() {
        return id_;
    }

    public void setId(Long pId) {
        id_ = pId;
    }

    public String getCode() {
        return code_;
    }

    public void setCode(String pCode) {
        code_ = pCode;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof Account) && ((Account) o).email_.equals(email_);
    }

}
