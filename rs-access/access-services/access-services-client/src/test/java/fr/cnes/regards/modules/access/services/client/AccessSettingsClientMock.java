package fr.cnes.regards.modules.access.services.client;

import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.modules.accessrights.client.IAccessRightSettingClient;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import org.springframework.context.annotation.Primary;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;

/**
 * Make Spring DI happy.
 */
@Primary
@Component
public class AccessSettingsClientMock implements IAccessRightSettingClient {

    @Override
    public ResponseEntity<EntityModel<DynamicTenantSetting>> update(String name, DynamicTenantSetting setting) {
        return null;
    }

    @Override
    public ResponseEntity<List<EntityModel<DynamicTenantSetting>>> retrieveAll(Set<String> names) {
        return null;
    }
}
