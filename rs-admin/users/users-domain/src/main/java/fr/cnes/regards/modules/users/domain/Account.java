package fr.cnes.regards.modules.users.domain;

import java.util.ArrayList;
import java.util.List;

import org.springframework.hateoas.ResourceSupport;

public class Account extends ResourceSupport {

    private String email_;

    private String firstName_;

    private String lastName_;

    private String login_;

    private String password_;

    private AccountStatus status_;

    private List<ProjectUser> projectUsers_;

    public Account() {
        super();
        this.projectUsers_ = new ArrayList<>();
        this.status_ = AccountStatus.PENDING;
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

    public List<ProjectUser> getProjectUsers() {
        return projectUsers_;
    }

    public void setProjectUsers(List<ProjectUser> pProjectUsers) {
        projectUsers_ = pProjectUsers;
    }

}
