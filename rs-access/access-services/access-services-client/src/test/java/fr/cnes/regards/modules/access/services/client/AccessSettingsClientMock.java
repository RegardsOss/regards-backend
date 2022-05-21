package fr.cnes.regards.modules.access.services.client;

import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSettingDto;
import fr.cnes.regards.modules.accessrights.client.IAccessRightSettingClient;
import org.springframework.context.annotation.Primary;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Make Spring DI happy.
 */
@Primary
@Component
public class AccessSettingsClientMock implements IAccessRightSettingClient {

    @Override
    public ResponseEntity<EntityModel<DynamicTenantSettingDto>> update(String name, DynamicTenantSettingDto setting) {
        return null;
    }

    @Override
    public ResponseEntity<List<EntityModel<DynamicTenantSettingDto>>> retrieveAll(Set<String> names) {
        return null;
    }
}
