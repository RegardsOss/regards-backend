/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.domain.projects;

import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.framework.security.role.DefaultRole;
import org.hibernate.annotations.Type;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Class ResourcesAccess
 * <p>
 * JPA Entity to manage resource accesses. ResourcesAccess POJO define the access and authorizations to a microservice
 * endpoint resource.
 *
 * @author Sébastien Binda
 * @author Christophe Mertz
 */
@Entity
@Table(name = "t_resources_access")
@SequenceGenerator(name = "resourcesAccessSequence", initialValue = 1, sequenceName = "seq_resources_access")
public class ResourcesAccess implements IIdentifiable<Long> {

    /**
     * Resource identifier
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "resourcesAccessSequence")
    @Column(name = "id")
    private Long id;

    /**
     * Resource description
     */
    @Column(name = "description")
    @Type(type = "text")
    private String description;

    /**
     * Microservice of the current resource
     */
    @NotBlank
    @Column(name = "microservice", length = 32)
    private String microservice;

    /**
     * The controller simple name
     */
    @NotBlank
    @Column(name = "controller_name", length = 256)
    private String controllerSimpleName;

    /**
     * Path of the endpoint
     */
    @NotBlank
    @Column(name = "resource", length = 512)
    private String resource;

    /**
     * Http Verb of the endpoint
     */
    @NotNull
    @Column(name = "verb", length = 10)
    @Enumerated(EnumType.STRING)
    private RequestMethod verb;

    @NotNull
    @Column(name = "defaultRole", length = 16)
    @Enumerated(EnumType.STRING)
    private DefaultRole defaultRole;

    /**
     * Default constructor
     */
    public ResourcesAccess() {
        super();
        verb = RequestMethod.GET;
    }

    /**
     * Constructor
     *
     * @param pResourcesAccessId the resources access id
     */
    public ResourcesAccess(final Long pResourcesAccessId) {
        super();
        id = pResourcesAccessId;
        verb = RequestMethod.GET;
    }

    /**
     * Constructor
     *
     * @param pResourcesAccessId    the resources access id
     * @param pDescription          the description
     * @param pMicroservice         the microservice name
     * @param pResource             the resource
     * @param pControllerSimpleName the controller name
     * @param pVerb                 the verb
     * @param pDefaultRole          the default role
     */
    public ResourcesAccess(final Long pResourcesAccessId,
                           final String pDescription,
                           final String pMicroservice,
                           final String pResource,
                           final String pControllerSimpleName,
                           final RequestMethod pVerb,
                           final DefaultRole pDefaultRole) {
        super();
        id = pResourcesAccessId;
        description = pDescription;
        microservice = pMicroservice;
        resource = pResource;
        controllerSimpleName = pControllerSimpleName;
        verb = pVerb;
        this.defaultRole = pDefaultRole;
    }

    /**
     * Constructor
     *
     * @param pDescription          the description
     * @param pMicroservice         the microservice name
     * @param pResource             the resource
     * @param pControllerSimpleName the controller name
     * @param pVerb                 the verb
     * @param pDefaultRole          the default role
     */
    public ResourcesAccess(final String pDescription,
                           final String pMicroservice,
                           final String pResource,
                           final String pControllerSimpleName,
                           final RequestMethod pVerb,
                           final DefaultRole pDefaultRole) {
        this(null, pDescription, pMicroservice, pResource, pControllerSimpleName, pVerb, pDefaultRole);
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((microservice == null) ? 0 : microservice.hashCode());
        result = (prime * result) + ((resource == null) ? 0 : resource.hashCode());
        result = (prime * result) + ((verb == null) ? 0 : verb.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ResourcesAccess other = (ResourcesAccess) obj;
        if (microservice == null) {
            if (other.microservice != null) {
                return false;
            }
        } else if (!microservice.equals(other.microservice)) {
            return false;
        }
        if (resource == null) {
            if (other.resource != null) {
                return false;
            }
        } else if (!resource.equals(other.resource)) {
            return false;
        }
        return verb == other.verb;
    }

    public void setId(final Long pId) {
        id = pId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String pDescription) {
        description = pDescription;
    }

    public String getMicroservice() {
        return microservice;
    }

    public void setMicroservice(final String pMicroservice) {
        microservice = pMicroservice;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(final String pResource) {
        resource = pResource;
    }

    public RequestMethod getVerb() {
        return verb;
    }

    public void setVerb(final RequestMethod pVerb) {
        verb = pVerb;
    }

    /**
     * @return the controller simple name
     */
    public String getControllerSimpleName() {
        return controllerSimpleName;
    }

    public void setControllerSimpleName(final String pControllerSimpleName) {
        controllerSimpleName = pControllerSimpleName;
    }

    public DefaultRole getDefaultRole() {
        return defaultRole;
    }

    public void setDefaultRole(final DefaultRole pDefaultRole) {
        defaultRole = pDefaultRole;
    }

    @Override
    public String toString() {
        return "ResourcesAccess{" + "id=" + id + ", description='" + description + '\'' + ", microservice='"
            + microservice + '\'' + ", controllerSimpleName='" + controllerSimpleName + '\'' + ", resource='" + resource
            + '\'' + ", verb=" + verb + ", defaultRole=" + defaultRole + '}';
    }
}
