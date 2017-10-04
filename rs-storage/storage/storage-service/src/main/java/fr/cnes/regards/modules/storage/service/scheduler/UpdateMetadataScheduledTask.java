/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service.scheduler;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.service.IAIPService;

/**
 * Task scheduled to be execute preiodicly to handle update of {@link AIP} metadatas after new dataStorage event received.<br/>
 * If a {@link DataFile} is stored in a new place, then the linked {@link AIP} metadata file should be updated with
 * the new informations.
 * @author Sébastien Binda
 */
public class UpdateMetadataScheduledTask implements Runnable {

    /**
     * Tenant for the current scheduled task.
     */
    private final String tenant;

    /**
     * {@link IAIPService}
     */
    private final IAIPService aipService;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    public UpdateMetadataScheduledTask(String pTenant, IAIPService pAipService,
            IRuntimeTenantResolver pRuntimeTenantResolver) {
        super();
        tenant = pTenant;
        aipService = pAipService;
        runtimeTenantResolver = pRuntimeTenantResolver;
    }

    @Override
    public void run() {
        runtimeTenantResolver.forceTenant(tenant);
        aipService.updateAlreadyStoredMetadata();
        runtimeTenantResolver.clearTenant();
    }

}
