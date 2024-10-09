package fr.cnes.regards.modules.dam.service.settings;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.modules.tenant.settings.service.AbstractSimpleDynamicSettingCustomizer;
import fr.cnes.regards.modules.dam.domain.settings.DamSettings;
import fr.cnes.regards.modules.fileaccess.dto.StorageType;
import fr.cnes.regards.modules.filecatalog.dto.StorageLocationDto;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StorageLocationSettingCustomizer extends AbstractSimpleDynamicSettingCustomizer {

    private final IStorageRestClient storageRestClient;

    public StorageLocationSettingCustomizer(IStorageRestClient storageRestClient) {
        super(DamSettings.STORAGE_LOCATION,
              "parameter [storage location] can be null or must be a valid string or existing online location");
        this.storageRestClient = storageRestClient;
    }

    @Override
    protected boolean isProperValue(Object value) {
        if (value == null) {
            return true;
        }
        return value instanceof String storageLocation && (StringUtils.isBlank(storageLocation) || isOnlineLocation(
            storageLocation));
    }

    private boolean isOnlineLocation(String location) {
        ResponseEntity<List<EntityModel<StorageLocationDto>>> responseEntity = storageRestClient.retrieve();
        if (!responseEntity.hasBody()) {
            return false;
        } else {
            List<StorageLocationDto> storageLocationList = HateoasUtils.unwrapList(responseEntity.getBody());
            return storageLocationList.stream()
                                      .filter(storageLocationDTO -> storageLocationDTO.getConfiguration()
                                                                                      .getStorageType()
                                                                                      .equals(StorageType.ONLINE))
                                      .anyMatch(storageLocationDTO -> storageLocationDTO.getName().equals(location));
        }
    }

}
