/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.acquisition.domain.metadata.dto;

import org.springframework.beans.BeanUtils;

import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;

/**
 * {@link MetaProduct} Dto
 * 
 * @author Christophe Mertz
 *
 */
public class MetaProductDto {

    protected Long id;

    private String label;

    private String algorithm;

    // TODO CMZ : util ?
    private Boolean cleanOriginalFile;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public Boolean getCleanOriginalFile() {
        return cleanOriginalFile;
    }

    public void setCleanOriginalFile(Boolean cleanOriginalFile) {
        this.cleanOriginalFile = cleanOriginalFile;
    }

    public static MetaProductDto fromMetaProduct(MetaProduct metaProduct) {
        MetaProductDto dto = new MetaProductDto();
        BeanUtils.copyProperties(metaProduct, dto);
        return dto;
    }

}
