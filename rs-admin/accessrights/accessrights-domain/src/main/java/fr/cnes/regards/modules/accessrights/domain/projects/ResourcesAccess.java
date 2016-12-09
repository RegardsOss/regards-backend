/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain.projects;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.SequenceGenerator;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.gson.annotation.GSonIgnore;
import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.framework.security.annotation.ResourceAccessAdapter;
import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.security.entity.listeners.UpdateAuthoritiesListener;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.modules.accessrights.domain.HttpVerb;

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
@Entity(name = "T_RESOURCES_ACCESS")
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
    private HttpVerb verb;

    /**
     * List of authorized roles to access the resource
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "TA_RESOURCES_ROLES",
            joinColumns = @JoinColumn(name = "RESOURCE_ID", referencedColumnName = "ID",
                    foreignKey = @javax.persistence.ForeignKey(name = "FK_RESOURCES_ROLES")),
            inverseJoinColumns = @JoinColumn(name = "ROLE_ID", referencedColumnName = "ID",
                    foreignKey = @javax.persistence.ForeignKey(name = "FK_ROLES_RESOURCES")))
    @GSonIgnore
    private List<Role> roles = new ArrayList<>();

    public ResourcesAccess() {
        super();
        verb = HttpVerb.GET;
    }

    public ResourcesAccess(final Long pResourcesAccessId) {
        super();
        id = pResourcesAccessId;
        verb = HttpVerb.GET;
    }

    public ResourcesAccess(final Long pResourcesAccessId, final String pDescription, final String pMicroservice,
            final String pResource, final HttpVerb pVerb) {
        super();
        id = pResourcesAccessId;
        description = pDescription;
        microservice = pMicroservice;
        resource = pResource;
        verb = pVerb;
    }

    public ResourcesAccess(final String pDescription, final String pMicroservice, final String pResource,
            final HttpVerb pVerb) {
        super();
        description = pDescription;
        microservice = pMicroservice;
        resource = pResource;
        verb = pVerb;
    }

    public ResourcesAccess(final ResourceMapping pMapping, final String pMicroservicename) {
        description = pMapping.getResourceAccess().description();
        microservice = pMicroservicename;
        resource = pMapping.getFullPath();
        verb = HttpVerb.valueOf(pMapping.getMethod().toString());
    }

    /**
     *
     * Convert a {@link ResourcesAccess} object to a {@link ResourceMapping} Object
     *
     * @return {@link ResourceMapping}
     * @since 1.0-SNAPSHOT
     */
    public ResourceMapping toResourceMapping() {
        final ResourceMapping mapping = new ResourceMapping(
                ResourceAccessAdapter.createResourceAccess(this.getDescription(), null), this.getResource(),
                RequestMethod.valueOf(this.getVerb().toString()));

        this.getRoles().forEach(role -> mapping.addAuthorizedRole(new RoleAuthority(role.getName())));
        return mapping;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return this.getId().hashCode();
    }

    @Override
    public boolean equals(final Object pObj) {
        if (pObj instanceof ResourcesAccess) {
            final ResourcesAccess toCompare = (ResourcesAccess) pObj;
            if ((this.getId() != null) && this.getId().equals(toCompare.getId())) {
                return true;
            } else {
                if ((this.getMicroservice() == null) || (this.getResource() == null) || (this.getVerb() == null)) {
                    return false;
                }
                if (this.getMicroservice().equals(toCompare.getMicroservice())
                        && this.getResource().equals(toCompare.getResource())
                        && this.getVerb().equals(toCompare.getVerb())) {
                    return true;
                }
            }
        }
        return false;
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

    public HttpVerb getVerb() {
        return verb;
    }

    public void setVerb(final HttpVerb pVerb) {
        verb = pVerb;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public void setRoles(final List<Role> pRoles) {
        roles = pRoles;
    }

    /**
     *
     * Add the given role to the authorized roles to access the current resource
     *
     * @param pRole
     *            A {@link Role}
     * @since 1.0-SNAPSHOT
     */
    public void addRole(final Role pRole) {
        if (roles == null) {
            roles = new ArrayList<>();
        }
        if (!roles.contains(pRole)) {
            roles.add(pRole);
        }
    }

    /**
     *
     * Add the given roles to the authorized roles to access the current resource
     *
     * @param pInheritedRoles
     *            a {@link List} of {@link Role}
     * @since 1.0-SNAPSHOT
     */
    public void addRoles(final List<Role> pInheritedRoles) {
        if (pInheritedRoles != null) {
            for (final Role role : pInheritedRoles) {
                this.addRole(role);
            }
        }
    }

}
