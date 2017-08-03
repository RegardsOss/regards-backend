package fr.cnes.regards.framework.modules.jobs.domain;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.hibernate.annotations.Type;

/**
 * Job parameter ie a name/value pair
 * @author oroussel
 */
@Embeddable
public class JobParameter {

    @Column(length = 100, nullable = false)
    private String name;

    @Type(type = "text")
    private String value;

    public JobParameter() {
    }

    public JobParameter(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JobParameter that = (JobParameter) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
