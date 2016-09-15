package fr.cnes.regards.modules.accessRights.domain;

import org.springframework.hateoas.ResourceSupport;

/*
 * LICENSE_PLACEHOLDER
 */
public class ResourcesAccess extends ResourceSupport {

    private Integer resourcesAccessId_;

    private String description_;

    private String microservice_;

    private String ressource_;

    private HttpVerb verb_;

    public ResourcesAccess() {
        verb_ = HttpVerb.GET;
    }

    public ResourcesAccess(Integer pResourcesAccessId, String pDescription, String pMicroservice, String pRessource,
            HttpVerb pVerb) {
        super();
        resourcesAccessId_ = pResourcesAccessId;
        description_ = pDescription;
        microservice_ = pMicroservice;
        ressource_ = pRessource;
        verb_ = pVerb;
    }

    public Integer getResourcesAccessId() {
        return resourcesAccessId_;
    }

    public void setResourcesAccessId(Integer pResourcesAccessId) {
        resourcesAccessId_ = pResourcesAccessId;
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

    public String getRessource() {
        return ressource_;
    }

    public void setRessource(String pRessource) {
        ressource_ = pRessource;
    }

    public HttpVerb getVerb() {
        return verb_;
    }

    public void setVerb(HttpVerb pVerb) {
        verb_ = pVerb;
    }

}
