/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

package fr.cnes.regards.framework.modules.tenant.settings.domain;

import fr.cnes.regards.framework.jpa.json.GsonUtil;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Objects;

@Entity
@Table(name = "t_dynamic_tenant_setting")
public class DynamicTenantSetting {

    @Id
    @SequenceGenerator(name = "tenantSettingSequence", sequenceName = "seq_dynamic_tenant_setting")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tenantSettingSequence")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(unique = true)
    private String name;

    @Column
    @Type(type = "text")
    private String description;

    @NotNull
    @Type(type = "text")
    @Column
    private String value;

    @NotNull
    @Type(type = "text")
    @Column(name = "default_value")
    private String defaultValue;

    @NotNull
    @Column(name = "class_name")
    private String className;

    public DynamicTenantSetting() {
    }

    public <T> DynamicTenantSetting(Long id, String name, String description, T defaultValue, T value) {
        this.id = id;
        this.name = name;
        this.description = description;
        setDefaultValue(defaultValue);
        setValue(value);
    }

    public <T> DynamicTenantSetting(String name, String description, T defaultValue) {
        this.name = name;
        this.description = description;
        setDefaultValue(defaultValue);
        setValue(defaultValue);
    }

    public <T> T getDefaultValue() {
        try {
            return getDefaultValue(Class.forName(className));
        } catch (ClassNotFoundException e) {
            throw new RsRuntimeException(e);
        }
    }

    public final <T> void setDefaultValue(T defaultValue) {
        if (defaultValue != null) {
            this.className = defaultValue.getClass().getName();
        }
        this.defaultValue = GsonUtil.toString(defaultValue);
    }

    public <T> T getDefaultValue(java.lang.reflect.Type type) {
        return type == null ? null : GsonUtil.fromString(defaultValue, type);
    }

    public <T> T getValue() {
        try {
            return getValue(Class.forName(className));
        } catch (ClassNotFoundException e) {
            throw new RsRuntimeException(e);
        }
    }

    public final <T> void setValue(T value) {
        if (value != null && className == null) {
            this.className = value.getClass().getName();
        }
        this.value = GsonUtil.toString(value);
    }

    public <T> T getValue(java.lang.reflect.Type type) {
        return type == null ? null : GsonUtil.fromString(value, type);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof DynamicTenantSetting))
            return false;
        DynamicTenantSetting that = (DynamicTenantSetting) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "DynamicTenantSetting{" + "name='" + name + '\'' + ", description='" + description + '\'' + ", " +
                "value='" + value + '\'' + ", defaultValue='" + defaultValue + '\'' + '}';
    }

}
