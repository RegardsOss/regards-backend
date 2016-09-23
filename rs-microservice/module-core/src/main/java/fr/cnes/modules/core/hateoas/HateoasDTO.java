/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.modules.core.hateoas;

import org.springframework.hateoas.ResourceSupport;

public class HateoasDTO<T> extends ResourceSupport {

    private T resource_;

    public HateoasDTO(T pResource) {
        super();
        resource_ = pResource;
    }

    public T getResource() {
        return resource_;
    }

    public void setResource(T pResource) {
        resource_ = pResource;
    }

}
