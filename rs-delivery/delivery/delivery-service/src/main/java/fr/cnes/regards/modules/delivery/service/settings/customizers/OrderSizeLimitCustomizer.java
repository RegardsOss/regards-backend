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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Customizer for {@link DeliverySettings#DELIVERY_ORDER_SIZE_LIMIT_BYTES}
 *
 * @author Sébastien Binda
 **/
@Service
public class OrderSizeLimitCustomizer implements IDynamicTenantSettingCustomizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderSizeLimitCustomizer.class);

    @Override
    public boolean isValid(DynamicTenantSetting dynamicTenantSetting) {
        boolean valid = (dynamicTenantSetting.getValue() instanceof Long orderSizeLimit) && orderSizeLimit >= 0;
        if (!valid) {
            LOGGER.error("'{}' must be a valid Long >= 0", DeliverySettings.DELIVERY_ORDER_SIZE_LIMIT_BYTES);
        }
        return valid;
    }

    @Override
    public boolean appliesTo(DynamicTenantSetting dynamicTenantSetting) {
        return DeliverySettings.DELIVERY_ORDER_SIZE_LIMIT_BYTES.equals(dynamicTenantSetting.getName());
    }
}
