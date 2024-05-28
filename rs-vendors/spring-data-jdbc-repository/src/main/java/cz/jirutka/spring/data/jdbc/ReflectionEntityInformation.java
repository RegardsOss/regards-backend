package cz.jirutka.spring.data.jdbc;

import jakarta.annotation.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.core.support.AbstractEntityInformation;
import org.springframework.data.util.AnnotationDetectionFieldCallback;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * @author Olivier Rousselot
 */
public class ReflectionEntityInformation<T, ID> extends AbstractEntityInformation<T, ID> {
    private static final Class<Id> DEFAULT_ID_ANNOTATION = Id.class;

    private Field field;

    /**
     * Creates a new {@link ReflectionEntityInformation} inspecting the given domain class for a field carrying the
     * {@link Id} annotation.
     *
     * @param domainClass must not be {@literal null}.
     */
    public ReflectionEntityInformation(Class<T> domainClass) {
        this(domainClass, DEFAULT_ID_ANNOTATION);
    }

    /**
     * Creates a new {@link ReflectionEntityInformation} inspecting the given domain class for a field carrying the given
     * annotation.
     *
     * @param domainClass must not be {@literal null}.
     * @param annotation must not be {@literal null}.
     */
    public ReflectionEntityInformation(Class<T> domainClass, final Class<? extends Annotation> annotation) {

        super(domainClass);

        Assert.notNull(annotation, "Annotation must not be null!");

        AnnotationDetectionFieldCallback callback = new AnnotationDetectionFieldCallback(annotation);
        ReflectionUtils.doWithFields(domainClass, callback);

        try {
            this.field = callback.getRequiredField();
        } catch (IllegalStateException o_O) {
            throw new IllegalArgumentException(String.format("Couldn't find field with annotation %s!", annotation), o_O);
        }

        ReflectionUtils.makeAccessible(this.field);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.core.EntityInformation#getId(java.lang.Object)
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public ID getId(Object entity) {

        Assert.notNull(entity, "Entity must not be null!");

        return (ID) ReflectionUtils.getField(field, entity);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.core.EntityInformation#getIdType()
     */
    @SuppressWarnings("unchecked")
    public Class<ID> getIdType() {
        return (Class<ID>) field.getType();
    }
}
