/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/**
 * POJO to configure a dataset
 *
 * @author Marc SORDI
 */
public class DatasetConfiguration {

    @NotBlank(message = "Datasource identifier must be set")
    private String datasource;

    private String subsetting;

    // FIXME add access right management
    // private Set<String> groups;

    @NotNull(message = "Feature must be set and must fit the model")
    private DatasetFeature feature;

    public DatasetConfiguration(String datasource, String subsetting, DatasetFeature feature) {
        this.datasource = datasource;
        this.subsetting = subsetting;
        this.feature = feature;
    }

    public String getDatasource() {
        return datasource;
    }

    public void setDatasource(String datasource) {
        this.datasource = datasource;
    }

    public String getSubsetting() {
        return subsetting;
    }

    public void setSubsetting(String subsetting) {
        this.subsetting = subsetting;
    }

    public DatasetFeature getFeature() {
        return feature;
    }

    public void setFeature(DatasetFeature feature) {
        this.feature = feature;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DatasetConfiguration that = (DatasetConfiguration) o;

        if (!Objects.equals(datasource, that.datasource)) {
            return false;
        }
        if (!Objects.equals(subsetting, that.subsetting)) {
            return false;
        }
        return feature.equals(that.feature);
    }

    @Override
    public int hashCode() {
        int result = datasource != null ? datasource.hashCode() : 0;
        result = 31 * result + (subsetting != null ? subsetting.hashCode() : 0);
        result = 31 * result + feature.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "DatasetConfiguration{"
               + "datasource='"
               + datasource
               + '\''
               + ", subsetting='"
               + subsetting
               + '\''
               + ", feature="
               + feature
               + '}';
    }
}
