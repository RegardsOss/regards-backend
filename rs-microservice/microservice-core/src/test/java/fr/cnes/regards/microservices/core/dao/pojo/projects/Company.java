/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.dao.pojo.projects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

/**
 *
 * Class Company
 *
 * JPA Company Entity. For projects multitenancy databases.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Entity(name = "T_COMPANY")
@SequenceGenerator(name = "companySequence", initialValue = 1, sequenceName = "SEQ_COMPANY")
public class Company {

    /**
     * Company identifier
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "companySequence")
    @Column(name = "id")
    private Long id_;

    /**
     * Company name
     */
    @Column(name = "name")
    private String name_;

    /**
     *
     * Constructor
     *
     * @since 1.0-SNAPSHOT
     */
    public Company() {

    }

    /**
     *
     * Constructor
     *
     * @param pName
     *            Company name
     * @since 1.0-SNAPSHOT
     */
    public Company(String pName) {
        super();
        name_ = pName;
    }

    /**
     *
     * Getter
     *
     * @return Company identifier
     * @since 1.0-SNPASHOT
     */
    public Long getId() {
        return id_;
    }

    /**
     *
     * Setter
     *
     * @param pId
     *            Company identifier
     * @since 1.0-SNAPSHOT
     */
    public void setId(Long pId) {
        id_ = pId;
    }

    /**
     *
     * Getter
     *
     * @return Company name
     * @since 1.0-SNPASHOT
     */
    public String getName() {
        return name_;
    }

    /**
     *
     * Setter
     *
     * @param pName
     *            Company name
     * @since 1.0-SNAPSHOT
     */
    public void setName(String pName) {
        name_ = pName;
    }

}
