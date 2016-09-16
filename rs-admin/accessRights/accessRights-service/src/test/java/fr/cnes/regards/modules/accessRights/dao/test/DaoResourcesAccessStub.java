/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.dao.test;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.accessRights.dao.IDaoResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.HttpVerb;
import fr.cnes.regards.modules.accessRights.domain.ResourcesAccess;

@Repository
public class DaoResourcesAccessStub implements IDaoResourcesAccess {

    private static List<ResourcesAccess> resourcesAccesses_;

    @PostConstruct
    public void init() {
        resourcesAccesses_ = new ArrayList<ResourcesAccess>();
        resourcesAccesses_
                .add(new ResourcesAccess(0, "ResourceAccess 0", "Microservice 0", "Resource 0", HttpVerb.GET));
        resourcesAccesses_
                .add(new ResourcesAccess(1, "ResourceAccess 1", "Microservice 1", "Resource 1", HttpVerb.PUT));
        resourcesAccesses_
                .add(new ResourcesAccess(2, "ResourceAccess 2", "Microservice 2", "Resource 2", HttpVerb.DELETE));
        resourcesAccesses_
                .add(new ResourcesAccess(3, "ResourceAccess 3", "Microservice 3", "Resource 3", HttpVerb.GET));
    }

    @Override
    public ResourcesAccess getById(Integer pResourcesAccessId) {
        return resourcesAccesses_.stream().filter(r -> r.getResourcesAccessId().equals(pResourcesAccessId)).findFirst()
                .get();
    }

    @Override
    public List<ResourcesAccess> getAll() {
        return resourcesAccesses_;
    }

}
