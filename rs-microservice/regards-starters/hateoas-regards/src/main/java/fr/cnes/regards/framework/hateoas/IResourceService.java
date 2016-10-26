/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.hateoas;

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
}
