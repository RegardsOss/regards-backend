/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.security.endpoint;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.regex.Pattern;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.hateoas.core.AnnotationMappingDiscoverer;
import org.springframework.hateoas.core.MappingDiscoverer;
import org.springframework.util.Assert;

/**
 * Customization of the {@link AnnotationMappingDiscoverer} with fine-grained support for meta-annotations with
 * attribute overrides in composed annotations The implementation is identical except for the method used for finding
 * Annotations, as we replace {@link AnnotationUtils.findAnnotation} with
 * {@link AnnotatedElementUtils.findMergedAnnotation}
 *
 * @author xbrochard
 */
public class AnnotatedElementMappingDiscoverer implements MappingDiscoverer {

    private static final Pattern MULTIPLE_SLASHES = Pattern.compile("\\/{2,}");

    private final Class<? extends Annotation> annotationType;

    private final String mappingAttributeName;

    /**
     * Creates an {@link AnnotatedElementMappingDiscoverer} for the given annotation type. Will lookup the {@code value}
     * attribute by default.
     *
     * @param annotation
     *            must not be {@literal null}.
     */
    public AnnotatedElementMappingDiscoverer(Class<? extends Annotation> annotation) {
        this(annotation, null);
    }

    /**
     * Creates an {@link AnnotatedElementMappingDiscoverer} for the given annotation type and attribute name.
     *
     * @param annotation
     *            must not be {@literal null}.
     * @param mappingAttributeName
     *            if {@literal null}, it defaults to {@code value}.
     */
    public AnnotatedElementMappingDiscoverer(Class<? extends Annotation> annotation, String mappingAttributeName) {

        Assert.notNull(annotation);

        this.annotationType = annotation;
        this.mappingAttributeName = mappingAttributeName;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.hateoas.core.MappingDiscoverer#getMapping(java.lang.Class)
     */
    @Override
    public String getMapping(Class<?> type) {

        Assert.notNull(type, "Type must not be null!");

        String[] mapping = getMappingFrom(AnnotatedElementUtils.findMergedAnnotation(type, annotationType));

        return mapping.length == 0 ? null : mapping[0];
    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.hateoas.core.MappingDiscoverer#getMapping(java.lang.reflect.Method)
     */
    @Override
    public String getMapping(Method method) {

        Assert.notNull(method, "Method must not be null!");
        return getMapping(method.getDeclaringClass(), method);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.hateoas.core.MappingDiscoverer#getMapping(java.lang.Class, java.lang.reflect.Method)
     */
    @Override
    public String getMapping(Class<?> type, Method method) {

        Assert.notNull(type, "Type must not be null!");
        Assert.notNull(method, "Method must not be null!");

        // AnnotationUtils.synthesizeAnnotation(annotationType, method);

        String[] mapping = getMappingFrom(AnnotatedElementUtils.findMergedAnnotation(method, annotationType));
        String typeMapping = getMapping(type);

        if ((mapping == null) || (mapping.length == 0)) {
            return typeMapping;
        }

        return (typeMapping == null) || "/".equals(typeMapping) ? mapping[0] : join(typeMapping, mapping[0]);
    }

    private String[] getMappingFrom(Annotation annotation) {

        Object value = mappingAttributeName == null ? AnnotationUtils.getValue(annotation)
                : AnnotationUtils.getValue(annotation, mappingAttributeName);

        if (value instanceof String) {
            return new String[] { (String) value };
        }
        else
            if (value instanceof String[]) {
                return (String[]) value;
            }
            else
                if (value == null) {
                    return new String[0];
                }

        throw new IllegalStateException(
                String.format("Unsupported type for the mapping attribute! Support String and String[] but got %s!",
                              value.getClass()));
    }

    /**
     * Joins the given mappings making sure exactly one slash.
     *
     * @param typeMapping
     *            must not be {@literal null} or empty.
     * @param mapping
     *            must not be {@literal null} or empty.
     * @return
     */
    private static String join(String typeMapping, String mapping) {
        return MULTIPLE_SLASHES.matcher(typeMapping.concat("/").concat(mapping)).replaceAll("/");
    }

}
