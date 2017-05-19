/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.person;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * {@link Person} test entity with a generated sequence
 *
 * @author Marc Sordi
 *
 */
@Entity
@Table(name = "t_person")
@SequenceGenerator(name = "personSequence", initialValue = 1, sequenceName = "seq_person")
public class Person {

    /**
     * User identifier
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "personSequence")
    @Column(name = "id")
    private Long id;

    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long pId) {
        id = pId;
    }

    public String getName() {
        return name;
    }

    public void setName(String pName) {
        name = pName;
    }
}
