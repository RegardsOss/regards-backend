/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.plugins.domain.parameter;

import javax.persistence.Transient;

import fr.cnes.regards.framework.gson.annotation.GsonIgnore;

/**
 * Supported plugin parameter
 *
 * @author Marc SORDI
 *
 */
public class StringPluginParam extends AbstractPluginParam<String> {

    public StringPluginParam() {
        super(PluginParamType.STRING);
    }

    @Transient
    @GsonIgnore
    protected transient String decryptedValue;

    @Override
    public boolean supportsDefaultValue() {
        return true;
    }

    @Override
    public void applyDefaultValue(String value) {
        if (!hasValue()) {
            this.value = value;
        }
    }

    public String getDecryptedValue() {
        return decryptedValue;
    }

    public void setDecryptedValue(String decryptedValue) {
        this.decryptedValue = decryptedValue;
    }

    @Override
    public boolean hasValue() {
        return value != null && !value.isEmpty();
    }
}
