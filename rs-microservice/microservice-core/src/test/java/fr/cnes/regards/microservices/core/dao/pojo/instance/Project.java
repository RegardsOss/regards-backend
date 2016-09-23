package fr.cnes.regards.microservices.core.dao.pojo.instance;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import fr.cnes.regards.microservices.core.dao.annotation.InstanceEntity;

@Entity(name = "T_PROJECT")
@InstanceEntity
@SequenceGenerator(name = "projectSequence", initialValue = 1, sequenceName = "SEQ_PROJECT")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "projectSequence")
    @Column(name = "id")
    private Long id;

    @Column(name = "firstname")
    private String firstName;

    public Long getId() {
        return id;
    }

    public void setId(Long pId) {
        id = pId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String pFirstName) {
        firstName = pFirstName;
    }

}
