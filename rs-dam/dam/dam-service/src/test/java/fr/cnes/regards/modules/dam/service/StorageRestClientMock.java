package fr.cnes.regards.modules.dam.service;

import com.google.common.collect.Sets;
import feign.Response;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import fr.cnes.regards.modules.storage.domain.database.StorageLocationConfiguration;
import fr.cnes.regards.modules.storage.domain.database.UserCurrentQuotas;
import fr.cnes.regards.modules.storage.domain.dto.StorageLocationDTO;
import fr.cnes.regards.modules.storage.domain.dto.quota.DownloadQuotaLimitsDto;
import fr.cnes.regards.modules.storage.domain.plugin.IOnlineStorageLocation;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import org.springframework.context.annotation.Primary;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Primary
@Component
public class StorageRestClientMock implements IStorageRestClient {
    @Override
    public Response downloadFile(String checksum, Boolean isContentInline) {
        return null;
    }

    @Override
    public ResponseEntity<List<EntityModel<StorageLocationDTO>>> retrieve() {
        PluginMetaData pluginMetaData = new PluginMetaData();
        pluginMetaData.setInterfaceNames(Sets.newHashSet(IOnlineStorageLocation.class.getName()));
        PluginConfiguration pluginConfiguration = new PluginConfiguration();
        pluginConfiguration.setMetaData(pluginMetaData);
        StorageLocationDTO storageLocationDTO = new StorageLocationDTO(
                "Local",
                0L,
                0L,
                0L,
                0L,
                true,
                true,
                true,
                new StorageLocationConfiguration("name", pluginConfiguration, 0L),
                true
        );
        List<EntityModel<StorageLocationDTO>> list = new ArrayList<>();
        list.add(new EntityModel<>(storageLocationDTO));
        return ResponseEntity.ok(list);
    }

    @Override
    public Response export() {
        return null;
    }

    @Override
    public ResponseEntity<DownloadQuotaLimitsDto> getQuotaLimits(String userEmail) {
        return null;
    }

    @Override
    public ResponseEntity<DownloadQuotaLimitsDto> upsertQuotaLimits(String userEmail, @Valid DownloadQuotaLimitsDto quotaLimits) {
        return null;
    }

    @Override
    public ResponseEntity<List<DownloadQuotaLimitsDto>> getQuotaLimits(String[] userEmails) {
        return null;
    }

    @Override
    public ResponseEntity<DownloadQuotaLimitsDto> getQuotaLimits() {
        return null;
    }

    @Override
    public ResponseEntity<UserCurrentQuotas> getCurrentQuotas() {
        return null;
    }

    @Override
    public ResponseEntity<UserCurrentQuotas> getCurrentQuotas(String userEmail) {
        return null;
    }

    @Override
    public ResponseEntity<List<UserCurrentQuotas>> getCurrentQuotasList(String[] userEmails) {
        return null;
    }

}
