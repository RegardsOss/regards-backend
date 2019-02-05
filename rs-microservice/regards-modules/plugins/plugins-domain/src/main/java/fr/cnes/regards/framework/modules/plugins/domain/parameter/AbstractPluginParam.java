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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.NotNull;

/**
 * Parameter associated to a plugin configuration <PluginConfiguration>
 * @author Christophe Mertz
 * @author Marc SORDI
 *
 * @param <T> parameter type
 */
public abstract class AbstractPluginParam<T> implements IPluginParam {

    @NotNull(message = "Parameter name is required")
    protected String name;

    @NotNull(message = "Parameter value is required")
    protected T value;

    protected Class<T> clazz;

    protected boolean dynamic = false;

    protected Set<T> dynamicsValues = new HashSet<>();

    @Override
    public boolean hasValue() {
        return value != null;
    }

    @Override
    public boolean isInstance(Class<?> clazz) {
        return clazz.isInstance(value);
    }

    @Override
    public String getType() {
        return clazz.getName();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean isValid(IPluginParam staticParam) {
        if (this.getClass().isInstance(staticParam)) {
            return isValidDynamicValue((T) staticParam.getValue());
        }
        return false;
    }

    public boolean isValidDynamicValue(T value) {
        if (dynamicsValues == null || dynamicsValues.isEmpty()) {
            // No restriction
            return true;
        } else {
            for (T dyn : dynamicsValues) {
                if (dyn.equals(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (name == null ? 0 : name.hashCode());
        return result;
    }

    /**
     * The name of the parameter is the natural id. Two plugin parameters can have the same name but not within same
     * plugin configuration
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        @SuppressWarnings("rawtypes")
        AbstractPluginParam other = (AbstractPluginParam) obj;

        if (name == null) {
            return other.name == null;
        } else {
            return name.equals(other.name);
        }
    }

    @Override
    public String toString() {
        return name + " - " + value + " - " + dynamic;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public Class<T> getClazz() {
        return clazz;
    }

    public void setClazz(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public boolean isDynamic() {
        return dynamic;
    }

    public void setDynamic(Boolean dynamic) {
        this.dynamic = dynamic;
    }

    public Set<T> getDynamicsValues() {
        return dynamicsValues;
    }

    public void setDynamicsValues(Set<T> dynamicsValues) {
        this.dynamicsValues = dynamicsValues;
    }

    // Fluent API

    @SuppressWarnings("unchecked")
    public <P extends AbstractPluginParam<T>> P with(String name, T value) {
        this.name = name;
        this.value = value;
        this.clazz = (Class<T>) value.getClass();
        return (P) this;
    }

    @SuppressWarnings("unchecked")
    public <P extends AbstractPluginParam<T>> P with(Class<T> clazz, String name) {
        this.name = name;
        this.clazz = clazz;
        return (P) this;
    }

    public AbstractPluginParam<T> dynamic() {
        this.setDynamic(Boolean.TRUE);
        return this;
    }

    public AbstractPluginParam<T> dynamic(Set<T> dynamicsValues) {
        this.setDynamic(Boolean.TRUE);
        this.setDynamicsValues(dynamicsValues);
        return this;
    }

    public AbstractPluginParam<T> dynamic(T dyn1) {
        this.setDynamic(Boolean.TRUE);
        this.setDynamicsValues(new HashSet<>(Arrays.asList(dyn1)));
        return this;
    }

    public AbstractPluginParam<T> dynamic(T dyn1, T dyn2) {
        this.setDynamic(Boolean.TRUE);
        this.setDynamicsValues(new HashSet<>(Arrays.asList(dyn1, dyn2)));
        return this;
    }

    public AbstractPluginParam<T> dynamic(T dyn1, T dyn2, T dyn3) {
        this.setDynamic(Boolean.TRUE);
        this.setDynamicsValues(new HashSet<>(Arrays.asList(dyn1, dyn2, dyn3)));
        return this;
    }
}
