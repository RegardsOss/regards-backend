package fr.cnes.regards.microservices.backend.pojo.administration;

import org.springframework.hateoas.ResourceSupport;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lmieulet on 02/08/16.
 */
public class Role extends ResourceSupport {
    private String name;
    private Role parentRole;
    private List<ResourceAccess> permissions;

    public Role(String name) {
        this.name = name;
        this.permissions = new ArrayList();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Role getParentRole() {
        return parentRole;
    }

    public void setParentRole(Role parentRole) {
        this.parentRole = parentRole;
    }

    public List<ResourceAccess> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<ResourceAccess> permissions) {
        this.permissions = permissions;
    }
}
