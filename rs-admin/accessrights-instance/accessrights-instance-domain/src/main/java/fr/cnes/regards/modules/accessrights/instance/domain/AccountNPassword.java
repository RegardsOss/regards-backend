package fr.cnes.regards.modules.accessrights.instance.domain;

import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "The initial password of the account.")
    @Valid
    @Length(max = 255)
    private String password;

    @Schema(description = "An optional project to associate the account to.")
    @Valid
    @Length(max = 128)
    private String project;

    @Schema(description = "The URL of the app from where the account creation request was issued. This field is "
                          + "mandatory only if the account status is left unspecified, or set to `EMAIL_VERIFICATION`.")
    private String originUrl;

    @Schema(description = "The URL to redirect the user to the password verification interface. This field is "
                          + "mandatory only if the account status is left unspecified, or set to `EMAIL_VERIFICATION`.")
    private String requestLink;

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

    public String getOriginUrl() {
        return originUrl;
    }

    public void setOriginUrl(String originUrl) {
        this.originUrl = originUrl;
    }

    public String getRequestLink() {
        return requestLink;
    }

    public void setRequestLink(String requestLink) {
        this.requestLink = requestLink;
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
        if (!Objects.equals(password, that.password)) {
            return false;
        }
        if (!Objects.equals(originUrl, that.originUrl)) {
            return false;
        }
        return Objects.equals(requestLink, that.requestLink);
    }

    @Override
    public int hashCode() {
        int result = account != null ? account.hashCode() : 0;
        result = (31 * result) + (password != null ? password.hashCode() : 0);
        return result;
    }

}
