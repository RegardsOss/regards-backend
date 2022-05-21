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
package fr.cnes.regards.modules.feature.domain.request;

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.framework.jpa.json.JsonTypeDescriptor;
import fr.cnes.regards.modules.feature.dto.StorageMetadata;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.MappedSuperclass;
import javax.validation.Valid;
import java.util.List;

/**
 * Shared metadata information about storage locations
 *
 * @author Sébastien Binda
 */
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
@Embeddable
@MappedSuperclass
public class FeatureStorageMedataEntity {

    @Valid
    @Column(columnDefinition = "jsonb")
    @Type(type = "jsonb", parameters = {
        @Parameter(name = JsonTypeDescriptor.ARG_TYPE, value = "fr.cnes.regards.modules.feature.dto.StorageMetadata") })
    private List<StorageMetadata> storages;

    public List<StorageMetadata> getStorages() {
        return storages;
    }

    public void setStorages(List<StorageMetadata> storages) {
        this.storages = storages;
    }

    public boolean hasStorage() {
        return storages != null && !storages.isEmpty();
    }

}
