package fr.cnes.regards.modules.accessrights.client;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.modules.tenant.settings.client.IDynamicTenantSettingClient;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@RestClient(name = "rs-admin", contextId = "rs-admin.access-right-setting-client")
public interface IAccessRightSettingClient extends IDynamicTenantSettingClient {

}
