package fr.cnes.regards.modules.access.services.client;

import java.util.List;
import java.util.Set;

import org.springframework.context.annotation.Primary;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSettingDto;
import fr.cnes.regards.modules.storage.client.IStorageSettingClient;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Primary
@Component
public class StorageSettingClientMock implements IStorageSettingClient {

    @Override
    public ResponseEntity<EntityModel<DynamicTenantSettingDto>> update(String name, DynamicTenantSettingDto setting) {
        return null;
    }

    @Override
    public ResponseEntity<List<EntityModel<DynamicTenantSettingDto>>> retrieveAll(Set<String> names) {
        return null;
    }
}
