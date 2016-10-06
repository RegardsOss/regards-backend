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
 * Test class for JPA Foreign keys
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Entity(name = "T_COMPANY")
@SequenceGenerator(name = "companySequence", initialValue = 1, sequenceName = "SEQ_COMPANY")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "companySequence")
    @Column(name = "id")
    private Long id_;

    @Column(name = "name")
    private String name_;

    public Company() {

    }

    public Company(String pName) {
        super();
        name_ = pName;
    }

    public Long getId() {
        return id_;
    }

    public void setId(Long pId) {
        id_ = pId;
    }

    public String getName() {
        return name_;
    }

    public void setName(String pName) {
        name_ = pName;
    }

}
