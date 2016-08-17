package fr.cnes.regards.microservices.backend.pojo;

/**
 * Created by lmieulet on 02/08/16.
 */
public class ResourceAccess {
    private String description;
    private String microservice;
    private String resource;
    private HttpVerb verb;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMicroservice() {
        return microservice;
    }

    public void setMicroservice(String microservice) {
        this.microservice = microservice;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getVerb() {
        return verb.toString();
    }

    public void setVerb(HttpVerb verb) {
        this.verb = verb;
    }
}
