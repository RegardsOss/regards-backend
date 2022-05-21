package fr.cnes.regards.modules.accessrights.instance.service.setting;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.AbstractSettingService;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingService;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountSettings;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RegardsTransactional
public class AccountSettingsService extends AbstractSettingService {

    public AccountSettingsService(IDynamicTenantSettingService dynamicTenantSettingService) {
        super(dynamicTenantSettingService);
    }

    @EventListener
    public void onApplicationStartedEvent(ApplicationStartedEvent event)
        throws EntityNotFoundException, EntityOperationForbiddenException, EntityInvalidException {
        init();
    }

    @Override
    protected List<DynamicTenantSetting> getSettingList() {
        return AccountSettings.SETTING_LIST;
    }

    public boolean isAutoAccept() {
        return AccountSettings.ValidationMode.AUTO_ACCEPT.equals(AccountSettings.ValidationMode.fromName(getValue(
            AccountSettings.VALIDATION)));
    }

}
