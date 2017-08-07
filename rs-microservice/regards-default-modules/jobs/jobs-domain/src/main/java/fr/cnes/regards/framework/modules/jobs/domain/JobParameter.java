package fr.cnes.regards.framework.modules.jobs.domain;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.hibernate.annotations.Type;

import fr.cnes.regards.framework.jpa.json.GsonUtil;

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

    @Column(name = "class_name", length = 255)
    private String className;

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

    public <T> T getValue() {
        try {
            return (className == null) ? null : GsonUtil.fromString(value, Class.forName(className));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e); // NOSONAR
        }
    }

    public void setValue(String value) {
        if (value != null) {
            this.className = value.getClass().getName();
        }
        this.value = GsonUtil.toString(value);

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
