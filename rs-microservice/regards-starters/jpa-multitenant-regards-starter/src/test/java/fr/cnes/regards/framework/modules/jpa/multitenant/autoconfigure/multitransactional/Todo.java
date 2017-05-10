
/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.jpa.multitenant.autoconfigure.multitransactional;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Basic pojo to test transaction synchronization
 *
 * @author Marc Sordi
 *
 */
@Entity
@Table(name = "t_todo")
@SequenceGenerator(name = "todoSequence", initialValue = 1, sequenceName = "seq_todo")
public class Todo {

    /**
     * User identifier
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "todoSequence")
    @Column(name = "id")
    private Long id;

    /**
     * Todo label
     */
    @Column(unique = true)
    private String label;

    public Long getId() {
        return id;
    }

    public void setId(Long pId) {
        id = pId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String pLabel) {
        label = pLabel;
    }

    @Override
    public boolean equals(Object pObj) {
        if (pObj instanceof Todo) {
            Todo todo = (Todo) pObj;
            return label.equals(todo.getLabel());
        }
        return false;
    }
}
