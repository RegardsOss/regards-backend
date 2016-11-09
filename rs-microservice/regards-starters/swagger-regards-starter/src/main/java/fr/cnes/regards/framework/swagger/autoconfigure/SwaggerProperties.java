/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.swagger.autoconfigure;

import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Swagger properties
 *
 * @author msordi
 *
 */
@ConfigurationProperties(prefix = "regards.swagger")
public class SwaggerProperties {

    @NotNull
    private String apiName;

    @NotNull
    private String apiTitle;

    @NotNull
    private String apiDescription;

    @NotNull
    private String apiLicense;

    @NotNull
    private String apiVersion;

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String pApiName) {
        apiName = pApiName;
    }

    public String getApiTitle() {
        return apiTitle;
    }

    public void setApiTitle(String pApiTitle) {
        apiTitle = pApiTitle;
    }

    public String getApiDescription() {
        return apiDescription;
    }

    public void setApiDescription(String pApiDescription) {
        apiDescription = pApiDescription;
    }

    public String getApiLicense() {
        return apiLicense;
    }

    public void setApiLicense(String pApiLicense) {
        apiLicense = pApiLicense;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String pApiVersion) {
        apiVersion = pApiVersion;
    }
}
