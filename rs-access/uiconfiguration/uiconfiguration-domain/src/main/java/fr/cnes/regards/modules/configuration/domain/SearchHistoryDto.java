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
package fr.cnes.regards.modules.configuration.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * @author Théo Lasserre
 */
public class SearchHistoryDto {

    @Schema(description = "Identifier to track the search history element.")
    private Long id;

    @NotNull(message = "name is required")
    @Schema(description = "Name of the search history element.")
    private String name;

    @NotNull(message = "configuration is required")
    @Schema(description = "Configuration of the search history element.")
    private String configuration;

    public SearchHistoryDto(String name, String configuration) {
        super();
        this.name = name;
        this.configuration = configuration;
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

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, configuration);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SearchHistoryDto that = (SearchHistoryDto) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(configuration,
                                                                                                that.configuration);
    }

    @Override
    public String toString() {
        return "SearchHistoryDto{" + "id='" + id + ", name=" + name + ", configuration='" + configuration + '}';
    }
}
