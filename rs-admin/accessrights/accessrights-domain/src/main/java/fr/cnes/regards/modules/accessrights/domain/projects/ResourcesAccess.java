/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain.projects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.framework.security.entity.listeners.UpdateAuthoritiesListener;
import fr.cnes.regards.framework.security.role.DefaultRole;

/**
 *
 * Class ResourcesAccess
 *
 * JPA Entity to manage resource accesses. ResourcesAccess POJO define the access and authorizations to a microservice
 * endpoint resource.
 *
 * @author Sébastien Binda
 * @author Christophe Mertz
 * @since 1.0-SNAPSHOT
 */
@Entity
@Table(name = "T_RESOURCES_ACCESS")
@EntityListeners(UpdateAuthoritiesListener.class)
@SequenceGenerator(name = "resourcesAccessSequence", initialValue = 1, sequenceName = "SEQ_RESOURCES_ACCESS")
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
    private String description;

    /**
     * Microservice of the current resource
     */
    @NotBlank
    @Column(name = "microservice")
    private String microservice;

    @NotBlank
    @Column(name = "controller_name")
    private String controllerSimpleName;

    /**
     * Path of the endpoint
     */
    @NotBlank
    @Column(name = "resource")
    private String resource;

    /**
     * Http Verb of the endpoint
     */
    @NotNull
    @Column(name = "verb")
    @Enumerated(EnumType.STRING)
    private RequestMethod verb;

    @NotNull
    @Column(name = "defaultRole")
    @Enumerated(EnumType.STRING)
    private DefaultRole defaultRole;

    public ResourcesAccess() {
        super();
        verb = RequestMethod.GET;
    }

    public ResourcesAccess(final Long pResourcesAccessId) {
        super();
        id = pResourcesAccessId;
        verb = RequestMethod.GET;
    }

    public ResourcesAccess(final Long pResourcesAccessId, final String pDescription, final String pMicroservice,
            final String pResource, final String pControllerSimpleName, final RequestMethod pVerb,
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

    public ResourcesAccess(final String pDescription, final String pMicroservice, final String pResource,
            final String pControllerSimpleName, final RequestMethod pVerb, final DefaultRole pDefaultRole) {
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
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ResourcesAccess other = (ResourcesAccess) obj;
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
        if (verb != other.verb) {
            return false;
        }
        return true;
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

    public String getControllerSimpleName() {
        return controllerSimpleName;
    }

    public void setControllerSimpleName(String pControllerSimpleName) {
        controllerSimpleName = pControllerSimpleName;
    }

    public DefaultRole getDefaultRole() {
        return defaultRole;
    }

    public void setDefaultRole(DefaultRole pDefaultRole) {
        defaultRole = pDefaultRole;
    }

}
