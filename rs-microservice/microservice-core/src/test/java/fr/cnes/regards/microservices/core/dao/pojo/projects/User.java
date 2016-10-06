
/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.dao.pojo.projects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;

/**
 *
 * Class User
 *
 * JPA Company Entity. For projects multitenancy databases.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Entity(name = "T_USER")
@SequenceGenerator(name = "userSequence", initialValue = 1, sequenceName = "SEQ_USER")
public class User {

    /**
     * User identifier
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "userSequence")
    @Column(name = "id")
    private Long id_;

    /**
     * User first name
     */
    @Column(name = "firstname")
    private String firstName_;

    /**
     * User last name
     */
    @Column(name = "lastname")
    private String lastName_;

    /**
     * User's company
     */
    @ManyToOne
    @JoinColumn(name = "company_id", foreignKey = @javax.persistence.ForeignKey(name = "FK_USER_COMPANY"))
    private Company company_;

    /**
     *
     * Constructor
     *
     * @since 1.0-SNAPSHOT
     */
    public User() {

    }

    /**
     *
     * Constructor
     *
     * @param pFirstName
     *            User first name
     * @param pLastName
     *            User last name
     * @since 1.0-SNAPSHOT
     */
    public User(String pFirstName, String pLastName) {
        super();
        this.firstName_ = pFirstName;
        this.lastName_ = pLastName;
    }

    /**
     *
     * Constructor
     *
     * @param pFirstName
     *            User first name
     * @param pLastName
     *            User last name
     * @param pCompany
     *            User's Company
     * @since 1.0-SNAPSHOT
     */
    public User(String pFirstName, String pLastName, Company pCompany) {
        super();
        this.firstName_ = pFirstName;
        this.lastName_ = pLastName;
        this.company_ = pCompany;
    }

    /**
     * Getter
     *
     * @return User identifier
     */
    public Long getId() {
        return id_;
    }

    /**
     * Setter
     *
     * @param pId
     *            User identifier
     */
    public void setId(Long pId) {
        id_ = pId;
    }

    /**
     * Getter
     *
     * @return User firstName
     */
    public String getFirstName() {
        return firstName_;
    }

    /**
     * Getter
     *
     * @return User lastName
     */
    public String getLastName() {
        return lastName_;
    }

    /**
     * Getter
     *
     * @return User's company
     */
    public Company getCompany() {
        return company_;
    }

    /**
     * Setter
     *
     * @param pFirstName
     *            User firstName
     */
    public void setFirstName(String pFirstName) {
        firstName_ = pFirstName;
    }

    /**
     * Setter
     *
     * @param pLastName
     *            User lastName
     */
    public void setLastName(String pLastName) {
        lastName_ = pLastName;
    }

    /**
     * Setter
     *
     * @param pCompany
     *            User's company
     */
    public void setCompany(Company pCompany) {
        company_ = pCompany;
    }

}
