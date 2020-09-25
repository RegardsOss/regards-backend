package fr.cnes.regards.modules.access.services.client;

import fr.cnes.regards.modules.accessrights.client.IAccessSettingsClient;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import org.springframework.context.annotation.Primary;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.validation.Valid;

/**
 * Make Spring DI happy.
 */
@Primary
@Component
public class AccessSettingsClientMock implements IAccessSettingsClient {
    @Override
    public ResponseEntity<EntityModel<AccessSettings>> retrieveAccessSettings() {
        return null;
    }

    @Override
    public ResponseEntity<EntityModel<AccessSettings>> updateAccessSettings(@Valid AccessSettings accessSettings) {
        return null;
    }
}
