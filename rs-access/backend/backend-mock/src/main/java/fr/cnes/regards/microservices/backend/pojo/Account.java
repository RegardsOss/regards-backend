package fr.cnes.regards.microservices.backend.pojo;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.springframework.hateoas.ResourceSupport;

import java.util.ArrayList;
import java.util.List;

public class Account extends ResourceSupport {
    private Long accountId;
    private String email;
    private String firstName;
    private String lastName;
    private String login;

    @JsonIgnore
    private String password;
    private int status;

    private List<ProjectUser> projectUsers;

	public Account(Long accountId, String firstName, String lastName, String email, String login, String password) {
        this.accountId = accountId;
        this.firstName = firstName;
        this.lastName = lastName;
	    this.email = email;
        this.login = login;
        this.password = password;
        this.status = AccountStatus.ACTIVE.getValue();
        this.projectUsers = new ArrayList();
	}

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public List<ProjectUser> getProjectUsers() {
        return projectUsers;
    }

    public void setProjectUsers(List<ProjectUser> projectUsers) {
        this.projectUsers = projectUsers;
    }
}
