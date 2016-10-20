/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.bootstrap;

import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author msordi
 *
 */
@ConfigurationProperties(prefix = "regards.microservice")
public class MicroserviceProperties {

    /**
     *
     * Name of the microservice
     */
    @NotNull
    private String name;

    /**
     *
     * Version of the microservice
     */
    @NotNull
    private String version;

    /**
     *
     * Dependencies of microservice
     */
    private String[] dependencies;

    public String getName() {
        return name;
    }

    public void setName(String pName) {
        name = pName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String pVersion) {
        version = pVersion;
    }

    public String[] getDependencies() {
        return dependencies;
    }

    public void setDependencies(String[] pDependencies) {
        dependencies = pDependencies;
    }

}
