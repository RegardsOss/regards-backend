/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.dao.stubs;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.accessRights.dao.IDaoResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.HttpVerb;
import fr.cnes.regards.modules.accessRights.domain.ResourcesAccess;

@Repository
@Profile("test")
@Primary
public class DaoResourcesAccessStub implements IDaoResourcesAccess {

    private static List<ResourcesAccess> resourcesAccesses_;

    public DaoResourcesAccessStub() {
        resourcesAccesses_ = new ArrayList<ResourcesAccess>();
        resourcesAccesses_
                .add(new ResourcesAccess(0L, "ResourceAccess 0", "Microservice 0", "Resource 0", HttpVerb.GET));
        resourcesAccesses_
                .add(new ResourcesAccess(1L, "ResourceAccess 1", "Microservice 1", "Resource 1", HttpVerb.PUT));
        resourcesAccesses_
                .add(new ResourcesAccess(2L, "ResourceAccess 2", "Microservice 2", "Resource 2", HttpVerb.DELETE));
        resourcesAccesses_
                .add(new ResourcesAccess(3L, "ResourceAccess 3", "Microservice 3", "Resource 3", HttpVerb.GET));
    }

    @Override
    public ResourcesAccess getById(Integer pResourcesAccessId) {
        return resourcesAccesses_.stream().filter(r -> r.getId().equals(pResourcesAccessId)).findFirst().get();
    }

    @Override
    public List<ResourcesAccess> getAll() {
        return resourcesAccesses_;
    }

}
