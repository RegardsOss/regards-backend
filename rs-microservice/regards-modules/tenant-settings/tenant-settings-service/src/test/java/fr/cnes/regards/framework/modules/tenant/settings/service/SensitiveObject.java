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
package fr.cnes.regards.framework.modules.tenant.settings.service;

import fr.cnes.regards.framework.encryption.sensitive.StringSensitive;

import java.util.Objects;

/**
 * @author Iliana Ghazali
 **/
public class SensitiveObject {

    private final String name;

    private final SensitiveComponent secretComponent;

    public SensitiveObject(String name, SensitiveComponent secretComponent) {
        this.name = name;
        this.secretComponent = secretComponent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SensitiveObject that = (SensitiveObject) o;
        return Objects.equals(name, that.name) && Objects.equals(secretComponent, that.secretComponent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, secretComponent);
    }

    @Override
    public String toString() {
        return "SensitiveObject{" + "name='" + name + '\'' + ", secretComponent=" + secretComponent + '}';
    }
}

class SensitiveComponent {
    int serialNumber;

    @StringSensitive
    String componentType;

    public SensitiveComponent(int serialNumber, String componentType) {
        this.serialNumber = serialNumber;
        this.componentType = componentType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SensitiveComponent that = (SensitiveComponent) o;
        return serialNumber == that.serialNumber && Objects.equals(componentType, that.componentType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serialNumber, componentType);
    }

    @Override
    public String toString() {
        return "SensitiveComponent{"
               + "serialNumber="
               + serialNumber
               + ", componentType='"
               + componentType
               + '\''
               + '}';
    }
}