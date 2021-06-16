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
package fr.cnes.regards.modules.dam.domain.entities;

import fr.cnes.regards.modules.dam.domain.entities.feature.DatasetFeature;
import lombok.Builder;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * POJO to configure a dataset
 *
 * @author Marc SORDI
 */
@Data
@Builder
public class DatasetConfiguration {

    @NotBlank(message = "Datasource identifier must be set")
    private String datasource;

    private String subsetting;

    // FIXME add access right management
    // private Set<String> groups;

    @NotNull(message = "Feature must be set and must fit the model")
    private DatasetFeature feature;
}
