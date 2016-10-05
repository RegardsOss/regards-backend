/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.security.endpoint;

import java.util.Optional;

import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.security.utils.endpoint.annotation.ResourceAccess;

/**
 *
 * Carries resource endpoint configuration
 *
 * @author msordi
 *
 */
public class ResourceMapping {

    /**
     * Resource separator for endpoint identifier construction : <b>resource@verb</b>
     */
    private static final String SEPARATOR = "@";

    /**
     * Resource access method annotation
     */
    private final ResourceAccess resourceAccess_;

    /**
     * Full URL paths to the resource (class level + method level)
     */
    private final Optional<String> fullPath_;

    /**
     * HTTP method
     */
    private final RequestMethod method_;

    /**
     * Constructor
     *
     * @param pResourceAccess
     *            the resource access annotation
     * @param pFullPath
     *            the URL path to access resource
     * @param pMethod
     *            the called HTTP method
     */
    public ResourceMapping(ResourceAccess pResourceAccess, Optional<String> pFullPath, RequestMethod pMethod) {
        resourceAccess_ = pResourceAccess;
        fullPath_ = pFullPath;
        method_ = pMethod;
    }

    /**
     * Constructor
     *
     * @param pFullPath
     *            the URL path to access resource
     * @param pMethod
     *            the called HTTP method
     */
    public ResourceMapping(Optional<String> pFullPath, RequestMethod pMethod) {
        this(null, pFullPath, pMethod);
    }

    /**
     * Compute resource identifier
     *
     * @return a unique identifier for the resource access
     */
    public String getResourceMappingId() {
        final StringBuffer identifier = new StringBuffer();

        if (fullPath_.isPresent()) {
            identifier.append(fullPath_.get());
        }

        identifier.append(SEPARATOR);
        identifier.append(method_.toString());

        return identifier.toString();
    }

    /**
     * @return the resourceAccess
     */
    public ResourceAccess getResourceAccess() {
        return resourceAccess_;
    }

    /**
     * @return the path
     */
    public Optional<String> getFullPath() {
        return fullPath_;
    }

    /**
     * @return the method
     */
    public RequestMethod getMethod() {
        return method_;
    }
}
