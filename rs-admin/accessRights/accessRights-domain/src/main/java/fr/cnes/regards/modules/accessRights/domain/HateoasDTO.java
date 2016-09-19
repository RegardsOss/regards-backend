package fr.cnes.regards.modules.accessRights.domain;

import org.springframework.hateoas.ResourceSupport;

public class HateoasDTO<T> extends ResourceSupport {

    T resource_;

    public T getResource() {
        return resource_;
    }

    public void setResource(T pResource) {
        resource_ = pResource;
    }

}
