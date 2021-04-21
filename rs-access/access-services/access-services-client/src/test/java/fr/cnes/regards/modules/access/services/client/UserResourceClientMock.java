package fr.cnes.regards.modules.access.services.client;

import fr.cnes.regards.modules.accessrights.client.IUserResourceClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import org.springframework.context.annotation.Primary;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.validation.Valid;
import java.util.List;

/**
 * Make Spring DI happy.
 */
@Primary
@Component
public class UserResourceClientMock implements IUserResourceClient {
    @Override
    public ResponseEntity<List<EntityModel<ResourcesAccess>>> retrieveProjectUserResources(String pUserLogin, String pBorrowedRoleName) {
        return null;
    }

    @Override
    public ResponseEntity<Void> updateProjectUserResources(String pLogin, @Valid List<ResourcesAccess> pUpdatedUserAccessRights) {
        return null;
    }

    @Override
    public ResponseEntity<Void> removeProjectUserResources(String pUserLogin) {
        return null;
    }
}
