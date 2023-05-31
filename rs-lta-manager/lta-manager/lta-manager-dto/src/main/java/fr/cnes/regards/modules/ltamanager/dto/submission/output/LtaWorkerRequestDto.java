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
package fr.cnes.regards.modules.ltamanager.dto.submission.output;

import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestDto;
import org.springframework.util.Assert;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.beans.ConstructorProperties;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Dto to send to the worker-manager following a successful save of submission requests
 *
 * @author Iliana Ghazali
 **/
public class LtaWorkerRequestDto {

    @NotBlank(message = "storage is required")
    private final String storage;

    @NotNull(message = "dataTypeStorePath is required")
    private final Path dataTypeStorePath;

    @NotBlank(message = "model is required")
    private final String model;

    @Valid
    private SubmissionRequestDto product;

    private boolean replace;

    @ConstructorProperties({ "storage", "dataTypeStorePath", "model", "product", "replace" })
    public LtaWorkerRequestDto(String storage,
                               Path dataTypeStorePath,
                               String model,
                               SubmissionRequestDto product,
                               boolean replace) {
        Assert.notNull(storage, "storage is mandatory !");
        Assert.notNull(dataTypeStorePath, "dataTypeStorePath is mandatory !");
        Assert.notNull(model, "model is mandatory !");
        Assert.notNull(product, "product is mandatory !");

        this.storage = storage;
        this.dataTypeStorePath = dataTypeStorePath;
        this.model = model;
        this.product = product;
        this.replace = replace;
    }

    public String getStorage() {
        return storage;
    }

    public Path getDataTypeStorePath() {
        return dataTypeStorePath;
    }

    public String getModel() {
        return model;
    }

    public SubmissionRequestDto getProduct() {
        return product;
    }

    public void setProduct(SubmissionRequestDto submissionRequestDto) {
        this.product = submissionRequestDto;
    }

    public boolean isReplace() {
        return replace;
    }

    public void setReplace(boolean replace) {
        this.replace = replace;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LtaWorkerRequestDto that = (LtaWorkerRequestDto) o;
        return replace == that.replace
               && storage.equals(that.storage)
               && dataTypeStorePath.equals(that.dataTypeStorePath)
               && model.equals(that.model)
               && product.equals(that.product);
    }

    @Override
    public int hashCode() {
        return Objects.hash(storage, dataTypeStorePath, model, product, replace);
    }

    @Override
    public String toString() {
        return "LtaWorkerRequestDto{"
               + "storage='"
               + storage
               + '\''
               + ", dataTypeStorePath="
               + dataTypeStorePath
               + ", model='"
               + model
               + '\''
               + ", product="
               + product
               + ", replace="
               + replace
               + '}';
    }
}
