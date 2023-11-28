package fr.cnes.regards.modules.dam.service;

import com.google.common.collect.Sets;
import feign.Response;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.dto.PluginMetaData;
import fr.cnes.regards.modules.filecatalog.dto.FileReferenceDto;
import fr.cnes.regards.modules.filecatalog.dto.StorageLocationConfigurationDto;
import fr.cnes.regards.modules.filecatalog.dto.StorageLocationDto;
import fr.cnes.regards.modules.filecatalog.dto.StorageType;
import fr.cnes.regards.modules.filecatalog.dto.quota.DownloadQuotaLimitsDto;
import fr.cnes.regards.modules.filecatalog.dto.quota.UserCurrentQuotasDto;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import org.springframework.context.annotation.Primary;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Primary
@Component
public class StorageRestClientMock implements IStorageRestClient {

    @Override
    public Response downloadFile(String checksum, Boolean isContentInline) {
        return null;
    }

    @Override
    public ResponseEntity<List<EntityModel<StorageLocationDto>>> retrieve() {
        PluginMetaData pluginMetaData = new PluginMetaData();
        pluginMetaData.setInterfaceNames(Sets.newHashSet(StorageLocationDto.class.getName()));
        PluginConfiguration pluginConfiguration = new PluginConfiguration();
        pluginConfiguration.setMetaData(pluginMetaData);
        StorageLocationConfigurationDto configuration = new StorageLocationConfigurationDto("name",
                                                                                            pluginConfiguration.toDto(),
                                                                                            StorageType.ONLINE,
                                                                                            0L);
        StorageLocationDto storageLocationDTO = StorageLocationDto.build("Local", configuration)
                                                                  .withAllowPhysicalDeletion()
                                                                  .withRunningProcessesInformation(true,
                                                                                                   true,
                                                                                                   true,
                                                                                                   false);
        return ResponseEntity.ok(Collections.singletonList(EntityModel.of(storageLocationDTO)));
    }

    @Override
    public Response export() {
        return null;
    }

    @Override
    public ResponseEntity<Set<FileReferenceDto>> getFileReferencesWithoutOwners(String storage, Set<String> checksums) {
        return null;
    }

    @Override
    public ResponseEntity<DownloadQuotaLimitsDto> getQuotaLimits(String userEmail) {
        return null;
    }

    @Override
    public ResponseEntity<DownloadQuotaLimitsDto> upsertQuotaLimits(String userEmail,
                                                                    @Valid DownloadQuotaLimitsDto quotaLimits) {
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
    public ResponseEntity<UserCurrentQuotasDto> getCurrentQuotas() {
        return null;
    }

    @Override
    public ResponseEntity<UserCurrentQuotasDto> getCurrentQuotas(String userEmail) {
        return null;
    }

    @Override
    public ResponseEntity<Long> getMaxQuota() {
        return null;
    }

    @Override
    public ResponseEntity<List<UserCurrentQuotasDto>> getCurrentQuotasList(String[] userEmails) {
        return null;
    }

}
