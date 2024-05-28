package fr.cnes.regards.modules.accessrights.instance.domain;

import jakarta.validation.Valid;
import org.hibernate.validator.constraints.Length;

import java.util.Objects;

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

    @Valid
    @Length(max = 128)
    private String project;

    public AccountNPassword() {
    }

    public AccountNPassword(Account account, String password) {
        this.account = account;
        this.password = password;
    }

    public AccountNPassword(Account account, String password, String project) {
        this.account = account;
        this.password = password;
        this.project = project;
    }

    public Account getAccount() {
        return account;
    }

    public AccountNPassword setAccount(Account account) {
        this.account = account;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public AccountNPassword setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getProject() {
        return project;
    }

    public AccountNPassword setProject(String project) {
        this.project = project;
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

        AccountNPassword that = (AccountNPassword) o;

        if (!Objects.equals(account, that.account)) {
            return false;
        }
        return Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        int result = account != null ? account.hashCode() : 0;
        result = (31 * result) + (password != null ? password.hashCode() : 0);
        return result;
    }
}
