/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.json;

import java.lang.reflect.Type;

import org.hibernate.type.descriptor.java.MutableMutabilityPlan;

/**
 * @author Marc Sordi
 *
 */
@SuppressWarnings("serial")
public class JsonBinaryMutableMutabilityPlan extends MutableMutabilityPlan<Object> {

    /**
     * JAVA object type : may be simple class or parameterized type
     */
    private transient Type type;

    public JsonBinaryMutableMutabilityPlan() {
        super();
    }

    /* (non-Javadoc)
     * @see org.hibernate.type.descriptor.java.MutableMutabilityPlan#deepCopyNotNull(java.lang.Object)
     */
    @Override
    protected Object deepCopyNotNull(Object value) {
        Type currentType = type == null ? value.getClass() : type;
        return GsonUtil.clone(value, currentType);
    }

    public void setType(Type type) {
        this.type = type;
    }

}
