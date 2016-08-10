package fr.cnes.regards.microservices.backend.pojo;

import org.springframework.hateoas.ResourceSupport;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lmieulet on 02/08/16.
 */
public class Role extends ResourceSupport {
    private String nom;
    private Role parentRole;
    private List<ResourcesAccess> permissionx;

    public Role(String nom) {
        this.nom = nom;
        this.permissionx = new ArrayList();
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public Role getParentRole() {
        return parentRole;
    }

    public void setParentRole(Role parentRole) {
        this.parentRole = parentRole;
    }

    public List<ResourcesAccess> getPermissions() {
        return permissionx;
    }

    public void setPermissions(List<ResourcesAccess> permissions) {
        this.permissionx = permissions;
    }
}
