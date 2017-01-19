/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.instance.autoconfigure.pojo;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;

/**
 *
 * Class Project
 *
 * JPA Project Entity. For instance database.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Entity
@Table(name = "t_project")
@InstanceEntity
@SequenceGenerator(name = "projectSequence", initialValue = 1, sequenceName = "seq_project")
public class TestProject {

    /**
     * Project identifier
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "projectSequence")
    @Column(name = "id")
    private Long id;

    /**
     * Project name
     */
    @Column(name = "name")
    private String name;

    /**
     *
     * Getter
     *
     * @return Project identifier
     * @since 1.0-SNAPSHOT
     */
    public Long getId() {
        return id;
    }

    /**
     *
     * Setter
     *
     * @param pId
     *            Project identifier
     * @since 1.0-SNAPSHOT
     */
    public void setId(Long pId) {
        id = pId;
    }

    /**
     *
     * Getter
     *
     * @return Project name
     * @since 1.0-SNAPSHOT
     */
    public String getName() {
        return name;
    }

    /**
     *
     * Setter
     *
     * @param pName
     *            Project name
     * @since 1.0-SNAPSHOT
     */
    public void setName(String pName) {
        name = pName;
    }

}
