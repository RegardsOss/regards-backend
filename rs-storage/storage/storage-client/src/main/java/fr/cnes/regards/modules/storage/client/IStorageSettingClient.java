package fr.cnes.regards.modules.storage.client;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.modules.tenant.settings.client.IDynamicTenantSettingClient;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@RestClient(name = "rs-storage", contextId = "rs-storage.setting.client")
public interface IStorageSettingClient extends IDynamicTenantSettingClient {

}
