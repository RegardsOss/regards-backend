package fr.cnes.regards.modules.accessrights.instance.domain;

import javax.validation.Valid;

import org.hibernate.validator.constraints.NotBlank;

/**
 * DTO used to comunicate between rs-admin and rs-admin-instance. This allows us to pass the password for account creation which is not serialized otherwise.
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public class AccountNPassword {

    @Valid
    private Account account;

    private String password;

    public AccountNPassword() {
    }

    public AccountNPassword(Account account, String password) {
        this.account = account;
        this.password = password;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
