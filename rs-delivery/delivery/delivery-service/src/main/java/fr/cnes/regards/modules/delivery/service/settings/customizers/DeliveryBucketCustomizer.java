/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.delivery.service.settings.customizers;

import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingCustomizer;
import fr.cnes.regards.modules.delivery.domain.settings.DeliverySettings;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;

import java.util.HashMap;

/**
 * Customizer for {@link DeliverySettings#DELIVERY_BUCKET}
 *
 * @author Iliana Ghazali
 **/
@Service
public class DeliveryBucketCustomizer implements IDynamicTenantSettingCustomizer {

    @Override
    public Errors isValid(DynamicTenantSetting dynamicTenantSetting) {
        Errors errors = new MapBindingResult(new HashMap<>(), DynamicTenantSetting.class.getName());
        if (!isProperValue(dynamicTenantSetting.getDefaultValue())) {
            errors.reject("invalid.default.setting.value",
                          "default setting value of parameter [delivery bucket] must be a valid string.");
        }
        if (!isProperValue(dynamicTenantSetting.getValue())) {
            errors.reject("invalid.setting.value",
                          "setting value of parameter [delivery bucket] must be a valid string.");
        }
        return errors;
    }

    @Override
    public boolean appliesTo(DynamicTenantSetting dynamicTenantSetting) {
        return DeliverySettings.DELIVERY_BUCKET.equals(dynamicTenantSetting.getName());
    }

    private boolean isProperValue(Object value) {
        return value instanceof String strValue && StringUtils.isNotBlank(strValue);
    }
}