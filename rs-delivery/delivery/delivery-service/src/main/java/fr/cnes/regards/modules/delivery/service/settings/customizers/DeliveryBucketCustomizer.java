/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.modules.tenant.settings.service.AbstractSimpleDynamicSettingCustomizer;
import fr.cnes.regards.modules.delivery.domain.settings.DeliverySettings;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * Customizer for {@link DeliverySettings#DELIVERY_BUCKET}
 *
 * @author Iliana Ghazali
 **/
@Service
public class DeliveryBucketCustomizer extends AbstractSimpleDynamicSettingCustomizer {

    public DeliveryBucketCustomizer() {
        super(DeliverySettings.DELIVERY_BUCKET, "parameter [request time to live] must be a valid positive number.");
    }

    @Override
    protected boolean isProperValue(Object value) {
        return value instanceof String strValue && StringUtils.isNotBlank(strValue);
    }
}