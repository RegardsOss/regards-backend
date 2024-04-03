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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.framework.modules.tenant.settings.service;

import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;

import java.util.HashMap;
import java.util.Objects;

/**
 * A default implementation for simple dynamic settings.
 *
 * @author tguillou
 */
public abstract class AbstractSimpleDynamicSettingCustomizer implements IDynamicTenantSettingCustomizer {

    private final String dynamicSettingName;

    private final String validationText;

    public AbstractSimpleDynamicSettingCustomizer(String dynamicSettingName, String validationText) {
        this.dynamicSettingName = dynamicSettingName;
        this.validationText = validationText;
    }

    /**
     * This method return true if the dynamic setting value is correct.
     */
    protected abstract boolean isProperValue(Object settingValue);

    @Override
    public boolean appliesTo(DynamicTenantSetting dynamicTenantSetting) {
        return Objects.equals(dynamicSettingName, dynamicTenantSetting.getName());
    }

    @Override
    public Errors isValid(DynamicTenantSetting dynamicTenantSetting) {
        Errors errors = new MapBindingResult(new HashMap<>(), DynamicTenantSetting.class.getName());
        Object defaultValue = dynamicTenantSetting.getDefaultValue();
        // default value cannot be null
        if (defaultValue == null || !isProperValue(defaultValue)) {
            errors.reject("invalid.default.setting.value",
                          String.format("default setting value error : %s", validationText));
        }
        if (!isProperValue(dynamicTenantSetting.getValue())) {
            errors.reject("invalid.setting.value", validationText);
        }
        return errors;
    }
}
