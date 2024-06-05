package fr.cnes.regards.framework.modules.jobs.domain;

import fr.cnes.regards.framework.jpa.json.GsonUtil;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Job parameter ie a name/value pair.
 * Value must not be a parametered type !!
 *
 * @author oroussel
 */
@Embeddable
public class JobParameter {

    @Column(length = 100, nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String value;

    @Column(name = "class_name", length = 255)
    private String className;

    public JobParameter() {
    }

    public <T> JobParameter(String name, T value) {
        this.name = name;
        this.setValue(value);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public <T> T getValue() {
        try {
            return getValue(Class.forName(className));
        } catch (ClassNotFoundException e) {
            throw new RsRuntimeException(e);
        }
    }

    public <T> T getValue(java.lang.reflect.Type type) {
        return type == null ? null : GsonUtil.fromString(value, type);
    }

    /**
     * Set value. Because this value is gsonified, it must not be a parametered type.
     * If yout want one, feel free to create a specific class inheriting parametered type
     */
    public final <T> void setValue(T value) {
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
