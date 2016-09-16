/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.domain;

import javax.validation.constraints.NotNull;

import org.springframework.hateoas.ResourceSupport;

public class ResourcesAccess extends ResourceSupport {

    @NotNull
    private final Integer resourcesAccessId_;

    private String description_;

    @NotNull
    private String microservice_;

    @NotNull
    private String resource_;

    @NotNull
    private HttpVerb verb_;

    public ResourcesAccess(Integer pResourcesAccessId) {
        resourcesAccessId_ = pResourcesAccessId;
        verb_ = HttpVerb.GET;
    }

    public ResourcesAccess(Integer pResourcesAccessId, String pDescription, String pMicroservice, String pResource,
            HttpVerb pVerb) {
        super();
        resourcesAccessId_ = pResourcesAccessId;
        description_ = pDescription;
        microservice_ = pMicroservice;
        resource_ = pResource;
        verb_ = pVerb;
    }

    public Integer getResourcesAccessId() {
        return resourcesAccessId_;
    }

    public String getDescription() {
        return description_;
    }

    public void setDescription(String pDescription) {
        description_ = pDescription;
    }

    public String getMicroservice() {
        return microservice_;
    }

    public void setMicroservice(String pMicroservice) {
        microservice_ = pMicroservice;
    }

    public String getResource() {
        return resource_;
    }

    public void setResource(String pResource) {
        resource_ = pResource;
    }

    public HttpVerb getVerb() {
        return verb_;
    }

    public void setVerb(HttpVerb pVerb) {
        verb_ = pVerb;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof ResourcesAccess) && ((ResourcesAccess) o).microservice_.equals(this.microservice_)
                && ((ResourcesAccess) o).resource_.equals(this.resource_)
                && ((ResourcesAccess) o).verb_.equals(this.verb_);

    }
}
