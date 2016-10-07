/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.domain.instance;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.hateoas.Identifiable;

import com.fasterxml.jackson.annotation.JsonIgnore;

import fr.cnes.regards.domain.annotation.InstanceEntity;
import fr.cnes.regards.modules.accessRights.domain.AccountStatus;

@InstanceEntity
@Entity(name = "T_ACCOUNT")
@SequenceGenerator(name = "accountSequence", initialValue = 1, sequenceName = "SEQ_ACCOUNT")
public class Account implements Identifiable<Long> {

    // TODO : Remove this useless attribute and add the id management in stub for test
    @Transient
    private static Long maxAccountId = 0L;

    @NotNull
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "accountSequence")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Email
    @Column(name = "email")
    private String email;

    @NotBlank
    @Column(name = "firstName")
    private String firstName;

    @NotBlank
    @Column(name = "lastName")
    private String lastName;

    @NotBlank
    @Column(name = "login")
    private String login;

    @NotBlank
    @Column(name = "password")
    // TODO: validation du mot de passe
    private String password;

    @NotNull
    @Column(name = "status")
    private AccountStatus status;

    @JsonIgnore
    @Column(name = "code")
    private String code;

    public Account() {
        super();
        status = AccountStatus.PENDING;
    }

    public Account(final String pEmail) {
        this();
        id = maxAccountId;
        maxAccountId++;
        email = pEmail;
        login = pEmail;
    }

    public Account(final String pEmail, final String pFirstName, final String pLastName, final String pPassword) {
        this(pEmail);
        firstName = pFirstName;
        lastName = pLastName;
        password = pPassword;
    }

    public Account(final String pEmail, final String pFirstName, final String pLastName, final String pLogin,
            final String pPassword) {
        this(pEmail);
        firstName = pFirstName;
        lastName = pLastName;
        login = pLogin;
        password = pPassword;
    }

    public Account(final Long pId, final String pEmail, final String pFirstName, final String pLastName,
            final String pLogin, final String pPassword,
            final AccountStatus pStatus, final String pCode) {
        super();
        id = pId;
        email = pEmail;
        firstName = pFirstName;
        lastName = pLastName;
        login = pLogin;
        password = pPassword;
        status = pStatus;
        code = pCode;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String pEmail) {
        email = pEmail;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(final String pFirstName) {
        firstName = pFirstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(final String pLastName) {
        lastName = pLastName;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(final String pLogin) {
        login = pLogin;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String pPassword) {
        password = pPassword;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(final AccountStatus pStatus) {
        status = pStatus;
    }

    public void unlock() {
        if (status.equals(AccountStatus.LOCKED)) {
            status = AccountStatus.ACTIVE;
        }
    }

    public void setAccountId(final Long pAccountId) {
        id = pAccountId;
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(final Long pId) {
        id = pId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String pCode) {
        code = pCode;
    }

    @Override
    public boolean equals(final Object o) {
        return (o instanceof Account) && ((Account) o).email.equals(email);
    }

}
