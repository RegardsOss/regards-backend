/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.domain;

import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.security.annotation.ResourceAccess;

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
    private final ResourceAccess resourceAccess;

    /**
     * Full URL paths to the resource (class level + method level)
     */
    private String fullPath = "/";

    /**
     * HTTP method
     */
    private final RequestMethod method;

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
    public ResourceMapping(final ResourceAccess pResourceAccess, final String pFullPath, final RequestMethod pMethod) {
        resourceAccess = pResourceAccess;
        fullPath = pFullPath;
        method = pMethod;
    }

    /**
     * Constructor
     *
     * @param pFullPath
     *            the URL path to access resource
     * @param pMethod
     *            the called HTTP method
     */
    public ResourceMapping(final String pFullPath, final RequestMethod pMethod) {
        this(null, pFullPath, pMethod);
    }

    /**
     * Compute resource identifier
     *
     * @return a unique identifier for the resource access
     */
    public String getResourceMappingId() {
        final StringBuffer identifier = new StringBuffer();

        identifier.append(fullPath);
        identifier.append(SEPARATOR);
        identifier.append(method.toString());

        return identifier.toString();
    }

    /**
     * @return the resourceAccess
     */
    public ResourceAccess getResourceAccess() {
        return resourceAccess;
    }

    /**
     * @return the path
     */
    public String getFullPath() {
        return fullPath;
    }

    /**
     * @return the method
     */
    public RequestMethod getMethod() {
        return method;
    }
}
