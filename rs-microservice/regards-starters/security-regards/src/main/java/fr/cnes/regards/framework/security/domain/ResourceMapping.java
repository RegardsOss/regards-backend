/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.security.domain;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;

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
     * Controller simple name
     */
    private String controllerSimpleName;

    /**
     * HTTP method
     */
    private final RequestMethod method;

    /**
     * Authorized roles to access the resource
     */
    private List<RoleAuthority> authorizedRoles = new ArrayList<>();

    /**
     * Constructor
     *
     * @param pResourceAccess
     *            the resource access annotation
     * @param pFullPath
     *            the URL path to access resource
     * @param pMethod
     *            the called HTTP method
     * @param pDefaultRole
     *            default role for resource access
     */
    public ResourceMapping(final ResourceAccess pResourceAccess, final String pFullPath, final RequestMethod pMethod,
            String pSimpleName, final RoleAuthority pDefaultRole) {
        resourceAccess = pResourceAccess;
        fullPath = pFullPath;
        method = pMethod;
        authorizedRoles.add(pDefaultRole);
        controllerSimpleName = pSimpleName;
    }

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
    public ResourceMapping(final ResourceAccess pResourceAccess, final String pFullPath,
            final String pControllerSimpleName, final RequestMethod pMethod) {
        resourceAccess = pResourceAccess;
        fullPath = pFullPath;
        method = pMethod;
        controllerSimpleName = pControllerSimpleName;
    }

    /**
     * Constructor
     *
     * @param pFullPath
     *            the URL path to access resource
     * @param pMethod
     *            the called HTTP method
     */
    public ResourceMapping(final String pFullPath, final String pControllerSimpleName, final RequestMethod pMethod) {
        this(null, pFullPath, pControllerSimpleName, pMethod);
    }

    /**
     * Compute resource identifier
     *
     * @return a unique identifier for the resource access
     */
    public String getResourceMappingId() {
        final StringBuilder identifier = new StringBuilder();

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

    public List<RoleAuthority> getAutorizedRoles() {
        return authorizedRoles;
    }

    public void setAutorizedRoles(final List<RoleAuthority> pAuthorizedRoles) {
        authorizedRoles = pAuthorizedRoles;
    }

    public void addAuthorizedRole(final RoleAuthority pRole) {
        authorizedRoles.add(pRole);
    }

    /**
     * @return the controller simple name
     */
    public String getControllerSimpleName() {
        return controllerSimpleName;
    }

    /**
     * Set the controller simple name
     * @param pControllerSimpleName
     */
    public void setControllerSimpleName(String pControllerSimpleName) {
        controllerSimpleName = pControllerSimpleName;
    }

    @Override
    public String toString() {
        return controllerSimpleName + "\t" + getResourceMappingId();
    }

}
