package fr.cnes.regards.modules.access.services.client;

import fr.cnes.regards.modules.accessrights.client.IRegistrationClient;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Primary;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Make Spring DI happy.
 */
@Primary
@Component
public class RegistrationClientMock implements IRegistrationClient {

    @Override
    public ResponseEntity<EntityModel<AccessRequestDto>> requestAccess(@Valid AccessRequestDto pAccessRequest) {
        return null;
    }

    @Override
    public ResponseEntity<EntityModel<AccessRequestDto>> requestExternalAccess(@Valid AccessRequestDto pAccessRequest) {
        return null;
    }

    @Override
    public ResponseEntity<Void> verifyEmail(String token) {
        return null;
    }

    @Override
    public ResponseEntity<Void> acceptAccessRequest(Long pAccessId) {
        return null;
    }

    @Override
    public ResponseEntity<Void> denyAccessRequest(Long pAccessId) {
        return null;
    }

    @Override
    public ResponseEntity<Void> activeAccess(Long accessId) {
        return null;
    }

    @Override
    public ResponseEntity<Void> inactiveAccess(Long accessId) {
        return null;
    }

    @Override
    public ResponseEntity<Void> removeAccessRequest(Long pAccessId) {
        return null;
    }
}
