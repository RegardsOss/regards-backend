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
package fr.cnes.regards.modules.ltamanager.domain.submission;

import fr.cnes.regards.framework.jpa.converters.PathAttributeConverter;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestDto;
import org.hibernate.annotations.Type;
import org.springframework.util.Assert;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Linked to {@link SubmissionRequest}
 *
 * @author Iliana Ghazali
 **/
@Embeddable
public class SubmittedProduct {

    @Column(nullable = false, updatable = false)
    @NotBlank(message = "datatype is required")
    private String datatype;

    @Column(length = 32, nullable = false, updatable = false)
    @NotBlank(message = "model is required")
    private String model;

    @Column(name = "store_path", nullable = false, updatable = false)
    @NotNull(message = "storePath is required")
    @Convert(converter = PathAttributeConverter.class)
    private Path storePath;

    @Column(updatable = false, columnDefinition = "jsonb")
    @Type(type = "jsonb")
    @Valid
    private SubmissionRequestDto product;

    public SubmittedProduct() {
        // no-args constructor for jpa
    }

    public SubmittedProduct(String datatype, String model, Path storePath, SubmissionRequestDto product) {
        Assert.notNull(datatype, "datatype is mandatory !");
        Assert.notNull(model, "model is mandatory !");
        Assert.notNull(storePath, "storePath is mandatory !");
        Assert.notNull(product, "product is mandatory !");

        this.datatype = datatype;
        this.model = model;
        this.storePath = storePath;
        this.product = product;
    }

    public String getDatatype() {
        return datatype;
    }

    public String getModel() {
        return model;
    }

    public Path getStorePath() {
        return storePath;
    }

    public SubmissionRequestDto getProduct() {
        return product;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SubmittedProduct that = (SubmittedProduct) o;
        return datatype.equals(that.datatype)
               && model.equals(that.model)
               && storePath.equals(that.storePath)
               && product.equals(that.product);
    }

    @Override
    public int hashCode() {
        return Objects.hash(datatype, model, storePath, product);
    }

    @Override
    public String toString() {
        return "SubmittedProduct{"
               + "datatype='"
               + datatype
               + '\''
               + ", model='"
               + model
               + '\''
               + ", storePath="
               + storePath
               + ", product="
               + product
               + '}';
    }
}
