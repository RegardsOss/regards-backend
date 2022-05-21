/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.accessrights.instance.service.setting;

import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingCustomizer;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountSettings;
import org.springframework.stereotype.Component;

@Component
public class AccountValidationModeSettingCustomizer implements IDynamicTenantSettingCustomizer {

    @Override
    public boolean isValid(DynamicTenantSetting dynamicTenantSetting) {
        return isProperValue(dynamicTenantSetting.getDefaultValue()) && (dynamicTenantSetting.getValue() == null
            || isProperValue(dynamicTenantSetting.getValue()));
    }

    @Override
    public boolean appliesTo(DynamicTenantSetting dynamicTenantSetting) {
        return AccountSettings.VALIDATION_SETTING.getName().equals(dynamicTenantSetting.getName());
    }

    private boolean isProperValue(Object value) {
        return value instanceof String && AccountSettings.ValidationMode.fromName((String) value) != null;
    }

}
