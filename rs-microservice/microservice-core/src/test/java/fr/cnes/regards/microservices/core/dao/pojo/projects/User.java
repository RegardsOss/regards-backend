
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

@Entity(name = "T_USER")
@SequenceGenerator(name = "userSequence", initialValue = 1, sequenceName = "SEQ_USER")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "userSequence")
    @Column(name = "id")
    private Long id;

    @Column(name = "firstname")
    private String firstName;

    @Column(name = "lastname")
    private String lastName;

    @ManyToOne
    @JoinColumn(name = "company_id", foreignKey = @javax.persistence.ForeignKey(name = "FK_USER_COMPANY"))
    private Company company;

    public User() {

    }

    public User(String pFirstName, String pLastName) {
        super();
        this.firstName = pFirstName;
        this.lastName = pLastName;
    }

    public User(String pFirstName, String pLastName, Company pCompany) {
        super();
        this.firstName = pFirstName;
        this.lastName = pLastName;
        this.company = pCompany;
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param pId
     *            the id to set
     */
    public void setId(Long pId) {
        id = pId;
    }

    /**
     * @return the firstName
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * @return the lastName
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * @return the company
     */
    public Company getCompany() {
        return company;
    }

    /**
     * @param pFirstName
     *            the firstName to set
     */
    public void setFirstName(String pFirstName) {
        firstName = pFirstName;
    }

    /**
     * @param pLastName
     *            the lastName to set
     */
    public void setLastName(String pLastName) {
        lastName = pLastName;
    }

    /**
     * @param pCompany
     *            the company to set
     */
    public void setCompany(Company pCompany) {
        company = pCompany;
    }

}
