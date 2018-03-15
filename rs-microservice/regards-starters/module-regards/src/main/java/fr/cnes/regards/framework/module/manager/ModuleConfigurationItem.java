/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.module.manager;

/**
 * This class represents a module configuration item. Class representing value may be annotated with
 * {@link ConfigIgnore} in addition to GsonIgnore to avoid specific field serialization.
 * @author Marc Sordi
 *
 */
public class ModuleConfigurationItem<T> {

    /**
     * The instance class
     */
    private final Class<T> key;

    /**
     * JSON string instance of the above JAVA class
     */
    private final T value;

    @SuppressWarnings("unchecked")
    public ModuleConfigurationItem(T value) {
        key = (Class<T>) value.getClass();
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    @SuppressWarnings("unchecked")
    public <U> U getTypedValue() {
        return (U) value;
    }

    public static <T> ModuleConfigurationItem<T> build(T value) {
        ModuleConfigurationItem<T> mci = new ModuleConfigurationItem<T>(value);
        return mci;
    }

    public Class<T> getKey() {
        return key;
    }
}
