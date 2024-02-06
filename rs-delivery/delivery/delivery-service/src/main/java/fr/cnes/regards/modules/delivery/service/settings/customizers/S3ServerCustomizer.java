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
import fr.cnes.regards.modules.delivery.domain.settings.S3DeliveryServer;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import java.util.HashMap;

/**
 * Customizer for {@link DeliverySettings#S3_SERVER}.
 *
 * @author Iliana Ghazali
 **/
@Service
public class S3ServerCustomizer implements IDynamicTenantSettingCustomizer {

    private final Validator validator;

    public S3ServerCustomizer(Validator validator) {
        this.validator = validator;
    }

    @Override
    public Errors isValid(DynamicTenantSetting dynamicTenantSetting) {
        Errors errors = new MapBindingResult(new HashMap<>(), S3DeliveryServer.class.getName());
        validator.validate(dynamicTenantSetting.getValue(), errors);
        return errors;
    }

    @Override
    public boolean appliesTo(DynamicTenantSetting dynamicTenantSetting) {
        return DeliverySettings.S3_SERVER.equals(dynamicTenantSetting.getName());
    }
}
