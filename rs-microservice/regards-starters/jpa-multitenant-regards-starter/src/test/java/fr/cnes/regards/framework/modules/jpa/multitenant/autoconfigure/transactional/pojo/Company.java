/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.jpa.multitenant.autoconfigure.transactional.pojo;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 *
 * Class Company
 *
 * JPA Company Entity. For projects multitenancy databases.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Entity
@Table(name = "t_company")
@SequenceGenerator(name = "companySequence", initialValue = 1, sequenceName = "seq_company")
public class Company {

    /**
     * Company identifier
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "companySequence")
    @Column(name = "id")
    private Long id;

    /**
     * Company name
     */
    @Column(name = "name")
    private String name;

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
        name = pName;
    }

    /**
     *
     * Getter
     *
     * @return Company identifier
     * @since 1.0-SNPASHOT
     */
    public Long getId() {
        return id;
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
        id = pId;
    }

    /**
     *
     * Getter
     *
     * @return Company name
     * @since 1.0-SNPASHOT
     */
    public String getName() {
        return name;
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
        name = pName;
    }

}
