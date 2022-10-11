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
package fr.cnes.regards.modules.ltamanager.service.settings.customizers;

import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingCustomizer;
import fr.cnes.regards.modules.ltamanager.domain.settings.LtaSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Customizer for {@link LtaSettings#STORAGE_KEY}
 *
 * @author Iliana Ghazali
 **/
@Service
public class LtaStorageSettingCustomizer implements IDynamicTenantSettingCustomizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(LtaStorageSettingCustomizer.class);

    private static final int LIMIT_STORAGE_CHARACTERS = 255;

    @Override
    public boolean isValid(DynamicTenantSetting dynamicTenantSetting) {
        boolean isValid = (dynamicTenantSetting.getValue() instanceof String storage) && (storage.length()
                                                                                          < LIMIT_STORAGE_CHARACTERS);
        if (!isValid) {
            LOGGER.error("\"{}\" must not exceed 255 characters.", LtaSettings.STORAGE_KEY);
        }
        return isValid;
    }

    @Override
    public boolean appliesTo(DynamicTenantSetting dynamicTenantSetting) {
        return LtaSettings.STORAGE_KEY.equals(dynamicTenantSetting.getName());
    }
}
