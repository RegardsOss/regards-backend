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
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.ltamanager.domain.settings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Objects;

/**
 * Linked to {@link LtaSettings#DATATYPES_KEY}, which is a Map<String, DatatypeParameter>
 *
 * @author Iliana Ghazali
 **/
public final class DatatypeParameter {

    @NotBlank(message = "model is required")
    @Size(max = 32, message = "model length is limited to 32 characters.")
    private final String model;

    @NotBlank(message = "storePath is required")
    @Size(max = 255, message = "storePath length is limited to 255 characters.")
    private String storePath;

    public DatatypeParameter(String model, String storePath) {
        this.model = model;
        this.storePath = storePath;
    }

    public String getModel() {
        return model;
    }

    public String getStorePath() {
        return storePath;
    }

    public void setStorePath(String storePath) {
        this.storePath = storePath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DatatypeParameter that = (DatatypeParameter) o;
        return model.equals(that.model) && storePath.equals(that.storePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(model, storePath);
    }

    @Override
    public String toString() {
        return "DatatypeParameter{" + "model='" + model + '\'' + ", storePath='" + storePath + '\'' + '}';
    }
}
