/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain.projects;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.SequenceGenerator;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;

import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.modules.accessrights.domain.HttpVerb;

@Entity(name = "T_RESOURCES_ACCESS")
@SequenceGenerator(name = "resourcesAccessSequence", initialValue = 1, sequenceName = "SEQ_RESOURCES_ACCESS")
public class ResourcesAccess implements IIdentifiable<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "resourcesAccessSequence")
    @Column(name = "id")
    private Long id;

    @Column(name = "description")
    private String description;

    @NotBlank
    @Column(name = "microservice")
    private String microservice;

    @NotBlank
    @Column(name = "resource")
    private String resource;

    @NotNull
    @Column(name = "verb")
    @Enumerated(EnumType.STRING)
    private HttpVerb verb;

    @ManyToMany
    @JoinTable(name = "TA_RESOURCES_ROLES",
            joinColumns = @JoinColumn(name = "RESOURCE_ID", referencedColumnName = "ID",
                    foreignKey = @javax.persistence.ForeignKey(name = "FK_RESOURCES_ROLES")),
            inverseJoinColumns = @JoinColumn(name = "ROLE_ID", referencedColumnName = "ID",
                    foreignKey = @javax.persistence.ForeignKey(name = "FK_ROLES_RESOURCES")))
    private List<Role> roles;

    public ResourcesAccess() {
        super();
        verb = HttpVerb.GET;
    }

    public ResourcesAccess(final Long pResourcesAccessId) {
        this();
        id = pResourcesAccessId;
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

}
