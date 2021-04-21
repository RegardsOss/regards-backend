package fr.cnes.regards.modules.accessrights.instance.domain;

import javax.validation.Valid;

import org.hibernate.validator.constraints.Length;

/**
 * DTO used to comunicate between rs-admin and rs-admin-instance. This allows us to pass the password for account creation which is not serialized otherwise.
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public class AccountNPassword {

    @Valid
    private Account account;

    @Valid
    @Length(max = 255)
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        AccountNPassword that = (AccountNPassword) o;

        if (account != null ? !account.equals(that.account) : that.account != null) {
            return false;
        }
        return password != null ? password.equals(that.password) : that.password == null;
    }

    @Override
    public int hashCode() {
        int result = account != null ? account.hashCode() : 0;
        result = (31 * result) + (password != null ? password.hashCode() : 0);
        return result;
    }
}
