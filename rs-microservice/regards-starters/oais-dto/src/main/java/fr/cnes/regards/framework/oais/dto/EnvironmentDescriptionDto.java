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
package fr.cnes.regards.framework.oais.dto;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * Environment description for oais dto
 *
 * @author mnguyen0
 */
public class EnvironmentDescriptionDto {

    private final Map<String, Object> softwareEnvironment;

    private final Map<String, Object> hardwareEnvironment;

    public EnvironmentDescriptionDto() {
        softwareEnvironment = Maps.newHashMap();
        hardwareEnvironment = Maps.newHashMap();
    }

    public Map<String, Object> getSoftwareEnvironment() {
        return softwareEnvironment;
    }

    public Map<String, Object> getHardwareEnvironment() {
        return hardwareEnvironment;
    }
}
