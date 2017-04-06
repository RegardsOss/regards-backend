/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.hateoas;

import org.springframework.cglib.core.Converter;
import org.springframework.hateoas.Resource;
import org.springframework.util.Assert;

/**
 *
 * Hypermedia resource service
 *
 * @author msordi
 *
 */
public interface IResourceService {

    /**
     * Convert object to resource
     *
     * @param <T>
     *            element to convert
     * @param pObject
     *            object
     * @return {@link Resource}
     */
    default <T> Resource<T> toResource(T pObject) {
        Assert.notNull(pObject);
        return new Resource<T>(pObject);
    }

    /**
     * Add a link to a resource for a single method
     *
     * @param <T>
     *            resource content type
     * @param pResource
     *            resource to manage
     * @param pController
     *            controller
     * @param pMethodName
     *            method name
     * @param pRel
     *            rel name
     * @param pMethodParams
     *            method parameters
     */
    <T> void addLink(Resource<T> pResource, Class<?> pController, String pMethodName, String pRel,
            MethodParam<?>... pMethodParams);

    /**
     * Custom way of adding link to a resource handling request params.
     *
     * For example, an endpoint like
     * getSomething(@RequestParam String name)
     * mapped to: "/something"
     * will generate a link like
     * "http://someting?name=myName"
     *
     * BUT!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     * It cannot handle type conversion, even if you declared the correct Spring {@link Converter}.
     *
     * For example, an endpoint like
     * getSomething(@RequestParam ComplexEntity entity)
     * mapped to: "/something"
     * will generate a conversion error, telling that it could not find the appropriate converter,
     * even if you defined in your classpath a converter implementing Converter<ComplexEntity, String>
     *
     * @param <T> resource content type
     * @param <C> controller type
     * @param pResource resource to manage
     * @param pController controller
     * @param pMethodName method name
     * @param pRel rel name
     * @param pMethodParams method parameters
     */
    <T, C> void addLinkWithParams(Resource<T> pResource, Class<C> pController, String pMethodName, String pRel,
            MethodParam<?>... pMethodParams);
}
