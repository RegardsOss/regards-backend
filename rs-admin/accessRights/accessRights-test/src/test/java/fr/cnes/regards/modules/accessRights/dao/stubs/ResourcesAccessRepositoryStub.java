/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.dao.stubs;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.microservices.core.test.repository.RepositoryStub;
import fr.cnes.regards.modules.accessRights.dao.projects.IResourcesAccessRepository;
import fr.cnes.regards.modules.accessRights.domain.HttpVerb;
import fr.cnes.regards.modules.accessRights.domain.projects.ResourcesAccess;

@Repository
@Profile("test")
@Primary
public class ResourcesAccessRepositoryStub extends RepositoryStub<ResourcesAccess>
        implements IResourcesAccessRepository {

    public ResourcesAccessRepositoryStub() {
        super();
        entities.add(new ResourcesAccess(0L, "ResourceAccess 0", "Microservice 0", "Resource 0", HttpVerb.GET));
        entities.add(new ResourcesAccess(1L, "ResourceAccess 1", "Microservice 1", "Resource 1", HttpVerb.PUT));
        entities.add(new ResourcesAccess(2L, "ResourceAccess 2", "Microservice 2", "Resource 2", HttpVerb.DELETE));
        entities.add(new ResourcesAccess(3L, "ResourceAccess 3", "Microservice 3", "Resource 3", HttpVerb.GET));
    }

}
