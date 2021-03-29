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
package fr.cnes.regards.modules.access.services.domain.ui;

/**
 *
 * Enumeration of default plugins to initialize at service start
 *
 * @author Sébastien Binda
 *
 */
public enum UIDefaultPluginEnum {

    DEFAULT_STRING_CRITERION_NAME("string-criteria", "/plugins/criterion/string/plugin.js", UIPluginTypesEnum.CRITERIA),

    DEFAULT_FULLTEXT_CRITERION_NAME("full-text-criteria", "/plugins/criterion/full-text/plugin.js",
            UIPluginTypesEnum.CRITERIA),

    DEFAULT_NUMERICAL_CRITERION_NAME("numerical-criteria", "/plugins/criterion/numerical/plugin.js",
            UIPluginTypesEnum.CRITERIA),

    DEFAULT_TWONUMERICAL_CRITERION_NAME("two-numerical-criteria", "/plugins/criterion/two-numerical/plugin.js",
            UIPluginTypesEnum.CRITERIA),

    DEFAULT_TEMPORAL_CRITERION_NAME("temporal-criteria", "/plugins/criterion/temporal/plugin.js",
            UIPluginTypesEnum.CRITERIA),

    DEFAULT_TWOTEMPORAL_CRITERION_NAME("two-temporal-criteria", "/plugins/criterion/two-temporal/plugin.js",
            UIPluginTypesEnum.CRITERIA),

    DEFAULT_ENUMERATED_CRITERION_NAME("enumerated-criteria", "/plugins/criterion/enumerated/plugin.js",
            UIPluginTypesEnum.CRITERIA),

    DEFAULT_DATAWITHONLYPIC_CRITERION_NAME("data-with-picture-only",
            "/plugins/criterion/data-with-picture-only/plugin.js", UIPluginTypesEnum.CRITERIA),

    DEFAULT_LAST_VERSION_ONLY_CRITERION_NAME("last-version-only", "/plugins/criterion/last-version-only/plugin.js",
            UIPluginTypesEnum.CRITERIA),

    DEFAULT_TOPONYM_CRITERION_NAME("toponym", "/plugins/criterion/toponym/plugin.js", UIPluginTypesEnum.CRITERIA),

    DEFAULT_NUMERICAL_RANGE_CRITERION_NAME("numerical-range-criteria",
            "/plugins/criterion/numerical-range-criteria/plugin.js", UIPluginTypesEnum.CRITERIA);

    private String value;

    private String path;

    private UIPluginTypesEnum type;

    UIDefaultPluginEnum(String value, String path, UIPluginTypesEnum type) {
        this.value = value;
        this.path = path;
        this.type = type;
    }

    public UIPluginDefinition build() {
        return UIPluginDefinition.build(this.getValue(), this.getPath(), this.getType());
    }

    public String getValue() {
        return value;
    }

    public String getPath() {
        return path;
    }

    public UIPluginTypesEnum getType() {
        return type;
    }

}
